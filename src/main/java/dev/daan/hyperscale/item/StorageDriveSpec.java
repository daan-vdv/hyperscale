package dev.daan.hyperscale.item;

public record StorageDriveSpec(
        String capacityLabel,
        String driveType,
        int maxItems,
        int maxTypes,
        int ioPerTick,
        int tintColor
) {
    // Enterprise HDDs: huge cheap capacity, slower spinning-disk I/O
    public static final StorageDriveSpec HDD_64GB = new StorageDriveSpec("64 GB", "HDD", 16_384, 16, 16, 0xFFFFFFFF);
    public static final StorageDriveSpec HDD_256GB = new StorageDriveSpec("256 GB", "HDD", 65_536, 32, 24, 0xFF90CAF9);
    public static final StorageDriveSpec HDD_1TB = new StorageDriveSpec("1 TB", "HDD", 262_144, 64, 32, 0xFFFFD54F);

    // SATA SSDs: balanced capacity and fast solid-state I/O
    public static final StorageDriveSpec SSD_16GB = new StorageDriveSpec("16 GB", "SATA SSD", 4_096, 16, 64, 0xFF80DEEA);
    public static final StorageDriveSpec SSD_64GB = new StorageDriveSpec("64 GB", "SATA SSD", 16_384, 32, 128, 0xFF4DD0E1);

    // NVMe SSDs: ultra-fast PCIe I/O for high-replica datacenters
    public static final StorageDriveSpec NVME_64GB = new StorageDriveSpec("64 GB", "NVMe SSD", 16_384, 32, 256, 0xFFCE93D8);
    public static final StorageDriveSpec NVME_256GB = new StorageDriveSpec("256 GB", "NVMe SSD", 65_536, 64, 512, 0xFFFF8A65);
}
