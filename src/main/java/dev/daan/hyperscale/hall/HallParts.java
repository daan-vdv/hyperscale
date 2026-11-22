package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;

public final class HallParts {
    public final List<BlockPos> switches = new ArrayList<>();
    public final List<BlockPos> inputs = new ArrayList<>();
    public final List<BlockPos> outputs = new ArrayList<>();
    public final List<BlockPos> racks = new ArrayList<>();
    public final List<BlockPos> drives = new ArrayList<>();
    public final List<BlockPos> recorders = new ArrayList<>();

    public boolean hasSwitch() {
        return !this.switches.isEmpty();
    }
}
