package dev.daan.hyperscale.menu;

import dev.daan.hyperscale.blockentity.LithographyEtcherBlockEntity;
import dev.daan.hyperscale.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class LithographyMenu extends AbstractContainerMenu {
    private static final int MACHINE_SLOTS = 4;

    private final BlockPos pos;
    private final ContainerData data;

    public LithographyMenu(int containerId, Inventory playerInventory, LithographyEtcherBlockEntity etcher) {
        this(containerId, playerInventory, etcher.getBlockPos(), etcher.items(), etcher.data);
    }

    public static LithographyMenu fromBuffer(int containerId, Inventory playerInventory, FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        if (playerInventory.player.level().getBlockEntity(pos) instanceof LithographyEtcherBlockEntity etcher) {
            return new LithographyMenu(containerId, playerInventory, etcher);
        }
        return new LithographyMenu(containerId, playerInventory, pos, new ItemStackHandler(MACHINE_SLOTS), new SimpleContainerData(6));
    }

    private LithographyMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler handler, ContainerData data) {
        super(ModMenus.LITHOGRAPHY.get(), containerId);
        this.pos = pos;
        this.data = data;

        // Mask (top), Wafer (mid-left), Catalyst (bottom-left), Output (right)
        this.addSlot(new SlotItemHandler(handler, LithographyEtcherBlockEntity.SLOT_MASK, 44, 19) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LithographyEtcherBlockEntity.isPhotomask(stack);
            }
        });
        this.addSlot(new SlotItemHandler(handler, LithographyEtcherBlockEntity.SLOT_WAFER, 44, 43) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return LithographyEtcherBlockEntity.isWafer(stack);
            }
        });
        this.addSlot(new SlotItemHandler(handler, LithographyEtcherBlockEntity.SLOT_CATALYST, 68, 43));
        this.addSlot(new SlotItemHandler(handler, LithographyEtcherBlockEntity.SLOT_OUTPUT, 126, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        this.addDataSlots(this.data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int progress() {
        return this.data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, this.data.get(1));
    }

    public int energyStored() {
        return this.data.get(2) * 10;
    }

    public int maxEnergy() {
        return Math.max(1, this.data.get(3) * 10);
    }

    public int waterStored() {
        return this.data.get(4);
    }

    public int maxWater() {
        return Math.max(1, this.data.get(5));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!this.moveItemStackTo(stack, MACHINE_SLOTS, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (LithographyEtcherBlockEntity.isPhotomask(stack)) {
                if (!this.moveItemStackTo(stack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (LithographyEtcherBlockEntity.isWafer(stack)) {
                if (!this.moveItemStackTo(stack, 1, 2, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.moveItemStackTo(stack, 2, 3, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }
}
