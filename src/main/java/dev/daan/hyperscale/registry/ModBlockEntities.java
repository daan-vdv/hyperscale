package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.ProbeBlockEntity;
import dev.daan.hyperscale.blockentity.ProbeItemHandler;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SidedItemHandler;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Hyperscale.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PortBlockEntity>> PORT =
            BLOCK_ENTITIES.register("port", () -> BlockEntityType.Builder.of(PortBlockEntity::new, ModBlocks.PORT.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SwitchBlockEntity>> SWITCH =
            BLOCK_ENTITIES.register("switch", () -> BlockEntityType.Builder.of(SwitchBlockEntity::new, ModBlocks.SWITCH.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ServerRackBlockEntity>> SERVER_RACK =
            BLOCK_ENTITIES.register("server_rack", () -> BlockEntityType.Builder.of(ServerRackBlockEntity::new, ModBlocks.SERVER_RACK.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DriveBayBlockEntity>> DRIVE_BAY =
            BLOCK_ENTITIES.register("drive_bay", () -> BlockEntityType.Builder.of(DriveBayBlockEntity::new, ModBlocks.DRIVE_BAY.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RecorderBlockEntity>> RECORDER =
            BLOCK_ENTITIES.register("recorder", () -> BlockEntityType.Builder.of(RecorderBlockEntity::new, ModBlocks.RECORDER.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ProbeBlockEntity>> PROBE =
            BLOCK_ENTITIES.register("probe", () -> BlockEntityType.Builder.of(
                    ProbeBlockEntity::new,
                    ModBlocks.RECORDER_INPUT.get(),
                    ModBlocks.RECORDER_OUTPUT.get()
            ).build(null));

    private ModBlockEntities() {
    }

    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PORT.get(), (blockEntity, side) -> {
            boolean input = blockEntity.getBlockState().getValue(PortBlock.MODE) == PortMode.INPUT;
            return new SidedItemHandler(blockEntity.items(), input, !input);
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SERVER_RACK.get(), (blockEntity, side) -> blockEntity.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, DRIVE_BAY.get(), (blockEntity, side) -> blockEntity.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PROBE.get(),
                (blockEntity, side) -> new ProbeItemHandler(blockEntity, blockEntity.items(), side));
    }
}
