package dev.daan.hyperscale.hall;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.daan.hyperscale.block.BackupGeneratorBlock;
import dev.daan.hyperscale.block.FluidPortBlock;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public final class HallFinder {
    public static final int MAX_BLOCKS = 512;

    private HallFinder() {
    }

    public static boolean isPart(BlockState state) {
        return state.is(ModBlocks.INTERFACE.get())
                || state.is(ModBlocks.PORT.get())
                || state.is(ModBlocks.FLUID_PORT.get())
                || state.is(ModBlocks.SWITCH.get())
                || state.is(ModBlocks.SERVER_RACK.get())
                || state.is(ModBlocks.DRIVE_BAY.get())
                || state.is(ModBlocks.IMAGE_REGISTRY.get())
                || state.is(ModBlocks.RECORDER.get())
                || state.is(ModBlocks.SERVER_FAN.get())
                || state.is(ModBlocks.WATERCOOLER_CONTROLLER.get())
                || state.is(ModBlocks.BACKUP_GENERATOR.get());
    }

    public static boolean isCable(BlockState state) {
        return state.is(ModBlocks.CABLE.get());
    }

    /**
     * Checks whether a connected run of Hybrid Cables is plugged into at least one Network Switch.
     * In a datacenter, cables only carry traffic when routed through a switch.
     */
    public static boolean isSwitchedCable(Level level, BlockPos cablePos) {
        return isSwitchedCableRun(level, cablePos, new HashMap<>());
    }

    private static boolean isSwitchedCableRun(Level level, BlockPos startPos, Map<BlockPos, Boolean> cache) {
        Boolean cached = cache.get(startPos);
        if (cached != null) {
            return cached;
        }
        if (!level.isLoaded(startPos) || !isCable(level.getBlockState(startPos))) {
            return false;
        }
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> run = new HashSet<>();
        BlockPos start = startPos.immutable();
        queue.add(start);
        run.add(start);
        boolean touchesSwitch = false;

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!level.isLoaded(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (nextState.is(ModBlocks.SWITCH.get())) {
                    touchesSwitch = true;
                } else if (isCable(nextState) && !run.contains(next) && run.size() < MAX_BLOCKS) {
                    BlockPos immutable = next.immutable();
                    run.add(immutable);
                    queue.add(immutable);
                }
            }
        }

        for (BlockPos pos : run) {
            cache.put(pos, touchesSwitch);
        }
        return touchesSwitch;
    }

    public static HallParts find(Level level, BlockPos origin) {
        HallParts parts = new HallParts();
        if (!level.isLoaded(origin)) {
            return parts;
        }
        BlockState originState = level.getBlockState(origin);
        Map<BlockPos, Boolean> cableSwitchCache = new HashMap<>();
        boolean originCable = isCable(originState);
        if (!isPart(originState) && !(originCable && isSwitchedCableRun(level, origin, cableSwitchCache))) {
            return parts;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        BlockPos start = origin.immutable();
        queue.add(start);
        seen.add(start);

        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            BlockState state = pos.equals(start) ? originState : level.getBlockState(pos);
            if (isPart(state)) {
                classify(state, pos, parts);
            }
            for (Direction direction : Direction.values()) {
                if (seen.size() >= MAX_BLOCKS) {
                    break;
                }
                BlockPos next = pos.relative(direction);
                if (seen.contains(next) || !level.isLoaded(next)) {
                    continue;
                }
                BlockState nextState = level.getBlockState(next);
                if (isPart(nextState)) {
                    BlockPos immutable = next.immutable();
                    seen.add(immutable);
                    queue.add(immutable);
                } else if (isCable(nextState) && isSwitchedCableRun(level, next, cableSwitchCache)) {
                    BlockPos immutable = next.immutable();
                    seen.add(immutable);
                    queue.add(immutable);
                }
            }
        }
        parts.sortCanonical();
        return parts;
    }

    private static void classify(BlockState state, BlockPos pos, HallParts parts) {
        if (state.is(ModBlocks.SWITCH.get())) {
            parts.switches.add(pos);
        } else if (state.is(ModBlocks.PORT.get())) {
            if (state.getValue(PortBlock.MODE) == PortMode.INPUT) {
                parts.inputs.add(pos);
            } else {
                parts.outputs.add(pos);
            }
        } else if (state.is(ModBlocks.FLUID_PORT.get())) {
            if (state.getValue(FluidPortBlock.MODE) == PortMode.INPUT) {
                parts.fluidInputs.add(pos);
            } else {
                parts.fluidOutputs.add(pos);
            }
        } else if (state.is(ModBlocks.SERVER_RACK.get()) && state.getValue(ServerRackBlock.HALF) == DoubleBlockHalf.LOWER) {
            parts.racks.add(pos);
        } else if (state.is(ModBlocks.DRIVE_BAY.get())) {
            parts.drives.add(pos);
        } else if (state.is(ModBlocks.IMAGE_REGISTRY.get())) {
            parts.registries.add(pos);
        } else if (state.is(ModBlocks.SERVER_FAN.get())) {
            parts.fans.add(pos);
        } else if (state.is(ModBlocks.WATERCOOLER_CONTROLLER.get())) {
            parts.watercoolers.add(pos);
        } else if (state.is(ModBlocks.BACKUP_GENERATOR.get()) && BackupGeneratorBlock.isPrimary(state)) {
            parts.generators.add(pos);
        }
    }
}
