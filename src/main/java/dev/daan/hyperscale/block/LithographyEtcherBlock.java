package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.LithographyEtcherBlockEntity;
import dev.daan.hyperscale.menu.LithographyMenu;
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
import net.neoforged.neoforge.fluids.FluidUtil;

public class LithographyEtcherBlock extends DecorativeFacingBlock implements EntityBlock {
    public LithographyEtcherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LithographyEtcherBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.LITHOGRAPHY_ETCHER.get()) {
            return null;
        }
        return (lvl, pos, st, be) -> {
            if (be instanceof LithographyEtcherBlockEntity etcher) {
                LithographyEtcherBlockEntity.serverTick(etcher);
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
        if (level.getBlockEntity(pos) instanceof LithographyEtcherBlockEntity etcher) {
            if (FluidUtil.interactWithFluidHandler(player, hand, etcher.tank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof LithographyEtcherBlockEntity etcher)) {
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inv, ignored) -> new LithographyMenu(containerId, inv, etcher),
                Component.translatable(this.getDescriptionId())
        ), buffer -> buffer.writeBlockPos(pos));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof LithographyEtcherBlockEntity etcher) {
            etcher.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
