package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
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
                        output.accept(ModItems.INTERFACE.get());
                        output.accept(ModItems.PORT.get());
                        output.accept(ModItems.SWITCH.get());
                        output.accept(ModItems.SERVER_RACK.get());
                        output.accept(ModItems.SERVER.get());
                        output.accept(ModItems.DRIVE_BAY.get());
                        output.accept(ModItems.HDD.get());
                        output.accept(ModItems.CPU.get());
                        output.accept(ModItems.RAM.get());
                        output.accept(ModItems.RECORDER.get());
                        output.accept(ModItems.RECORDER_INPUT.get());
                        output.accept(ModItems.RECORDER_OUTPUT.get());
                        output.accept(ModItems.EMPTY_IMAGE_CARD.get());
                        output.accept(ModItems.BINDER.get());
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
