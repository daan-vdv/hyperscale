package dev.daan.hyperscale.menu;

import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class RecorderMenu extends AbstractContainerMenu {
    public static final int DATA_SIZE = 12;

    private final BlockPos pos;
    private final ContainerData data;

    public RecorderMenu(int containerId, Inventory playerInventory, BlockPos pos, ItemStackHandler handler, ContainerData data) {
        super(ModMenus.RECORDER.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.addSlot(new SlotItemHandler(handler, 0, 26, 78) {
            @Override
            public boolean mayPickup(Player player) {
                return !RecorderMenu.this.learning();
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return !RecorderMenu.this.learning() && super.mayPlace(stack);
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 124 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 182));
        }
        this.addDataSlots(data);
    }

    public static RecorderMenu fromBuffer(int containerId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        return new RecorderMenu(containerId, playerInventory, buffer.readBlockPos(), new ItemStackHandler(1), new SimpleContainerData(DATA_SIZE));
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int data(int index) {
        return this.data.get(index);
    }

    public boolean learning() {
        return this.data.get(0) == RecordPhase.LEARNING.ordinal();
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!(player.level().getBlockEntity(this.pos) instanceof RecorderBlockEntity recorder)) {
            return false;
        }
        switch (id) {
            case 2 -> recorder.startOrContinue();
            case 3 -> recorder.stop();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.pos).is(ModBlocks.RECORDER.get())
                && player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!this.moveItemStackTo(stack, 1, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
