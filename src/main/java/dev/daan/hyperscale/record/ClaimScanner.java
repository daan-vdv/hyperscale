package dev.daan.hyperscale.record;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.daan.hyperscale.block.ProbeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;

public final class ClaimScanner {
    public record Claim(BlockPos min, BlockPos max, Set<BlockPos> inventories) {
        public int width() {
            return this.max.getX() - this.min.getX() + 1;
        }

        public int height() {
            return this.max.getY() - this.min.getY() + 1;
        }

        public int depth() {
            return this.max.getZ() - this.min.getZ() + 1;
        }
    }

    private ClaimScanner() {
    }

    public static Claim build(Level level, List<BlockPos> terminals) {
        if (terminals.isEmpty()) {
            return null;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : terminals) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        minX -= RecordLimits.CLAIM_PAD;
        minY -= RecordLimits.CLAIM_PAD;
        minZ -= RecordLimits.CLAIM_PAD;
        maxX += RecordLimits.CLAIM_PAD;
        maxY += RecordLimits.CLAIM_PAD;
        maxZ += RecordLimits.CLAIM_PAD;
        if (maxX - minX + 1 > RecordLimits.MAX_CLAIM_SIDE
                || maxY - minY + 1 > RecordLimits.MAX_CLAIM_SIDE
                || maxZ - minZ + 1 > RecordLimits.MAX_CLAIM_SIDE) {
            return null;
        }

        Set<BlockPos> inventories = new HashSet<>();
        BlockPos min = new BlockPos(minX, minY, minZ);
        BlockPos max = new BlockPos(maxX, maxY, maxZ);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.isLoaded(pos)) {
                        continue;
                    }
                    if (level.getBlockState(pos).getBlock() instanceof ProbeBlock) {
                        continue;
                    }
                    if (hasItems(level, pos)) {
                        inventories.add(pos.immutable());
                    }
                }
            }
        }
        return new Claim(min, max, inventories);
    }

    public static boolean hasItems(Level level, BlockPos pos) {
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

    public static boolean isTerminal(BlockEntity blockEntity) {
        return blockEntity instanceof dev.daan.hyperscale.blockentity.ProbeBlockEntity;
    }
}
