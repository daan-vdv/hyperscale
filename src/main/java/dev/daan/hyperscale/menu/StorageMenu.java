package dev.daan.hyperscale.menu;

import dev.daan.hyperscale.block.RecorderBlock;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class StorageMenu extends AbstractContainerMenu {
    public static final int BUTTON_BACK = 1;

    private final BlockPos pos;
    private final BlockPos accessPos;
    private final StorageKind kind;
    private final int machineSlots;

    public StorageMenu(int containerId, Inventory playerInventory, BlockPos pos, ItemStackHandler handler, StorageKind kind) {
        this(containerId, playerInventory, pos, pos, handler, kind);
    }

    public StorageMenu(int containerId, Inventory playerInventory, BlockPos pos, BlockPos accessPos, ItemStackHandler handler, StorageKind kind) {
        super(kind.menu(), containerId);
        this.pos = pos;
        this.accessPos = accessPos;
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
                y = 22 + (slot / 3) * 18;
            } else if (this.machineSlots == 4) {
                x = 17 + slot * 42;
                y = 40;
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

    public static StorageMenu fromBuffer(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer, StorageKind kind) {
        BlockPos pos = buffer.readBlockPos();
        BlockPos accessPos = kind == StorageKind.PROBE ? buffer.readBlockPos() : pos;
        return new StorageMenu(containerId, playerInventory, pos, accessPos, clientHandler(kind), kind);
    }

    private static ItemStackHandler clientHandler(StorageKind kind) {
        return new ItemStackHandler(kind.slots()) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (kind) {
                    case DRIVE -> stack.getItem() instanceof StorageDriveItem;
                    case REGISTRY, SWITCH -> stack.is(ModItems.IMAGE.get());
                    case RACK -> switch (slot) {
                        case 0 -> stack.is(ModItems.SERVER.get());
                        case 1 -> stack.getItem() instanceof CpuItem;
                        case 2 -> stack.getItem() instanceof RamItem;
                        case 3 -> stack.getItem() instanceof GpuItem;
                        default -> false;
                    };
                    case PORT, PROBE -> true;
                };
            }

            @Override
            public int getSlotLimit(int slot) {
                return (kind == StorageKind.DRIVE || kind == StorageKind.RACK || kind == StorageKind.REGISTRY || kind == StorageKind.SWITCH) ? 1 : 64;
            }
        };
    }

    public BlockPos pos() {
        return this.pos;
    }

    public StorageKind kind() {
        return this.kind;
    }

    public boolean isRemoteProbe() {
        return this.kind == StorageKind.PROBE && !this.pos.equals(this.accessPos);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == BUTTON_BACK && this.isRemoteProbe() && player instanceof ServerPlayer serverPlayer) {
            RecorderBlock.open(serverPlayer.level(), this.accessPos, serverPlayer);
            return true;
        }
        return false;
    }

    @Override
    public boolean stillValid(Player player) {
        if (this.kind == StorageKind.PROBE && RecorderBlockEntity.blocksPlayerAccess(player.level(), this.pos)) {
            return false;
        }
        return player.level().isLoaded(this.pos)
                && this.kind.matches(player.level().getBlockState(this.pos))
                && player.distanceToSqr(this.accessPos.getX() + 0.5D, this.accessPos.getY() + 0.5D, this.accessPos.getZ() + 0.5D) <= 64.0D;
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
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
