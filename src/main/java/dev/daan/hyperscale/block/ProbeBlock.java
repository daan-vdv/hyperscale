package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.ProbeBlockEntity;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

public class ProbeBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    private final ProbeMode mode;

    public ProbeBlock(Properties properties, ProbeMode mode) {
        super(properties);
        this.mode = mode;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public ProbeMode mode() {
        return this.mode;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = facing.getOpposite();
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ProbeBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.PROBE.get()) {
            return null;
        }
        return (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof ProbeBlockEntity probe) {
                ProbeBlockEntity.serverTick(probe);
            }
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        this.open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (BinderItem.apply(level, pos, player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        this.open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (!(player instanceof ServerPlayer serverPlayer) || !(level.getBlockEntity(pos) instanceof ProbeBlockEntity probe)) {
            return;
        }
        serverPlayer.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new StorageMenu(containerId, inventory, pos, pos, probe.items(), StorageKind.PROBE),
                Component.translatable(this.getDescriptionId())
        ), buffer -> {
            buffer.writeBlockPos(pos);
            buffer.writeBlockPos(pos);
        });
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ProbeBlockEntity probe) {
            probe.notifyRemoved();
            probe.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
