package dev.daan.hyperscale.hall;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record StackCount(ResourceLocation item, int count) {
    public static final Codec<StackCount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("item").forGetter(StackCount::item),
            Codec.INT.fieldOf("count").forGetter(StackCount::count)
    ).apply(instance, StackCount::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, StackCount> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            StackCount::item,
            ByteBufCodecs.VAR_INT,
            StackCount::count,
            StackCount::new
    );

    public StackCount {
        count = Math.max(1, count);
    }
}
