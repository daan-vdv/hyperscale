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
            case DRIVE -> stack.is(ModItems.IMAGE.get()) || stack.is(ModItems.HDD.get());
            case RACK -> stack.is(ModItems.SERVER.get()) || stack.is(ModItems.CPU.get()) || stack.is(ModItems.RAM.get());
            case PORT, PROBE -> false;
        };
    }
}
