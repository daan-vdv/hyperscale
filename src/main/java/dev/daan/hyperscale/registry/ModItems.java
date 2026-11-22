package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.item.EmptyImageCardItem;
import dev.daan.hyperscale.item.HddItem;
import dev.daan.hyperscale.item.ImageItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Hyperscale.MOD_ID);

    public static final DeferredItem<BlockItem> INTERFACE = ITEMS.registerSimpleBlockItem(ModBlocks.INTERFACE);
    public static final DeferredItem<BlockItem> PORT = ITEMS.registerSimpleBlockItem(ModBlocks.PORT);
    public static final DeferredItem<BlockItem> SWITCH = ITEMS.registerSimpleBlockItem(ModBlocks.SWITCH);
    public static final DeferredItem<BlockItem> SERVER_RACK = ITEMS.registerSimpleBlockItem(ModBlocks.SERVER_RACK);
    public static final DeferredItem<BlockItem> DRIVE_BAY = ITEMS.registerSimpleBlockItem(ModBlocks.DRIVE_BAY);
    public static final DeferredItem<BlockItem> RECORDER = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER);
    public static final DeferredItem<BlockItem> RECORDER_INPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_INPUT);
    public static final DeferredItem<BlockItem> RECORDER_OUTPUT = ITEMS.registerSimpleBlockItem(ModBlocks.RECORDER_OUTPUT);

    public static final DeferredItem<Item> SERVER = ITEMS.registerSimpleItem("server", new Item.Properties());
    public static final DeferredItem<EmptyImageCardItem> EMPTY_IMAGE_CARD = ITEMS.register("empty_image_card",
            () -> new EmptyImageCardItem(new Item.Properties().stacksTo(16)));
    public static final DeferredItem<ImageItem> IMAGE = ITEMS.register("image",
            () -> new ImageItem(new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.UNCOMMON)));
    public static final DeferredItem<BinderItem> BINDER = ITEMS.register("binder",
            () -> new BinderItem(new Item.Properties().stacksTo(1)));
    public static final DeferredItem<HddItem> HDD = ITEMS.register("hdd", () -> new HddItem(new Item.Properties()));
    public static final DeferredItem<Item> CPU = ITEMS.registerSimpleItem("cpu", new Item.Properties());
    public static final DeferredItem<Item> RAM = ITEMS.registerSimpleItem("ram", new Item.Properties());

    private ModItems() {
    }
}
