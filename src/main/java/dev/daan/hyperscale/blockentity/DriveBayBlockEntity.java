package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DriveBayBlockEntity extends InventoryBlockEntity {
    public static final int SLOTS = 6;

    public DriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRIVE_BAY.get(), pos, state, SLOTS);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.is(ModItems.IMAGE.get());
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    public int imageCount() {
        int count = 0;
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            if (this.items.getStackInSlot(slot).is(ModItems.IMAGE.get())) {
                count++;
            }
        }
        return count;
    }
}
