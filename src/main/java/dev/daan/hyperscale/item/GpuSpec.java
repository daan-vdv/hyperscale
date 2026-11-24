package dev.daan.hyperscale.item;

public record GpuSpec(int tflops, int vramGb, int computeThreads, double speedBonus, int activeFePerTick, int tintColor) {
    public static final GpuSpec COMPUTE_T1 = new GpuSpec(16, 12, 4, 0.35D, 20, 0xFF4CAF50);
    public static final GpuSpec DATACENTER_T2 = new GpuSpec(64, 48, 16, 0.80D, 80, 0xFF29B6F6);
    public static final GpuSpec TENSOR_T3 = new GpuSpec(256, 192, 64, 1.60D, 250, 0xFFE040FB);
}
