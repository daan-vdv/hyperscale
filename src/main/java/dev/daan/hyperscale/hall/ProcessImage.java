package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record ProcessImage(
        List<StackCount> inputs,
        List<StackCount> outputs,
        int cycleTicks,
        int energyPerCycle,
        int machineCount,
        List<FluidStackCount> fluidInputs,
        List<FluidStackCount> fluidOutputs
) {
    public static final Codec<ProcessImage> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            StackCount.CODEC.listOf().fieldOf("inputs").forGetter(ProcessImage::inputs),
            StackCount.CODEC.listOf().fieldOf("outputs").forGetter(ProcessImage::outputs),
            Codec.INT.fieldOf("cycle_ticks").forGetter(ProcessImage::cycleTicks),
            Codec.INT.optionalFieldOf("energy_per_cycle", 0).forGetter(ProcessImage::energyPerCycle),
            Codec.INT.optionalFieldOf("machine_count", 1).forGetter(ProcessImage::machineCount),
            FluidStackCount.CODEC.listOf().optionalFieldOf("fluid_inputs", List.of()).forGetter(ProcessImage::fluidInputs),
            FluidStackCount.CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(ProcessImage::fluidOutputs)
    ).apply(instance, ProcessImage::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ProcessImage> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ProcessImage decode(RegistryFriendlyByteBuf buf) {
            List<StackCount> ins = ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC).decode(buf);
            List<StackCount> outs = ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC).decode(buf);
            int ticks = ByteBufCodecs.VAR_INT.decode(buf);
            int energy = ByteBufCodecs.VAR_INT.decode(buf);
            int machines = ByteBufCodecs.VAR_INT.decode(buf);
            List<FluidStackCount> fIns = ByteBufCodecs.collection(ArrayList::new, FluidStackCount.STREAM_CODEC).decode(buf);
            List<FluidStackCount> fOuts = ByteBufCodecs.collection(ArrayList::new, FluidStackCount.STREAM_CODEC).decode(buf);
            return new ProcessImage(ins, outs, ticks, energy, machines, fIns, fOuts);
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buf, ProcessImage value) {
            ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC).encode(buf, new ArrayList<>(value.inputs()));
            ByteBufCodecs.collection(ArrayList::new, StackCount.STREAM_CODEC).encode(buf, new ArrayList<>(value.outputs()));
            ByteBufCodecs.VAR_INT.encode(buf, value.cycleTicks());
            ByteBufCodecs.VAR_INT.encode(buf, value.energyPerCycle());
            ByteBufCodecs.VAR_INT.encode(buf, value.machineCount());
            ByteBufCodecs.collection(ArrayList::new, FluidStackCount.STREAM_CODEC).encode(buf, new ArrayList<>(value.fluidInputs()));
            ByteBufCodecs.collection(ArrayList::new, FluidStackCount.STREAM_CODEC).encode(buf, new ArrayList<>(value.fluidOutputs()));
        }
    };

    public ProcessImage(List<StackCount> inputs, List<StackCount> outputs, int cycleTicks) {
        this(inputs, outputs, cycleTicks, 0, 1, List.of(), List.of());
    }

    public ProcessImage(List<StackCount> inputs, List<StackCount> outputs, int cycleTicks, int energyPerCycle) {
        this(inputs, outputs, cycleTicks, energyPerCycle, 1, List.of(), List.of());
    }

    public ProcessImage(List<StackCount> inputs, List<StackCount> outputs, int cycleTicks, int energyPerCycle, int machineCount) {
        this(inputs, outputs, cycleTicks, energyPerCycle, machineCount, List.of(), List.of());
    }

    public ProcessImage {
        inputs = merge(inputs);
        outputs = merge(outputs);
        cycleTicks = Math.max(1, cycleTicks);
        energyPerCycle = Math.max(0, energyPerCycle);
        machineCount = Math.max(1, machineCount);
        fluidInputs = mergeFluids(fluidInputs);
        fluidOutputs = mergeFluids(fluidOutputs);
    }

    private static List<StackCount> merge(List<StackCount> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>();
        for (StackCount entry : raw) {
            if (entry != null && entry.count() > 0) {
                merged.merge(entry.item(), entry.count(), Integer::sum);
            }
        }
        List<StackCount> result = new ArrayList<>(merged.size());
        for (Map.Entry<ResourceLocation, Integer> entry : merged.entrySet()) {
            result.add(new StackCount(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }

    private static List<FluidStackCount> mergeFluids(List<FluidStackCount> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>();
        for (FluidStackCount entry : raw) {
            if (entry != null && entry.amount() > 0) {
                merged.merge(entry.fluid(), entry.amount(), Integer::sum);
            }
        }
        List<FluidStackCount> result = new ArrayList<>(merged.size());
        for (Map.Entry<ResourceLocation, Integer> entry : merged.entrySet()) {
            result.add(new FluidStackCount(entry.getKey(), entry.getValue()));
        }
        return List.copyOf(result);
    }
}
