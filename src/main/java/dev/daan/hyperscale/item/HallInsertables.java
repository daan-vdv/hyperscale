package dev.daan.hyperscale.item;

import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.world.item.ItemStack;

public final class HallInsertables {
    private HallInsertables() {
    }

    public static boolean isBinder(ItemStack stack) {
        return stack.is(ModItems.BINDER.get());
    }

    public static boolean insertsInto(StorageKind kind, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return switch (kind) {
            case DRIVE -> stack.getItem() instanceof StorageDriveItem;
            case REGISTRY, SWITCH -> stack.is(ModItems.IMAGE.get());
            case RACK -> stack.is(ModItems.SERVER.get())
                    || stack.getItem() instanceof CpuItem
                    || stack.getItem() instanceof RamItem
                    || stack.getItem() instanceof GpuItem;
            case PORT, PROBE -> false;
        };
    }
}
