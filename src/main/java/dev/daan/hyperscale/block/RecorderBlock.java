package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.menu.RecorderMenu;
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
import net.minecraft.world.inventory.ContainerData;
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

public class RecorderBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;

    public RecorderBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
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
        return new RecorderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.RECORDER.get()) {
            return null;
        }
        return (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof RecorderBlockEntity recorder) {
                RecorderBlockEntity.serverTick(recorder);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (BinderItem.apply(level, pos, player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        open(level, pos, player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        open(level, pos, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof RecorderBlockEntity recorder) {
            recorder.scanSealedRoom();
            serverPlayer.openMenu(new SimpleMenuProvider(
                    (containerId, inventory, ignored) -> new RecorderMenu(containerId, inventory, pos, recorder.items(), recorderData(recorder)),
                    Component.translatable("block.hyperscale.recorder")
            ), buffer -> buffer.writeBlockPos(pos));
        }
    }

    private static ContainerData recorderData(RecorderBlockEntity recorder) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return recorder.data(index);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return RecorderMenu.DATA_SIZE;
            }
        };
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RecorderBlockEntity recorder) {
            recorder.release();
            recorder.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
