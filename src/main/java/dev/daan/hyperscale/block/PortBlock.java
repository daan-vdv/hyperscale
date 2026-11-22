package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.item.HallInsertables;
import dev.daan.hyperscale.menu.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

public class PortBlock extends InventoryMachineBlock {
    public static final EnumProperty<PortMode> MODE = EnumProperty.create("mode", PortMode.class);

    public PortBlock(Properties properties) {
        super(properties, StorageKind.PORT);
        this.registerDefaultState(this.stateDefinition.any().setValue(MODE, PortMode.INPUT));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PortBlockEntity(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(MODE);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            this.open(level, pos, player);
            return InteractionResult.SUCCESS;
        }
        this.cycle(state, level, pos, player);
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (HallInsertables.isBinder(stack)) {
            return BinderItem.apply(level, pos, player, stack)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (player.isShiftKeyDown() || !stack.isEmpty()) {
            this.open(level, pos, player);
            return ItemInteractionResult.SUCCESS;
        }
        this.cycle(state, level, pos, player);
        return ItemInteractionResult.SUCCESS;
    }

    private void cycle(BlockState state, Level level, BlockPos pos, Player player) {
        if (level.isClientSide) {
            return;
        }
        BlockState cycled = state.cycle(MODE);
        level.setBlock(pos, cycled, net.minecraft.world.level.block.Block.UPDATE_ALL);
        level.invalidateCapabilities(pos);
        player.displayClientMessage(
                Component.translatable("message.hyperscale.port." + cycled.getValue(MODE).getSerializedName()),
                true
        );
    }
}
