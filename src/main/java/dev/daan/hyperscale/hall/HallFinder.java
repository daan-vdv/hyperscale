package dev.daan.hyperscale.hall;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

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
    public static final int MAX_BLOCKS = 64;

    private HallFinder() {
    }

    public static boolean isPart(BlockState state) {
        return state.is(ModBlocks.INTERFACE.get())
                || state.is(ModBlocks.PORT.get())
                || state.is(ModBlocks.SWITCH.get())
                || state.is(ModBlocks.SERVER_RACK.get())
                || state.is(ModBlocks.DRIVE_BAY.get())
                || state.is(ModBlocks.RECORDER.get());
    }

    public static HallParts find(Level level, BlockPos origin) {
        HallParts parts = new HallParts();
        if (!isPart(level.getBlockState(origin))) {
            return parts;
        }

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(origin);
        seen.add(origin);

        while (!queue.isEmpty() && seen.size() <= MAX_BLOCKS) {
            BlockPos pos = queue.removeFirst();
            classify(level, pos, parts);
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (seen.contains(next) || !isPart(level.getBlockState(next))) {
                    continue;
                }
                seen.add(next);
                queue.add(next);
            }
        }
        return parts;
    }

    private static void classify(Level level, BlockPos pos, HallParts parts) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.SWITCH.get())) {
            parts.switches.add(pos);
        } else if (state.is(ModBlocks.PORT.get())) {
            if (state.getValue(PortBlock.MODE) == PortMode.INPUT) {
                parts.inputs.add(pos);
            } else {
                parts.outputs.add(pos);
            }
        } else if (state.is(ModBlocks.SERVER_RACK.get()) && state.getValue(ServerRackBlock.HALF) == DoubleBlockHalf.LOWER) {
            parts.racks.add(pos);
        } else if (state.is(ModBlocks.DRIVE_BAY.get())) {
            parts.drives.add(pos);
        } else if (state.is(ModBlocks.RECORDER.get())) {
            parts.recorders.add(pos);
        }
    }
}
