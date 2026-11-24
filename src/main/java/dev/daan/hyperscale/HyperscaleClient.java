package dev.daan.hyperscale;

import dev.daan.hyperscale.client.InterfaceScreen;
import dev.daan.hyperscale.client.LithographyScreen;
import dev.daan.hyperscale.client.RecorderScreen;
import dev.daan.hyperscale.client.StorageScreen;
import dev.daan.hyperscale.client.WaferPressScreen;
import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@EventBusSubscriber(modid = Hyperscale.MOD_ID, value = Dist.CLIENT)
public final class HyperscaleClient {
    private static final int TINT_RECORDER = 0xFF4B6A9B;
    private static final int TINT_INPUT = 0xFF3CA66B;
    private static final int TINT_OUTPUT = 0xFFD9822B;
    private static final int TINT_FLUID_INPUT = 0xFF2B9AD9;
    private static final int TINT_FLUID_OUTPUT = 0xFF1F6FA8;
    private static final int TINT_POWER = 0xFFE6C235;
    private static final int TINT_WAFER_PRESS = 0xFF2E8B9E;
    private static final int TINT_LITHOGRAPHY = 0xFF7E4BB8;

    private HyperscaleClient() {
    }

    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.INTERFACE.get(), InterfaceScreen::new);
        event.register(ModMenus.PORT.get(), StorageScreen::new);
        event.register(ModMenus.RACK.get(), StorageScreen::new);
        event.register(ModMenus.DRIVE_BAY.get(), StorageScreen::new);
        event.register(ModMenus.IMAGE_REGISTRY.get(), StorageScreen::new);
        event.register(ModMenus.SWITCH.get(), StorageScreen::new);
        event.register(ModMenus.PROBE.get(), StorageScreen::new);
        event.register(ModMenus.RECORDER.get(), RecorderScreen::new);
        event.register(ModMenus.WAFER_PRESS.get(), WaferPressScreen::new);
        event.register(ModMenus.LITHOGRAPHY.get(), LithographyScreen::new);
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_RECORDER : -1, ModBlocks.RECORDER.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_INPUT : -1, ModBlocks.RECORDER_INPUT.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_OUTPUT : -1, ModBlocks.RECORDER_OUTPUT.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_FLUID_INPUT : -1, ModBlocks.RECORDER_FLUID_INPUT.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_FLUID_OUTPUT : -1, ModBlocks.RECORDER_FLUID_OUTPUT.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_POWER : -1, ModBlocks.RECORDER_POWER.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_WAFER_PRESS : -1, ModBlocks.WAFER_PRESS.get());
        event.register((state, level, pos, tintIndex) -> tintIndex == 0 ? TINT_LITHOGRAPHY : -1, ModBlocks.LITHOGRAPHY_ETCHER.get());
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_RECORDER : -1, ModItems.RECORDER.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_INPUT : -1, ModItems.RECORDER_INPUT.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_OUTPUT : -1, ModItems.RECORDER_OUTPUT.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_FLUID_INPUT : -1, ModItems.RECORDER_FLUID_INPUT.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_FLUID_OUTPUT : -1, ModItems.RECORDER_FLUID_OUTPUT.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_POWER : -1, ModItems.RECORDER_POWER.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_WAFER_PRESS : -1, ModItems.WAFER_PRESS.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? TINT_LITHOGRAPHY : -1, ModItems.LITHOGRAPHY_ETCHER.get());

        // Semiconductor intermediates tinted cleanly
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF6B7280 : -1, ModItems.SILICON.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF15803D : -1, ModItems.PCB.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF94A3B8 : -1, ModItems.SILICON_WAFER.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF38BDF8 : -1, ModItems.DOPED_WAFER.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFA855F7 : -1, ModItems.QUANTUM_WAFER.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFF59E0B : -1, ModItems.MASK_LOGIC.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF3B82F6 : -1, ModItems.MASK_MEMORY.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF10B981 : -1, ModItems.MASK_STORAGE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFEC4899 : -1, ModItems.MASK_TENSOR.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFFBBF24 : -1, ModItems.LOGIC_DIE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF60A5FA : -1, ModItems.MEMORY_DIE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF34D399 : -1, ModItems.NAND_DIE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFF472B6 : -1, ModItems.TENSOR_DIE.get());
        event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFFC084FC : -1, ModItems.QUANTUM_DIE.get());

        ModItems.allCpus().forEach(holder ->
                event.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof CpuItem cpu
                        ? cpu.spec().tintColor() : -1, holder.get()));
        ModItems.allRams().forEach(holder ->
                event.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof RamItem ram
                        ? ram.spec().tintColor() : -1, holder.get()));
        ModItems.allGpus().forEach(holder ->
                event.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof GpuItem gpu
                        ? gpu.spec().tintColor() : -1, holder.get()));
        ModItems.allDrives().forEach(holder ->
                event.register((stack, tintIndex) -> tintIndex == 0 && stack.getItem() instanceof StorageDriveItem drive
                        ? drive.spec().tintColor() : -1, holder.get()));
    }
}
