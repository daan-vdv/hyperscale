package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ImageRegistryBlockEntity extends InventoryBlockEntity {
    public static final int SLOTS = 9;

    public ImageRegistryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.IMAGE_REGISTRY.get(), pos, state, SLOTS);
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
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.is(ModItems.IMAGE.get()) && stack.has(ModDataComponents.PROCESS_IMAGE.get())) {
                count++;
            }
        }
        return count;
    }
}
