package dev.daan.hyperscale.blockentity;

import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class ProbeItemHandler implements IItemHandler {
    private final ProbeBlockEntity probe;
    private final ItemStackHandler delegate;
    private final Direction side;

    public ProbeItemHandler(ProbeBlockEntity probe, ItemStackHandler delegate, Direction side) {
        this.probe = probe;
        this.delegate = delegate;
        this.side = side;
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
        if (!this.canInsert() || stack.isEmpty()) {
            return stack;
        }
        // Output recipes are taught only by the probe pump pulling from claim inventories.
        // Capability inserts are ignored for counting so a hopper/player cannot stuff the probe.
        return this.delegate.insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        if (!this.canExtract()) {
            return ItemStack.EMPTY;
        }
        ItemStack extracted = this.delegate.extractItem(slot, amount, simulate);
        if (!simulate && !extracted.isEmpty() && this.probe.countsAsInput(this.side)) {
            this.probe.notifyTransfer(extracted.copy());
        }
        return extracted;
    }

    @Override
    public int getSlotLimit(int slot) {
        return this.delegate.getSlotLimit(slot);
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.delegate.isItemValid(slot, stack);
    }

    private boolean canInsert() {
        return this.probe.mode() == dev.daan.hyperscale.block.ProbeMode.INPUT
                ? !this.probe.facesInterior(this.side)
                : this.probe.facesInterior(this.side);
    }

    private boolean canExtract() {
        return this.probe.mode() == dev.daan.hyperscale.block.ProbeMode.INPUT
                ? this.probe.facesInterior(this.side)
                : !this.probe.facesInterior(this.side);
    }
}
