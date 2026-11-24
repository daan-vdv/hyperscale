package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.block.ProbeMode;
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
        ItemStack remainder = this.delegate.insertItem(slot, stack, simulate);
        if (!simulate && this.probe.countsAsOutput(this.side)) {
            int moved = stack.getCount() - remainder.getCount();
            if (moved > 0) {
                this.probe.notifyTransfer(stack.copyWithCount(moved));
            }
        }
        return remainder;
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
        return this.canInsert() && this.delegate.isItemValid(slot, stack);
    }

    private boolean canInsert() {
        if (this.probe.mode() == ProbeMode.INPUT) {
            // Allow loading Input probes from exterior faces when not sampling
            return !this.probe.facesInterior(this.side) && !this.probe.isSampling();
        }
        // Allow interior pipes (e.g. Mekanism Logistical Transporters) to push into Output probes while sampling
        return this.probe.facesInterior(this.side) && this.probe.isSampling();
    }

    private boolean canExtract() {
        if (this.probe.mode() == ProbeMode.INPUT) {
            // Only let interior pipes pull from Input probes while sampling so batches don't drain early
            return this.probe.facesInterior(this.side) && this.probe.isSampling();
        }
        // Allow emptying Output probes from exterior faces when not sampling
        return !this.probe.facesInterior(this.side) && !this.probe.isSampling();
    }
}
