package dev.daan.hyperscale.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

public class InventoryBlockEntity extends BlockEntity {
    private final int expectedSlots;
    private boolean suppressItemCallbacks;
    protected final ItemStackHandler items;

    public InventoryBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots) {
        super(type, pos, state);
        this.expectedSlots = slots;
        this.items = new ItemStackHandler(slots) {
            @Override
            protected void onContentsChanged(int slot) {
                if (!InventoryBlockEntity.this.suppressItemCallbacks) {
                    InventoryBlockEntity.this.onItemsChanged(slot);
                }
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return InventoryBlockEntity.this.isItemValid(slot, stack);
            }

            @Override
            public int getSlotLimit(int slot) {
                return InventoryBlockEntity.this.slotLimit(slot);
            }
        };
    }

    protected void onItemsChanged(int slot) {
        this.sync();
    }

    protected int slotLimit(int slot) {
        return 64;
    }

    protected boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    public ItemStackHandler items() {
        return this.items;
    }

    public boolean insertOne(ItemStack stack, boolean creative) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            if (this.items.getStackInSlot(slot).isEmpty() && this.items.isItemValid(slot, stack)) {
                ItemStack single = creative ? stack.copyWithCount(1) : stack.split(1);
                this.items.setStackInSlot(slot, single);
                return true;
            }
        }
        return false;
    }

    public void dropContents() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.suppressItemCallbacks = true;
        try {
            for (int slot = 0; slot < this.items.getSlots(); slot++) {
                ItemStack stack = this.items.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(this.level, this.worldPosition, stack.copy());
                    this.items.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        } finally {
            this.suppressItemCallbacks = false;
        }
    }

    public void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Inventory", this.items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Inventory", Tag.TAG_COMPOUND)) {
            CompoundTag invTag = tag.getCompound("Inventory").copy();
            invTag.putInt("Size", this.expectedSlots);
            this.items.deserializeNBT(registries, invTag);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
