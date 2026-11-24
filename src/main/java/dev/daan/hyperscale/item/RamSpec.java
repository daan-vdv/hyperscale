package dev.daan.hyperscale.item;

public record RamSpec(int capacityGb, String generation, double speedMultiplier, int tintColor) {
    public static final RamSpec DDR4_4GB = new RamSpec(4, "DDR4", 1.00D, 0xFF7FA8C9);
    public static final RamSpec DDR5_4GB = new RamSpec(4, "DDR5", 1.25D, 0xFF56CCF2);
    public static final RamSpec DDR4_16GB = new RamSpec(16, "DDR4", 1.00D, 0xFFFFFFFF);
    public static final RamSpec DDR4_64GB = new RamSpec(64, "DDR4", 1.05D, 0xFF64B5F6);
    public static final RamSpec DDR5_64GB = new RamSpec(64, "DDR5", 1.25D, 0xFF4DD0E1);
    public static final RamSpec DDR5_256GB = new RamSpec(256, "DDR5", 1.30D, 0xFFB388FF);
    public static final RamSpec DDR3_512GB = new RamSpec(512, "DDR3 ECC", 0.85D, 0xFF81C784);
    public static final RamSpec DDR5_512GB = new RamSpec(512, "DDR5 ECC", 1.35D, 0xFFFFD54F);
}
