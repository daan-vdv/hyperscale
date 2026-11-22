package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record ProcessImage(List<StackCount> inputs, List<StackCount> outputs, int cycleTicks) {
    public static final Codec<ProcessImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StackCount.CODEC.listOf().fieldOf("inputs").forGetter(ProcessImage::inputs),
            StackCount.CODEC.listOf().fieldOf("outputs").forGetter(ProcessImage::outputs),
            Codec.INT.fieldOf("cycle_ticks").forGetter(ProcessImage::cycleTicks)
    ).apply(instance, ProcessImage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessImage> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC),
            ProcessImage::inputs,
            ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC),
            ProcessImage::outputs,
            ByteBufCodecs.VAR_INT,
            ProcessImage::cycleTicks,
            ProcessImage::new
    );

    public ProcessImage {
        inputs = List.copyOf(inputs);
        outputs = List.copyOf(outputs);
        cycleTicks = Math.max(1, cycleTicks);
    }
}
