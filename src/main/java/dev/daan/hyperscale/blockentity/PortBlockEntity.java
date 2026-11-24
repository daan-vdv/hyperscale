package dev.daan.hyperscale.blockentity;

import java.util.ArrayList;
import java.util.List;

import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public class PortBlockEntity extends InventoryBlockEntity {
    public static final int FILTER_SLOTS = 3;

    private final IItemHandler inputHandler = new SidedItemHandler(this.items, true, false);
    private final IItemHandler outputHandler = new SidedItemHandler(this.items, false, true);
    private final ItemStackHandler filter = new ItemStackHandler(FILTER_SLOTS) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private int filterCursor;

    public PortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORT.get(), pos, state, 1);
    }

    public IItemHandler capability() {
        return this.getBlockState().getValue(PortBlock.MODE) == PortMode.INPUT
                ? this.inputHandler
                : this.outputHandler;
    }

    public ItemStack getFilterSlot(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) {
            return ItemStack.EMPTY;
        }
        return this.filter.getStackInSlot(slot);
    }

    public void setFilterSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= FILTER_SLOTS) {
            return;
        }
        ItemStack ghost = (stack == null || stack.isEmpty()) ? ItemStack.EMPTY : stack.copyWithCount(1);
        this.filter.setStackInSlot(slot, ghost);
        this.sync();
    }

    public boolean hasFilter() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (!this.filter.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public List<ResourceLocation> filterItemIds() {
        List<ResourceLocation> ids = new ArrayList<>(FILTER_SLOTS);
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack stack = this.filter.getStackInSlot(i);
            if (!stack.isEmpty()) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (!ids.contains(id)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    public boolean allowsExport(ResourceLocation itemId) {
        if (!this.hasFilter()) {
            return true;
        }
        for (int i = 0; i < FILTER_SLOTS; i++) {
            ItemStack stack = this.filter.getStackInSlot(i);
            if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).equals(itemId)) {
                return true;
            }
        }
        return false;
    }

    public int nextFilterCursor(int size) {
        if (size <= 0) {
            return 0;
        }
        int current = Math.floorMod(this.filterCursor, size);
        this.filterCursor = (current + 1) % size;
        return current;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Filter", this.filter.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Filter", Tag.TAG_COMPOUND)) {
            CompoundTag filterTag = tag.getCompound("Filter").copy();
            filterTag.putInt("Size", FILTER_SLOTS);
            this.filter.deserializeNBT(registries, filterTag);
        }
    }
}
