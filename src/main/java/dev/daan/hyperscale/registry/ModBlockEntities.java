package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.block.BackupGeneratorBlock;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.FluidPortBlockEntity;
import dev.daan.hyperscale.blockentity.FluidProbeBlockEntity;
import dev.daan.hyperscale.blockentity.ImageRegistryBlockEntity;
import dev.daan.hyperscale.blockentity.LithographyEtcherBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.PowerPortBlockEntity;
import dev.daan.hyperscale.blockentity.ProbeBlockEntity;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.blockentity.WaferPressBlockEntity;
import dev.daan.hyperscale.blockentity.WatercoolerBlockEntity;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallParts;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Hyperscale.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortBlockEntity>> PORT =
            BLOCK_ENTITIES.register("port", () -> BlockEntityType.Builder.of(PortBlockEntity::new, ModBlocks.PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidPortBlockEntity>> FLUID_PORT =
            BLOCK_ENTITIES.register("fluid_port", () -> BlockEntityType.Builder.of(FluidPortBlockEntity::new, ModBlocks.FLUID_PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwitchBlockEntity>> SWITCH =
            BLOCK_ENTITIES.register("switch", () -> BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK =
            BLOCK_ENTITIES.register("server_rack", () -> BlockEntityType.Builder.of(ServerRackBlockEntity::new, ModBlocks.SERVER_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveBayBlockEntity>> DRIVE_BAY =
            BLOCK_ENTITIES.register("drive_bay", () -> BlockEntityType.Builder.of(DriveBayBlockEntity::new, ModBlocks.DRIVE_BAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ImageRegistryBlockEntity>> IMAGE_REGISTRY =
            BLOCK_ENTITIES.register("image_registry", () -> BlockEntityType.Builder.of(ImageRegistryBlockEntity::new, ModBlocks.IMAGE_REGISTRY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecorderBlockEntity>> RECORDER =
            BLOCK_ENTITIES.register("recorder", () -> BlockEntityType.Builder.of(RecorderBlockEntity::new, ModBlocks.RECORDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeBlockEntity>> PROBE =
            BLOCK_ENTITIES.register("probe", () -> BlockEntityType.Builder.of(
                    ProbeBlockEntity::new,
                    ModBlocks.RECORDER_INPUT.get(),
                    ModBlocks.RECORDER_OUTPUT.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PowerPortBlockEntity>> POWER_PORT =
            BLOCK_ENTITIES.register("power_port", () -> BlockEntityType.Builder.of(
                    PowerPortBlockEntity::new,
                    ModBlocks.RECORDER_POWER.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BackupGeneratorBlockEntity>> BACKUP_GENERATOR =
            BLOCK_ENTITIES.register("backup_generator", () -> BlockEntityType.Builder.of(
                    BackupGeneratorBlockEntity::new,
                    ModBlocks.BACKUP_GENERATOR.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<FluidProbeBlockEntity>> FLUID_PROBE =
            BLOCK_ENTITIES.register("fluid_probe", () -> BlockEntityType.Builder.of(
                    FluidProbeBlockEntity::new,
                    ModBlocks.RECORDER_FLUID_INPUT.get(),
                    ModBlocks.RECORDER_FLUID_OUTPUT.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WatercoolerBlockEntity>> WATERCOOLER =
            BLOCK_ENTITIES.register("watercooler", () -> BlockEntityType.Builder.of(
                    WatercoolerBlockEntity::new,
                    ModBlocks.WATERCOOLER_CONTROLLER.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WaferPressBlockEntity>> WAFER_PRESS =
            BLOCK_ENTITIES.register("wafer_press", () -> BlockEntityType.Builder.of(
                    WaferPressBlockEntity::new,
                    ModBlocks.WAFER_PRESS.get()
            ).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LithographyEtcherBlockEntity>> LITHOGRAPHY_ETCHER =
            BLOCK_ENTITIES.register("lithography_etcher", () -> BlockEntityType.Builder.of(
                    LithographyEtcherBlockEntity::new,
                    ModBlocks.LITHOGRAPHY_ETCHER.get()
            ).build(null));

    private ModBlockEntities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PORT.get(), (blockEntity, side) -> blockEntity.capability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_PORT.get(), (blockEntity, side) -> blockEntity.capability());
        event.registerBlock(Capabilities.ItemHandler.BLOCK, (level, pos, state, blockEntity, side) -> {
            BlockPos base = ServerRackBlock.basePos(pos, state);
            return level.getBlockEntity(base) instanceof ServerRackBlockEntity rack ? rack.items() : null;
        }, ModBlocks.SERVER_RACK.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRIVE_BAY.get(), (blockEntity, side) -> blockEntity.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, IMAGE_REGISTRY.get(), (blockEntity, side) -> blockEntity.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PROBE.get(), ProbeBlockEntity::capability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, FLUID_PROBE.get(), FluidProbeBlockEntity::capability);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, WATERCOOLER.get(), (blockEntity, side) -> blockEntity.capability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, WAFER_PRESS.get(), (blockEntity, side) -> blockEntity.itemCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, WAFER_PRESS.get(), (blockEntity, side) -> blockEntity.energyCapability());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, LITHOGRAPHY_ETCHER.get(), (blockEntity, side) -> blockEntity.itemCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, LITHOGRAPHY_ETCHER.get(), (blockEntity, side) -> blockEntity.energyCapability());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, LITHOGRAPHY_ETCHER.get(), (blockEntity, side) -> blockEntity.fluidCapability());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, POWER_PORT.get(), PowerPortBlockEntity::capability);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, SWITCH.get(), (blockEntity, side) -> blockEntity.energyCapability());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, blockEntity, side) -> {
            BlockPos primary = BackupGeneratorBlock.primaryPos(pos, state);
            return level.getBlockEntity(primary) instanceof BackupGeneratorBlockEntity gen ? gen.energyCapability() : null;
        }, ModBlocks.BACKUP_GENERATOR.get());
        event.registerBlock(Capabilities.EnergyStorage.BLOCK, (level, pos, state, blockEntity, side) -> {
            if (!HallFinder.isSwitchedCable(level, pos)) {
                return null;
            }
            return cableEnergyFacade(level, pos);
        }, ModBlocks.CABLE.get());
    }

    private static IEnergyStorage cableEnergyFacade(Level level, BlockPos cablePos) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int toReceive, boolean simulate) {
                if (toReceive <= 0) {
                    return 0;
                }
                HallParts parts = HallFinder.find(level, cablePos);
                if (!parts.hasSwitch()) {
                    return 0;
                }
                int left = toReceive;
                if (level.getBlockEntity(parts.switches.get(0)) instanceof SwitchBlockEntity sw) {
                    left -= sw.energyCapability().receiveEnergy(left, simulate);
                }
                for (BlockPos genPos : parts.generators) {
                    if (left <= 0) {
                        break;
                    }
                    if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof BackupGeneratorBlockEntity gen) {
                        left -= gen.energyCapability().receiveEnergy(left, simulate);
                    }
                }
                return toReceive - left;
            }

            @Override
            public int extractEnergy(int toExtract, boolean simulate) {
                return 0;
            }

            @Override
            public int getEnergyStored() {
                HallParts parts = HallFinder.find(level, cablePos);
                if (!parts.hasSwitch()) {
                    return 0;
                }
                long total = 0;
                if (level.getBlockEntity(parts.switches.get(0)) instanceof SwitchBlockEntity sw) {
                    total += sw.energyStored();
                }
                for (BlockPos genPos : parts.generators) {
                    if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof BackupGeneratorBlockEntity gen) {
                        total += gen.energyStored();
                    }
                }
                return (int) Math.min(Integer.MAX_VALUE, total);
            }

            @Override
            public int getMaxEnergyStored() {
                HallParts parts = HallFinder.find(level, cablePos);
                if (!parts.hasSwitch()) {
                    return 0;
                }
                long total = 0;
                if (level.getBlockEntity(parts.switches.get(0)) instanceof SwitchBlockEntity sw) {
                    total += sw.maxEnergyStored();
                }
                for (BlockPos genPos : parts.generators) {
                    if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof BackupGeneratorBlockEntity gen) {
                        total += gen.maxEnergyStored();
                    }
                }
                return (int) Math.min(Integer.MAX_VALUE, total);
            }

            @Override
            public boolean canExtract() {
                return false;
            }

            @Override
            public boolean canReceive() {
                return true;
            }
        };
    }
}
