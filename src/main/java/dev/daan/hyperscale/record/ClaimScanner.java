package dev.daan.hyperscale.record;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.daan.hyperscale.HyperscaleConfig;
import dev.daan.hyperscale.block.FluidProbeBlock;
import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class ClaimScanner {
    public record Claim(
            BlockPos outerMin,
            BlockPos outerMax,
            BlockPos innerMin,
            BlockPos innerMax,
            Set<BlockPos> inventories,
            Set<BlockPos> energyBlocks,
            Set<BlockPos> fluidBlocks,
            List<BlockPos> inputs,
            List<BlockPos> outputs,
            List<BlockPos> fluidInputs,
            List<BlockPos> fluidOutputs,
            List<BlockPos> powerPorts
    ) {
        public int width() {
            return this.outerMax.getX() - this.outerMin.getX() + 1;
        }

        public int height() {
            return this.outerMax.getY() - this.outerMin.getY() + 1;
        }

        public int depth() {
            return this.outerMax.getZ() - this.outerMin.getZ() + 1;
        }
    }

    public record SealedResult(Claim claim, RecordStatus error, String detail) {
        public static SealedResult ok(Claim claim) {
            return new SealedResult(claim, null, "");
        }

        public static SealedResult fail(RecordStatus error) {
            return new SealedResult(null, error, "");
        }

        public static SealedResult fail(RecordStatus error, String detail) {
            return new SealedResult(null, error, detail == null ? "" : detail);
        }
    }

    private ClaimScanner() {
    }

    /**
     * Locates a hollow rectangular prism of {@code RECORDER_CASING} (and optional {@code RECORDER_POWER} wall ports)
     * with the compiler in one of its walls, validates shell integrity, forbidden blocks, players, and container
     * entities, and collects all interior inventories, energy/fluid buffers, probes, and wall power ports.
     */
    public static SealedResult buildSealed(Level level, BlockPos recorderPos, Direction facing) {
        Direction[] candidates = new Direction[6];
        candidates[0] = facing.getOpposite();
        int idx = 1;
        for (Direction dir : Direction.values()) {
            if (dir != candidates[0]) {
                candidates[idx++] = dir;
            }
        }

        SealedResult bestFailure = SealedResult.fail(RecordStatus.NO_CASING);
        for (Direction dir : candidates) {
            BlockPos seed = recorderPos.relative(dir);
            if (!level.isLoaded(seed)) {
                continue;
            }
            BlockState seedState = level.getBlockState(seed);
            if (isShellBlock(seedState, seed, recorderPos)) {
                continue;
            }
            SealedResult result = tryFromSeed(level, recorderPos, seed);
            if (result.claim() != null) {
                return result;
            }
            if (result.error() != RecordStatus.NO_CASING) {
                bestFailure = result;
            }
        }
        return bestFailure;
    }

    private static SealedResult tryFromSeed(Level level, BlockPos recorderPos, BlockPos seed) {
        int minX = rayToShell(level, seed, Direction.WEST, recorderPos);
        int maxX = rayToShell(level, seed, Direction.EAST, recorderPos);
        int minY = rayToShell(level, seed, Direction.DOWN, recorderPos);
        int maxY = rayToShell(level, seed, Direction.UP, recorderPos);
        int minZ = rayToShell(level, seed, Direction.NORTH, recorderPos);
        int maxZ = rayToShell(level, seed, Direction.SOUTH, recorderPos);
        if (minX == Integer.MIN_VALUE || maxX == Integer.MIN_VALUE
                || minY == Integer.MIN_VALUE || maxY == Integer.MIN_VALUE
                || minZ == Integer.MIN_VALUE || maxZ == Integer.MIN_VALUE) {
            return SealedResult.fail(RecordStatus.NO_CASING);
        }

        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int depth = maxZ - minZ + 1;
        if (width < 3 || height < 3 || depth < 3) {
            return SealedResult.fail(RecordStatus.NO_CASING);
        }
        if (width > RecordLimits.MAX_CLAIM_SIDE
                || height > RecordLimits.MAX_CLAIM_SIDE
                || depth > RecordLimits.MAX_CLAIM_SIDE) {
            return SealedResult.fail(RecordStatus.TOO_LARGE);
        }

        BlockPos outerMin = new BlockPos(minX, minY, minZ);
        BlockPos outerMax = new BlockPos(maxX, maxY, maxZ);
        if (!isOnShellFace(recorderPos, outerMin, outerMax)) {
            return SealedResult.fail(RecordStatus.NO_CASING);
        }
        List<BlockPos> powerPorts = new ArrayList<>();
        if (!scanShell(level, outerMin, outerMax, recorderPos, powerPorts)) {
            return SealedResult.fail(RecordStatus.NO_CASING);
        }

        BlockPos innerMin = new BlockPos(minX + 1, minY + 1, minZ + 1);
        BlockPos innerMax = new BlockPos(maxX - 1, maxY - 1, maxZ - 1);
        if (hasPlayerInside(level, innerMin, innerMax)) {
            return SealedResult.fail(RecordStatus.PLAYER_INSIDE);
        }
        if (hasContainerEntityInside(level, innerMin, innerMax)) {
            return SealedResult.fail(RecordStatus.ENTITY_INSIDE);
        }

        boolean allowlistMode = HyperscaleConfig.USE_ALLOWLIST.get();
        Set<BlockPos> inventories = new HashSet<>();
        Set<BlockPos> energyBlocks = new HashSet<>();
        Set<BlockPos> fluidBlocks = new HashSet<>();
        List<BlockPos> inputs = new ArrayList<>();
        List<BlockPos> outputs = new ArrayList<>();
        List<BlockPos> fluidInputs = new ArrayList<>();
        List<BlockPos> fluidOutputs = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = innerMin.getX(); x <= innerMax.getX(); x++) {
            for (int y = innerMin.getY(); y <= innerMax.getY(); y++) {
                for (int z = innerMin.getZ(); z <= innerMax.getZ(); z++) {
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) {
                        return SealedResult.fail(RecordStatus.SEAL_BROKEN);
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    if (state.getBlock() instanceof ProbeBlock probe) {
                        if (probe.mode() == ProbeMode.INPUT) {
                            inputs.add(cursor.immutable());
                        } else {
                            outputs.add(cursor.immutable());
                        }
                        continue;
                    }
                    if (state.getBlock() instanceof FluidProbeBlock fluidProbe) {
                        if (fluidProbe.mode() == ProbeMode.INPUT) {
                            fluidInputs.add(cursor.immutable());
                        } else {
                            fluidOutputs.add(cursor.immutable());
                        }
                        continue;
                    }
                    if (state.is(ModTags.COMPILER_FORBIDDEN) || (allowlistMode && !state.is(ModTags.COMPILER_ALLOWED))) {
                        String blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                        return SealedResult.fail(RecordStatus.FORBIDDEN_BLOCK, blockId);
                    }
                    if (state.hasBlockEntity()) {
                        BlockPos imm = cursor.immutable();
                        if (hasItems(level, cursor)) {
                            inventories.add(imm);
                        }
                        if (hasEnergy(level, cursor)) {
                            energyBlocks.add(imm);
                        }
                        if (hasFluids(level, cursor)) {
                            fluidBlocks.add(imm);
                        }
                    }
                }
            }
        }
        inputs.sort(BlockPos::compareTo);
        outputs.sort(BlockPos::compareTo);
        fluidInputs.sort(BlockPos::compareTo);
        fluidOutputs.sort(BlockPos::compareTo);
        powerPorts.sort(BlockPos::compareTo);
        return SealedResult.ok(new Claim(
                outerMin, outerMax, innerMin, innerMax,
                inventories, energyBlocks, fluidBlocks,
                List.copyOf(inputs), List.copyOf(outputs),
                List.copyOf(fluidInputs), List.copyOf(fluidOutputs),
                List.copyOf(powerPorts)
        ));
    }

    private static int rayToShell(Level level, BlockPos seed, Direction dir, BlockPos recorderPos) {
        BlockPos.MutableBlockPos cursor = seed.mutable();
        for (int step = 1; step <= RecordLimits.MAX_CLAIM_SIDE; step++) {
            cursor.move(dir);
            if (!level.isLoaded(cursor)) {
                return Integer.MIN_VALUE;
            }
            if (isShellBlock(level.getBlockState(cursor), cursor, recorderPos)) {
                return switch (dir.getAxis()) {
                    case X -> cursor.getX();
                    case Y -> cursor.getY();
                    case Z -> cursor.getZ();
                };
            }
        }
        return Integer.MIN_VALUE;
    }

    private static boolean isOnShellFace(BlockPos pos, BlockPos min, BlockPos max) {
        boolean onX = pos.getX() == min.getX() || pos.getX() == max.getX();
        boolean onY = pos.getY() == min.getY() || pos.getY() == max.getY();
        boolean onZ = pos.getZ() == min.getZ() || pos.getZ() == max.getZ();
        boolean inBounds = pos.getX() >= min.getX() && pos.getX() <= max.getX()
                && pos.getY() >= min.getY() && pos.getY() <= max.getY()
                && pos.getZ() >= min.getZ() && pos.getZ() <= max.getZ();
        return inBounds && (onX || onY || onZ);
    }

    public static boolean isShellIntact(Level level, BlockPos min, BlockPos max, BlockPos recorderPos) {
        return scanShell(level, min, max, recorderPos, null);
    }

    private static boolean scanShell(Level level, BlockPos min, BlockPos max, BlockPos recorderPos, List<BlockPos> powerPorts) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    boolean shell = x == min.getX() || x == max.getX()
                            || y == min.getY() || y == max.getY()
                            || z == min.getZ() || z == max.getZ();
                    if (!shell) {
                        continue;
                    }
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) {
                        return false;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (!isShellBlock(state, cursor, recorderPos)) {
                        return false;
                    }
                    if (powerPorts != null && state.is(ModBlocks.RECORDER_POWER.get())) {
                        powerPorts.add(cursor.immutable());
                    }
                }
            }
        }
        return true;
    }

    private static boolean isShellBlock(BlockState state, BlockPos pos, BlockPos recorderPos) {
        if (pos.equals(recorderPos)) {
            return state.is(ModBlocks.RECORDER.get());
        }
        return state.is(ModBlocks.RECORDER_CASING.get()) || state.is(ModBlocks.RECORDER_POWER.get());
    }

    public static boolean hasPlayerInside(Level level, BlockPos innerMin, BlockPos innerMax) {
        AABB interior = interiorBox(innerMin, innerMax);
        return !level.getEntitiesOfClass(Player.class, interior, player -> !player.isSpectator()).isEmpty();
    }

    public static boolean hasContainerEntityInside(Level level, BlockPos innerMin, BlockPos innerMax) {
        AABB interior = interiorBox(innerMin, innerMax);
        for (Entity entity : level.getEntities((Entity) null, interior, e -> !(e instanceof Player))) {
            if (entity instanceof Container) {
                return true;
            }
            if (entity.getCapability(Capabilities.ItemHandler.ENTITY, null) != null
                    || entity.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, null) != null) {
                return true;
            }
        }
        return false;
    }

    private static AABB interiorBox(BlockPos innerMin, BlockPos innerMax) {
        return new AABB(
                innerMin.getX(), innerMin.getY(), innerMin.getZ(),
                innerMax.getX() + 1.0D, innerMax.getY() + 1.0D, innerMax.getZ() + 1.0D
        );
    }

    public static boolean hasItems(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof Container) {
            return true;
        }
        if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasEnergy(Level level, BlockPos pos) {
        if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasFluids(Level level, BlockPos pos) {
        if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null) != null) {
            return true;
        }
        for (Direction direction : Direction.values()) {
            if (level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction) != null) {
                return true;
            }
        }
        return false;
    }
}
