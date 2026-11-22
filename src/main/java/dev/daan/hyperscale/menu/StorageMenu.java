package dev.daan.hyperscale.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StorageMenu extends AbstractContainerMenu {
    private final BlockPos pos;
    private final StorageKind kind;
    private final int machineSlots;

    public StorageMenu(int containerId, Inventory playerInventory, BlockPos pos, ItemStackHandler handler, StorageKind kind) {
        super(kind.menu(), containerId);
        this.pos = pos;
        this.kind = kind;
        this.machineSlots = kind.slots();
        for (int slot = 0; slot < this.machineSlots; slot++) {
            int x;
            int y;
            if (this.machineSlots == 9) {
                x = 62 + (slot % 3) * 18;
                y = 16 + (slot / 3) * 18;
            } else if (this.machineSlots == 6) {
                x = 62 + (slot % 3) * 18;
                y = 20 + (slot / 3) * 18;
            } else {
                x = this.machineSlots == 1 ? 80 : 26 + slot * 54;
                y = 40;
            }
            this.addSlot(new SlotItemHandler(handler, slot, x, y));
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                this.addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
    }

    public static StorageMenu fromBuffer(int containerId, Inventory playerInventory, net.minecraft.network.RegistryFriendlyByteBuf buffer, StorageKind kind) {
        return new StorageMenu(containerId, playerInventory, buffer.readBlockPos(), new ItemStackHandler(kind.slots()), kind);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public StorageKind kind() {
        return this.kind;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.kind.matches(player.level().getBlockState(this.pos))
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
        int playerStart = this.machineSlots;
        int playerEnd = playerStart + 36;
        if (index < this.machineSlots) {
            if (!this.moveItemStackTo(stack, playerStart, playerEnd, true)) {
                return ItemStack.EMPTY;
            }
        } else if (!this.moveItemStackTo(stack, 0, this.machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
