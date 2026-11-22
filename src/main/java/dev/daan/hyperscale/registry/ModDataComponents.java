package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.hall.FurnaceImage;
import dev.daan.hyperscale.hall.ProcessImage;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModDataComponents {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Hyperscale.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<FurnaceImage>> FURNACE_IMAGE =
            COMPONENTS.register("furnace_image", () -> DataComponentType.<FurnaceImage>builder()
                    .persistent(FurnaceImage.CODEC)
                    .networkSynchronized(FurnaceImage.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ProcessImage>> PROCESS_IMAGE =
            COMPONENTS.register("process_image", () -> DataComponentType.<ProcessImage>builder()
                    .persistent(ProcessImage.CODEC)
                    .networkSynchronized(ProcessImage.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<GlobalPos>> BINDER_TARGET =
            COMPONENTS.register("binder_target", () -> DataComponentType.<GlobalPos>builder()
                    .persistent(GlobalPos.CODEC)
                    .networkSynchronized(GlobalPos.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }
}
