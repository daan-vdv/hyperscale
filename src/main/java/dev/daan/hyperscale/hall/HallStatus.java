package dev.daan.hyperscale.hall;

import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class HallStatus {
    private HallStatus() {
    }

    public static Component hover(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.INTERFACE.get())) {
            HallParts parts = HallFinder.find(level, pos);
            if (parts.drives.isEmpty()) {
                return Component.translatable("status.hyperscale.interface.no_drive");
            }
            return Component.translatable("status.hyperscale.interface.online");
        }
        if (state.is(ModBlocks.DRIVE_BAY.get())) {
            if (!(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) || bay.imageCount() == 0) {
                return Component.translatable("status.hyperscale.drive.empty");
            }
            return Component.translatable("status.hyperscale.drive.count", bay.imageCount());
        }
        if (state.is(ModBlocks.SERVER_RACK.get())) {
            BlockPos base = ServerRackBlock.basePos(pos, state);
            if (!(level.getBlockEntity(base) instanceof ServerRackBlockEntity rack)) {
                return Component.translatable("status.hyperscale.rack.no_server");
            }
            if (!rack.hasServer()) {
                return Component.translatable("status.hyperscale.rack.no_server");
            }
            if (!rack.hasCpu()) {
                return Component.translatable("status.hyperscale.rack.missing_cpu");
            }
            if (!rack.hasRam()) {
                return Component.translatable("status.hyperscale.rack.missing_ram");
            }
            return Component.translatable("status.hyperscale.rack.online");
        }
        if (state.is(ModBlocks.PORT.get())) {
            boolean linked = HallFinder.find(level, pos).hasSwitch();
            if (state.getValue(PortBlock.MODE) == PortMode.INPUT) {
                return Component.translatable(linked ? "status.hyperscale.port.accepting" : "status.hyperscale.port.unlinked");
            }
            return Component.translatable(linked ? "status.hyperscale.port.ejecting" : "status.hyperscale.port.unlinked");
        }
        if (state.is(ModBlocks.RECORDER.get())) {
            if (!(level.getBlockEntity(pos) instanceof RecorderBlockEntity recorder)) {
                return Component.translatable("status.hyperscale.compiler.unbound");
            }
            if (recorder.boundInputs() == 0 || recorder.boundOutputs() == 0) {
                return Component.translatable("status.hyperscale.compiler.unbound");
            }
            if (recorder.phase() == RecordPhase.LEARNING) {
                int run = Math.min(recorder.matched() + 1, RecordLimits.CYCLES);
                return Component.translatable("status.hyperscale.compiler.recording", run, RecordLimits.CYCLES);
            }
            return Component.translatable("status.hyperscale.compiler.idle");
        }
        return null;
    }
}
