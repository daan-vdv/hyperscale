package dev.daan.hyperscale.hall;

import dev.daan.hyperscale.block.BackupGeneratorBlock;
import dev.daan.hyperscale.block.FluidPortBlock;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.FluidPortBlockEntity;
import dev.daan.hyperscale.blockentity.ImageRegistryBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.WatercoolerBlockEntity;
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
            if (!parts.hasSwitch()) {
                return Component.translatable("gui.hyperscale.interface.no_switch");
            }
            if (parts.drives.isEmpty()) {
                return Component.translatable("status.hyperscale.interface.no_drive");
            }
            return Component.translatable("status.hyperscale.interface.online");
        }
        if (state.is(ModBlocks.SWITCH.get())) {
            HallParts parts = HallFinder.find(level, pos);
            HallCapacity cap = HallCapacity.compute(level, parts);
            if (cap.readyRacks() <= 0) {
                return Component.translatable("status.hyperscale.switch.no_rack");
            }
            return Component.translatable("status.hyperscale.switch.online_stats", cap.readyRacks(), cap.totalRamGb(), cap.totalVcpu());
        }
        if (state.is(ModBlocks.CABLE.get())) {
            boolean switched = HallFinder.isSwitchedCable(level, pos);
            return Component.translatable(switched ? "status.hyperscale.cable.linked" : "status.hyperscale.cable.unswitched");
        }
        if (state.is(ModBlocks.SERVER_FAN.get())) {
            boolean linked = HallFinder.find(level, pos).hasSwitch();
            return Component.translatable(linked ? "status.hyperscale.fan.online" : "status.hyperscale.fan.unlinked", HallThermal.FAN_COOLING_W);
        }
        if (state.is(ModBlocks.WATERCOOLER_CONTROLLER.get())) {
            boolean linked = HallFinder.find(level, pos).hasSwitch();
            if (!linked) {
                return Component.translatable("status.hyperscale.watercooler.unlinked");
            }
            int coolant = level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler ? cooler.coolantAmount() : 0;
            boolean water = coolant > 0;
            int savings = Math.min(75, WatercoolerBlockEntity.adjacentRadiatorFans(level, pos) * 25);
            return Component.translatable(
                    water ? "status.hyperscale.watercooler.liquid" : "status.hyperscale.watercooler.dry",
                    water ? HallThermal.WATERCOOLER_LIQUID_W : HallThermal.WATERCOOLER_DRY_W,
                    coolant,
                    WatercoolerBlockEntity.CAPACITY_MB,
                    savings
            );
        }
        if (state.is(ModBlocks.BACKUP_GENERATOR.get())) {
            BlockPos primary = BackupGeneratorBlock.primaryPos(pos, state);
            if (level.getBlockEntity(primary) instanceof BackupGeneratorBlockEntity gen) {
                return Component.translatable("status.hyperscale.generator.ups", gen.energyStored(), gen.maxEnergyStored());
            }
            return Component.translatable("block.hyperscale.backup_generator");
        }
        if (state.is(ModBlocks.IMAGE_REGISTRY.get())) {
            if (!(level.getBlockEntity(pos) instanceof ImageRegistryBlockEntity reg) || reg.imageCount() == 0) {
                return Component.translatable("status.hyperscale.registry.empty");
            }
            return Component.translatable("status.hyperscale.registry.count", reg.imageCount(), ImageRegistryBlockEntity.SLOTS);
        }
        if (state.is(ModBlocks.DRIVE_BAY.get())) {
            if (!(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) || bay.driveCount() == 0) {
                return Component.translatable("status.hyperscale.drive.no_drives");
            }
            if (bay.failedDriveCount() > 0) {
                return Component.translatable(
                        bay.hasRaidFaultTolerance() ? "status.hyperscale.drive.raid_degraded" : "status.hyperscale.drive.failed",
                        bay.raidMode().label(),
                        bay.failedDriveCount()
                );
            }
            return Component.translatable("status.hyperscale.drive.usage_raid", bay.raidMode().label(), bay.driveCount(), bay.usedItems(), bay.totalCapacity());
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
            if (!linked) {
                return Component.translatable("status.hyperscale.port.unlinked");
            }
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port && port.hasFilter()) {
                var ids = port.filterItemIds();
                var first = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ids.get(0));
                if (ids.size() == 1) {
                    return Component.translatable("status.hyperscale.port.ejecting_filter", Component.translatable(first.getDescriptionId()));
                }
                return Component.translatable("status.hyperscale.port.ejecting_filter_more", Component.translatable(first.getDescriptionId()), ids.size() - 1);
            }
            return Component.translatable("status.hyperscale.port.ejecting");
        }
        if (state.is(ModBlocks.FLUID_PORT.get()) && level.getBlockEntity(pos) instanceof FluidPortBlockEntity fp) {
            String mode = state.getValue(FluidPortBlock.MODE) == PortMode.INPUT ? "Input" : "Output";
            var fluid = fp.tank().getFluid();
            if (fluid.isEmpty()) {
                return Component.literal("Fluid Port (" + mode + "): Empty");
            }
            return Component.literal("Fluid Port (" + mode + "): " + fluid.getAmount() + " / " + FluidPortBlockEntity.CAPACITY_MB + " mB " + fluid.getHoverName().getString());
        }
        if (state.is(ModBlocks.RECORDER.get())) {
            if (!(level.getBlockEntity(pos) instanceof RecorderBlockEntity recorder)) {
                return Component.translatable("status.hyperscale.compiler.unbound");
            }
            if (recorder.phase() == RecordPhase.LEARNING) {
                int run = Math.min(recorder.matched() + 1, RecordLimits.CYCLES);
                return Component.translatable("status.hyperscale.compiler.recording", run, RecordLimits.CYCLES);
            }
            if (recorder.boundInputs() == 0 || recorder.boundOutputs() == 0) {
                return Component.translatable("status.hyperscale.compiler.unbound");
            }
            return Component.translatable("status.hyperscale.compiler.idle");
        }
        return null;
    }
}
