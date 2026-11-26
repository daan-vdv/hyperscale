package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.compat.guideme.HyperscaleGuideCompat;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Hyperscale.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HYPERSCALE = TABS.register("hyperscale",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.hyperscale"))
                    .icon(() -> ModItems.INTERFACE.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                        if (ModList.get().isLoaded("guideme")) {
                            output.accept(HyperscaleGuideCompat.createGuideItem());
                        }
                        output.accept(ModItems.INTERFACE.get());
                        output.accept(ModItems.PORT.get());
                        output.accept(ModItems.FLUID_PORT.get());
                        output.accept(ModItems.SWITCH.get());
                        output.accept(ModItems.SERVER_RACK.get());
                        output.accept(ModItems.SERVER.get());
                        output.accept(ModItems.DRIVE_BAY.get());
                        output.accept(ModItems.IMAGE_REGISTRY.get());
                        output.accept(ModItems.BACKUP_GENERATOR.get());
                        output.accept(ModItems.CABLE.get());
                        output.accept(ModItems.SERVER_FAN.get());
                        output.accept(ModItems.WATERCOOLER_CONTROLLER.get());
                        output.accept(ModItems.WAFER_PRESS.get());
                        output.accept(ModItems.LITHOGRAPHY_ETCHER.get());
                        output.accept(ModItems.SILICON.get());
                        output.accept(ModItems.PCB.get());
                        output.accept(ModItems.SILICON_WAFER.get());
                        output.accept(ModItems.DOPED_WAFER.get());
                        output.accept(ModItems.QUANTUM_WAFER.get());
                        output.accept(ModItems.MASK_LOGIC.get());
                        output.accept(ModItems.MASK_MEMORY.get());
                        output.accept(ModItems.MASK_STORAGE.get());
                        output.accept(ModItems.MASK_TENSOR.get());
                        output.accept(ModItems.LOGIC_DIE.get());
                        output.accept(ModItems.MEMORY_DIE.get());
                        output.accept(ModItems.NAND_DIE.get());
                        output.accept(ModItems.TENSOR_DIE.get());
                        output.accept(ModItems.QUANTUM_DIE.get());
                        ModItems.allCpus().forEach(item -> output.accept(item.get()));
                        ModItems.allRams().forEach(item -> output.accept(item.get()));
                        ModItems.allGpus().forEach(item -> output.accept(item.get()));
                        ModItems.allDrives().forEach(item -> output.accept(item.get()));
                        output.accept(ModItems.RECORDER.get());
                        output.accept(ModItems.RECORDER_CASING.get());
                        output.accept(ModItems.RECORDER_INPUT.get());
                        output.accept(ModItems.RECORDER_OUTPUT.get());
                        output.accept(ModItems.RECORDER_FLUID_INPUT.get());
                        output.accept(ModItems.RECORDER_FLUID_OUTPUT.get());
                        output.accept(ModItems.RECORDER_POWER.get());
                        output.accept(ModItems.EMPTY_IMAGE_CARD.get());
                        output.accept(ModItems.BINDER.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
