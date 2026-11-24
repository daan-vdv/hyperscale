package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.WatercoolerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class WatercoolerBlock extends DecorativeFacingBlock implements EntityBlock {
    public WatercoolerBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WatercoolerBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler) {
            showStatus(level, pos, player, cooler);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler) {
            if (FluidUtil.interactWithFluidHandler(player, hand, cooler.tank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (!level.isClientSide) {
                showStatus(level, pos, player, cooler);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void showStatus(Level level, BlockPos pos, Player player, WatercoolerBlockEntity cooler) {
        int fans = WatercoolerBlockEntity.adjacentRadiatorFans(level, pos);
        int savingsPct = Math.min(75, fans * 25);
        player.displayClientMessage(
                Component.translatable(
                        "message.hyperscale.watercooler.status",
                        cooler.coolantAmount(),
                        WatercoolerBlockEntity.CAPACITY_MB,
                        savingsPct
                ),
                true
        );
    }
}
