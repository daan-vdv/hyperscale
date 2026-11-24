package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.WaferPressBlockEntity;
import dev.daan.hyperscale.menu.WaferPressMenu;
import dev.daan.hyperscale.registry.ModBlockEntities;
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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class WaferPressBlock extends DecorativeFacingBlock implements EntityBlock {
    public WaferPressBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WaferPressBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.WAFER_PRESS.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof WaferPressBlockEntity press) {
                WaferPressBlockEntity.serverTick(press);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof WaferPressBlockEntity press)) {
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inv, ignored) -> new WaferPressMenu(containerId, inv, press),
                Component.translatable(this.getDescriptionId())
        ), buffer -> buffer.writeBlockPos(pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof WaferPressBlockEntity press) {
            press.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
