package dev.daan.hyperscale.registry;

import com.mojang.serialization.Codec;
import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.hall.FurnaceImage;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.item.DriveContents;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
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

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<DriveContents>> DRIVE_CONTENTS =
            COMPONENTS.register("drive_contents", () -> DataComponentType.<DriveContents>builder()
                    .persistent(DriveContents.CODEC)
                    .networkSynchronized(DriveContents.STREAM_CODEC)
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> HARDWARE_HEALTH =
            COMPONENTS.register("hardware_health", () -> DataComponentType.<Integer>builder()
                    .persistent(Codec.INT)
                    .networkSynchronized(ByteBufCodecs.VAR_INT)
                    .build());

    private ModDataComponents() {
    }
}
