package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.InventoryBlockEntity;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.item.HallInsertables;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public abstract class InventoryMachineBlock extends Block implements EntityBlock {
    private final StorageKind kind;

    protected InventoryMachineBlock(Properties properties, StorageKind kind) {
        super(properties);
        this.kind = kind;
    }

    protected StorageKind kind() {
        return this.kind;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        this.open(level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (HallInsertables.isBinder(stack)) {
            return BinderItem.apply(level, pos, player, stack)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (HallInsertables.insertsInto(this.kind, stack)) {
            if (player instanceof ServerPlayer && level.getBlockEntity(pos) instanceof InventoryBlockEntity inventory) {
                if (!inventory.insertOne(player.getItemInHand(hand))) {
                    this.open(level, pos, player);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        this.open(level, pos, player);
        return ItemInteractionResult.SUCCESS;
    }

    protected void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof InventoryBlockEntity inventory) {
            this.open(serverPlayer, pos, inventory);
        }
    }

    private void open(ServerPlayer player, BlockPos pos, InventoryBlockEntity inventory) {
        player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, ignored) -> new StorageMenu(containerId, playerInventory, pos, inventory.items(), this.kind),
                Component.translatable(this.kind.titleKey())
        ), buffer -> buffer.writeBlockPos(pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof InventoryBlockEntity inventory) {
            inventory.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
