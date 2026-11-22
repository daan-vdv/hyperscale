package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ServerRackBlockEntity extends InventoryBlockEntity {
    public ServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_RACK.get(), pos, state, 3);
    }

    public boolean hasServer() {
        return this.items.getStackInSlot(0).is(ModItems.SERVER.get());
    }

    public boolean hasCpu() {
        return this.items.getStackInSlot(1).is(ModItems.CPU.get());
    }

    public boolean hasRam() {
        return this.items.getStackInSlot(2).is(ModItems.RAM.get());
    }

    public boolean isReady() {
        return this.hasServer() && this.hasCpu() && this.hasRam();
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> stack.is(ModItems.SERVER.get());
            case 1 -> stack.is(ModItems.CPU.get());
            case 2 -> stack.is(ModItems.RAM.get());
            default -> false;
        };
    }
}
