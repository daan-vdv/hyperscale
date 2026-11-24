package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;

public final class HallParts {
    public final List<BlockPos> switches = new ArrayList<>();
    public final List<BlockPos> inputs = new ArrayList<>();
    public final List<BlockPos> outputs = new ArrayList<>();
    public final List<BlockPos> fluidInputs = new ArrayList<>();
    public final List<BlockPos> fluidOutputs = new ArrayList<>();
    public final List<BlockPos> racks = new ArrayList<>();
    public final List<BlockPos> drives = new ArrayList<>();
    public final List<BlockPos> registries = new ArrayList<>();
    public final List<BlockPos> fans = new ArrayList<>();
    public final List<BlockPos> watercoolers = new ArrayList<>();
    public final List<BlockPos> generators = new ArrayList<>();

    public boolean hasSwitch() {
        return !this.switches.isEmpty();
    }

    void sortCanonical() {
        Comparator<BlockPos> order = BlockPos::compareTo;
        this.switches.sort(order);
        this.inputs.sort(order);
        this.outputs.sort(order);
        this.fluidInputs.sort(order);
        this.fluidOutputs.sort(order);
        this.racks.sort(order);
        this.drives.sort(order);
        this.registries.sort(order);
        this.fans.sort(order);
        this.watercoolers.sort(order);
        this.generators.sort(order);
    }
}
