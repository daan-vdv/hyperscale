package dev.daan.hyperscale.hall;

import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.item.CpuSpec;
import dev.daan.hyperscale.item.GpuSpec;
import dev.daan.hyperscale.item.RamSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * Aggregated hardware pool across all ready Server Racks in a datacenter hall.
 */
public record HallCapacity(
        int readyRacks,
        int totalRamGb,
        int totalVcpu,
        int totalTflops,
        double speedMultiplier,
        double energyEfficiency,
        int gpuActiveFePerTick
) {
    public static final HallCapacity EMPTY = new HallCapacity(0, 0, 0, 0, 1.0D, 1.0D, 0);

    public static HallCapacity compute(Level level, HallParts parts) {
        int ready = 0;
        int totalRam = 0;
        int totalVcpu = 0;
        int totalTflops = 0;
        double speedSum = 0.0D;
        double gpuAccelSum = 0.0D;
        double efficiencySum = 0.0D;
        int gpuFe = 0;

        for (BlockPos pos : parts.racks) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack)) {
                continue;
            }
            if (!rack.isReady()) {
                continue;
            }
            CpuSpec cpu = rack.cpuSpec();
            RamSpec ram = rack.ramSpec();
            GpuSpec gpu = rack.gpuSpec();
            if (cpu == null || ram == null) {
                continue;
            }
            ready++;
            totalRam += ram.capacityGb() + cpu.cacheRamGb();
            totalVcpu += cpu.cores();
            speedSum += cpu.clockSpeed() * ram.speedMultiplier();
            efficiencySum += cpu.energyEfficiency();
            if (gpu != null) {
                totalRam += gpu.vramGb();
                totalVcpu += gpu.computeThreads();
                totalTflops += gpu.tflops();
                gpuAccelSum += gpu.speedBonus();
                gpuFe += gpu.activeFePerTick();
            }
        }

        if (ready == 0) {
            return EMPTY;
        }
        double avgBaseSpeed = speedSum / ready;
        double gpuFactor = 1.0D + (gpuAccelSum / ready);
        double effectiveSpeed = Math.max(0.25D, avgBaseSpeed * gpuFactor);
        double avgEfficiency = Math.max(0.5D, efficiencySum / ready);
        return new HallCapacity(ready, totalRam, totalVcpu, totalTflops, effectiveSpeed, avgEfficiency, gpuFe);
    }
}
