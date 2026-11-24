package dev.daan.hyperscale.registry;

import java.util.List;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.CpuSpec;
import dev.daan.hyperscale.item.EmptyImageCardItem;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.GpuSpec;
import dev.daan.hyperscale.item.ImageItem;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.RamSpec;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.item.StorageDriveSpec;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Hyperscale.MOD_ID);

    public static final DeferredItem<BlockItem> INTERFACE = ITEMS.registerSimpleBlockItem(ModBlocks.INTERFACE);
    public static final DeferredItem<BlockItem> PORT = ITEMS.registerSimpleBlockItem(ModBlocks.PORT);
    public static final DeferredItem<BlockItem> FLUID_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.FLUID_PORT);
    public static final DeferredItem<BlockItem> SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.SWITCH);
    public static final DeferredItem<BlockItem> SERVER_RACK = ITEMS.registerSimpleBlockItem(ModBlocks.SERVER_RACK);
    public static final DeferredItem<BlockItem> DRIVE_BAY = ITEMS.registerSimpleBlockItem(ModBlocks.DRIVE_BAY);
    public static final DeferredItem<BlockItem> IMAGE_REGISTRY = ITEMS.registerSimpleBlockItem(ModBlocks.IMAGE_REGISTRY);
    public static final DeferredItem<BlockItem> RECORDER = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER);
    public static final DeferredItem<BlockItem> RECORDER_INPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_INPUT);
    public static final DeferredItem<BlockItem> RECORDER_OUTPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_OUTPUT);
    public static final DeferredItem<BlockItem> RECORDER_FLUID_INPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_FLUID_INPUT);
    public static final DeferredItem<BlockItem> RECORDER_FLUID_OUTPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_FLUID_OUTPUT);
    public static final DeferredItem<BlockItem> RECORDER_POWER = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_POWER);
    public static final DeferredItem<BlockItem> RECORDER_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_CASING);
    public static final DeferredItem<BlockItem> BACKUP_GENERATOR = ITEMS.registerSimpleBlockItem(ModBlocks.BACKUP_GENERATOR);
    public static final DeferredItem<BlockItem> CABLE = ITEMS.registerSimpleBlockItem(ModBlocks.CABLE);
    public static final DeferredItem<BlockItem> SERVER_FAN = ITEMS.registerSimpleBlockItem(ModBlocks.SERVER_FAN);
    public static final DeferredItem<BlockItem> WATERCOOLER_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.WATERCOOLER_CONTROLLER);
    public static final DeferredItem<BlockItem> WAFER_PRESS = ITEMS.registerSimpleBlockItem(ModBlocks.WAFER_PRESS);
    public static final DeferredItem<BlockItem> LITHOGRAPHY_ETCHER = ITEMS.registerSimpleBlockItem(ModBlocks.LITHOGRAPHY_ETCHER);

    // Semiconductor Fab Intermediates
    public static final DeferredItem<Item> SILICON = ITEMS.registerSimpleItem("silicon", new Item.Properties());
    public static final DeferredItem<Item> PCB = ITEMS.registerSimpleItem("pcb", new Item.Properties());
    public static final DeferredItem<Item> SILICON_WAFER = ITEMS.registerSimpleItem("silicon_wafer", new Item.Properties());
    public static final DeferredItem<Item> DOPED_WAFER = ITEMS.registerSimpleItem("doped_wafer", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> QUANTUM_WAFER = ITEMS.registerSimpleItem("quantum_wafer", new Item.Properties().rarity(Rarity.RARE));

    // Reusable Lithography Photomasks
    public static final DeferredItem<Item> MASK_LOGIC = ITEMS.registerSimpleItem("mask_logic", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MASK_MEMORY = ITEMS.registerSimpleItem("mask_memory", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MASK_STORAGE = ITEMS.registerSimpleItem("mask_storage", new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> MASK_TENSOR = ITEMS.registerSimpleItem("mask_tensor", new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON));

    // Etched Silicon Dies
    public static final DeferredItem<Item> LOGIC_DIE = ITEMS.registerSimpleItem("logic_die", new Item.Properties());
    public static final DeferredItem<Item> MEMORY_DIE = ITEMS.registerSimpleItem("memory_die", new Item.Properties());
    public static final DeferredItem<Item> NAND_DIE = ITEMS.registerSimpleItem("nand_die", new Item.Properties());
    public static final DeferredItem<Item> TENSOR_DIE = ITEMS.registerSimpleItem("tensor_die", new Item.Properties().rarity(Rarity.UNCOMMON));
    public static final DeferredItem<Item> QUANTUM_DIE = ITEMS.registerSimpleItem("quantum_die", new Item.Properties().rarity(Rarity.RARE));

    public static final DeferredItem<Item> SERVER = ITEMS.registerSimpleItem("server", new Item.Properties());
    public static final DeferredItem<EmptyImageCardItem> EMPTY_IMAGE_CARD = ITEMS.register("empty_image_card",
            () -> new EmptyImageCardItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ImageItem> IMAGE = ITEMS.register("image",
            () -> new ImageItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));
    public static final DeferredItem<BinderItem> BINDER = ITEMS.register("binder",
            () -> new BinderItem(new Item.Properties().stacksTo(1)));

    // Storage Drives (keep "hdd" ID as 64GB Enterprise HDD for backwards compatibility)
    public static final DeferredItem<StorageDriveItem> HDD = ITEMS.register("hdd",
            () -> new StorageDriveItem(new Item.Properties(), StorageDriveSpec.HDD_64GB));
    public static final DeferredItem<StorageDriveItem> HDD_256GB = ITEMS.register("hdd_256gb",
            () -> new StorageDriveItem(new Item.Properties().rarity(Rarity.UNCOMMON), StorageDriveSpec.HDD_256GB));
    public static final DeferredItem<StorageDriveItem> HDD_1TB = ITEMS.register("hdd_1tb",
            () -> new StorageDriveItem(new Item.Properties().rarity(Rarity.RARE), StorageDriveSpec.HDD_1TB));
    public static final DeferredItem<StorageDriveItem> SSD_16GB = ITEMS.register("ssd_16gb",
            () -> new StorageDriveItem(new Item.Properties(), StorageDriveSpec.SSD_16GB));
    public static final DeferredItem<StorageDriveItem> SSD_64GB = ITEMS.register("ssd_64gb",
            () -> new StorageDriveItem(new Item.Properties().rarity(Rarity.UNCOMMON), StorageDriveSpec.SSD_64GB));
    public static final DeferredItem<StorageDriveItem> NVME_64GB = ITEMS.register("nvme_64gb",
            () -> new StorageDriveItem(new Item.Properties().rarity(Rarity.RARE), StorageDriveSpec.NVME_64GB));
    public static final DeferredItem<StorageDriveItem> NVME_256GB = ITEMS.register("nvme_256gb",
            () -> new StorageDriveItem(new Item.Properties().rarity(Rarity.EPIC), StorageDriveSpec.NVME_256GB));

    // CPUs (keep "cpu" ID as 8-Core Server CPU for backwards compatibility)
    public static final DeferredItem<CpuItem> CPU_4C = ITEMS.register("cpu_4c",
            () -> new CpuItem(new Item.Properties(), CpuSpec.EDGE_4C));
    public static final DeferredItem<CpuItem> CPU = ITEMS.register("cpu",
            () -> new CpuItem(new Item.Properties(), CpuSpec.SERVER_8C));
    public static final DeferredItem<CpuItem> CPU_16C = ITEMS.register("cpu_16c",
            () -> new CpuItem(new Item.Properties().rarity(Rarity.UNCOMMON), CpuSpec.XEON_16C));
    public static final DeferredItem<CpuItem> CPU_64C = ITEMS.register("cpu_64c",
            () -> new CpuItem(new Item.Properties().rarity(Rarity.RARE), CpuSpec.EPYC_64C));
    public static final DeferredItem<CpuItem> CPU_128C = ITEMS.register("cpu_128c",
            () -> new CpuItem(new Item.Properties().rarity(Rarity.EPIC), CpuSpec.QUANTUM_128C));

    // RAMs (keep "ram" ID as 16GB DDR4 for backwards compatibility)
    public static final DeferredItem<RamItem> RAM_4GB_DDR4 = ITEMS.register("ram_4gb_ddr4",
            () -> new RamItem(new Item.Properties(), RamSpec.DDR4_4GB));
    public static final DeferredItem<RamItem> RAM_4GB_DDR5 = ITEMS.register("ram_4gb_ddr5",
            () -> new RamItem(new Item.Properties(), RamSpec.DDR5_4GB));
    public static final DeferredItem<RamItem> RAM = ITEMS.register("ram",
            () -> new RamItem(new Item.Properties(), RamSpec.DDR4_16GB));
    public static final DeferredItem<RamItem> RAM_64GB_DDR4 = ITEMS.register("ram_64gb_ddr4",
            () -> new RamItem(new Item.Properties().rarity(Rarity.UNCOMMON), RamSpec.DDR4_64GB));
    public static final DeferredItem<RamItem> RAM_64GB_DDR5 = ITEMS.register("ram_64gb_ddr5",
            () -> new RamItem(new Item.Properties().rarity(Rarity.UNCOMMON), RamSpec.DDR5_64GB));
    public static final DeferredItem<RamItem> RAM_256GB_DDR5 = ITEMS.register("ram_256gb_ddr5",
            () -> new RamItem(new Item.Properties().rarity(Rarity.RARE), RamSpec.DDR5_256GB));
    public static final DeferredItem<RamItem> RAM_512GB_DDR3 = ITEMS.register("ram_512gb_ddr3",
            () -> new RamItem(new Item.Properties().rarity(Rarity.RARE), RamSpec.DDR3_512GB));
    public static final DeferredItem<RamItem> RAM_512GB_DDR5 = ITEMS.register("ram_512gb_ddr5",
            () -> new RamItem(new Item.Properties().rarity(Rarity.EPIC), RamSpec.DDR5_512GB));

    // GPUs (optional 4th Server Rack slot)
    public static final DeferredItem<GpuItem> GPU_T1 = ITEMS.register("gpu_t1",
            () -> new GpuItem(new Item.Properties().rarity(Rarity.UNCOMMON), GpuSpec.COMPUTE_T1));
    public static final DeferredItem<GpuItem> GPU_T2 = ITEMS.register("gpu_t2",
            () -> new GpuItem(new Item.Properties().rarity(Rarity.RARE), GpuSpec.DATACENTER_T2));
    public static final DeferredItem<GpuItem> GPU_T3 = ITEMS.register("gpu_t3",
            () -> new GpuItem(new Item.Properties().rarity(Rarity.EPIC), GpuSpec.TENSOR_T3));

    public static List<DeferredItem<StorageDriveItem>> allDrives() {
        return List.of(HDD, HDD_256GB, HDD_1TB, SSD_16GB, SSD_64GB, NVME_64GB, NVME_256GB);
    }

    public static List<DeferredItem<CpuItem>> allCpus() {
        return List.of(CPU_4C, CPU, CPU_16C, CPU_64C, CPU_128C);
    }

    public static List<DeferredItem<RamItem>> allRams() {
        return List.of(RAM_4GB_DDR4, RAM_4GB_DDR5, RAM, RAM_64GB_DDR4, RAM_64GB_DDR5, RAM_256GB_DDR5, RAM_512GB_DDR3, RAM_512GB_DDR5);
    }

    public static List<DeferredItem<GpuItem>> allGpus() {
        return List.of(GPU_T1, GPU_T2, GPU_T3);
    }

    private ModItems() {
    }
}
