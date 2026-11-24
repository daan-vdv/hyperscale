package dev.daan.hyperscale.menu;

import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallParts;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InterfaceMenu extends AbstractContainerMenu {
    private final BlockPos pos;

    public InterfaceMenu(int containerId, Inventory inventory, BlockPos pos) {
        super(ModMenus.INTERFACE.get(), containerId);
        this.pos = pos;
    }

    public static InterfaceMenu fromBuffer(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        return new InterfaceMenu(containerId, inventory, buffer.readBlockPos());
    }

    public static InterfaceMenu create(int containerId, Inventory inventory, BlockPos pos) {
        Level level = inventory.player.level();
        if (!level.isClientSide) {
            HallParts parts = HallFinder.find(level, pos);
            if (parts.hasSwitch() && level.getBlockEntity(parts.switches.get(0)) instanceof SwitchBlockEntity switchEntity) {
                switchEntity.sync();
            }
            for (BlockPos drivePos : parts.drives) {
                if (level.isLoaded(drivePos) && level.getBlockEntity(drivePos) instanceof DriveBayBlockEntity bay) {
                    bay.sync();
                }
            }
        }
        return new InterfaceMenu(containerId, inventory, pos);
    }

    public BlockPos pos() {
        return this.pos;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.pos).is(ModBlocks.INTERFACE.get())
                && player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
