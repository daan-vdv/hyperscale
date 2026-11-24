package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.FluidPortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public class FluidPortBlock extends Block implements EntityBlock {
    public static final EnumProperty<PortMode> MODE = EnumProperty.create("mode", PortMode.class);

    public FluidPortBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, PortMode.INPUT));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPortBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        this.cycle(state, level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof FluidPortBlockEntity fluidPort) {
            if (FluidUtil.interactWithFluidHandler(player, hand, fluidPort.tank())) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        this.cycle(state, level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void cycle(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return;
        }
        BlockState cycled = state.cycle(MODE);
        level.setBlock(pos, cycled, Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        player.displayClientMessage(
                Component.translatable("message.hyperscale.fluid_port." + cycled.getValue(MODE).getSerializedName()),
                true
        );
    }
}
