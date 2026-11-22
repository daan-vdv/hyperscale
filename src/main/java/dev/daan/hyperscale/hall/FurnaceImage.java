package dev.daan.hyperscale.hall;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record FurnaceImage(ResourceLocation input, ResourceLocation output, int cookingTime) {
    public static final Codec<FurnaceImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("input").forGetter(FurnaceImage::input),
            ResourceLocation.CODEC.fieldOf("output").forGetter(FurnaceImage::output),
            Codec.INT.fieldOf("cooking_time").forGetter(FurnaceImage::cookingTime)
    ).apply(instance, FurnaceImage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FurnaceImage> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            FurnaceImage::input,
            ResourceLocation.STREAM_CODEC,
            FurnaceImage::output,
            ByteBufCodecs.VAR_INT,
            FurnaceImage::cookingTime,
            FurnaceImage::new
    );

    public FurnaceImage {
        cookingTime = Math.max(1, cookingTime);
    }
}
