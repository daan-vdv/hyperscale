package dev.daan.hyperscale.item;

public record CpuSpec(int cores, double clockSpeed, int cacheRamGb, double energyEfficiency, int tintColor) {
    public static final CpuSpec EDGE_4C = new CpuSpec(4, 0.90D, 0, 1.00D, 0xFF90A4AE);
    public static final CpuSpec SERVER_8C = new CpuSpec(8, 1.00D, 0, 1.00D, 0xFFFFFFFF);
    public static final CpuSpec XEON_16C = new CpuSpec(16, 1.25D, 2, 0.95D, 0xFF4FC3F7);
    public static final CpuSpec EPYC_64C = new CpuSpec(64, 1.60D, 8, 0.90D, 0xFFBA68C8);
    public static final CpuSpec QUANTUM_128C = new CpuSpec(128, 2.20D, 16, 0.80D, 0xFFFFCA28);
}
