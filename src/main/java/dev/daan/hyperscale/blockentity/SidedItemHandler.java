package dev.daan.hyperscale.blockentity;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class SidedItemHandler implements IItemHandler {
    private final ItemStackHandler delegate;
    private final boolean canInsert;
    private final boolean canExtract;

    public SidedItemHandler(ItemStackHandler delegate, boolean canInsert, boolean canExtract) {
        this.delegate = delegate;
        this.canInsert = canInsert;
        this.canExtract = canExtract;
    }

    @Override
    public int getSlots() {
        return this.delegate.getSlots();
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return this.delegate.getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        if (!this.canInsert) {
            return stack;
        }
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!this.canExtract) {
            return ItemStack.EMPTY;
        }
        return this.delegate.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.canInsert && this.delegate.isItemValid(slot, stack);
    }
}
