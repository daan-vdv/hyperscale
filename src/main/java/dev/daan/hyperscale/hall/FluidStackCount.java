package dev.daan.hyperscale.hall;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record FluidStackCount(ResourceLocation fluid, int amount) {
    public static final Codec<FluidStackCount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("fluid").forGetter(FluidStackCount::fluid),
            Codec.INT.fieldOf("amount").forGetter(FluidStackCount::amount)
    ).apply(instance, FluidStackCount::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidStackCount> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC,
            FluidStackCount::fluid,
            ByteBufCodecs.VAR_INT,
            FluidStackCount::amount,
            FluidStackCount::new
    );

    public FluidStackCount {
        amount = Math.max(0, amount);
    }
}
