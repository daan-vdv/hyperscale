package dev.daan.hyperscale.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.daan.hyperscale.blockentity.InventoryBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.item.BinderItem;
import dev.daan.hyperscale.item.HallInsertables;
import dev.daan.hyperscale.menu.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2-tall rack. Lower half holds the block entity and full model; upper half is a
 * collision/interaction sibling (same pattern as doors) so the top is solid and clickable.
 */
public class ServerRackBlock extends InventoryMachineBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;

    /**
     * One cell of the rack (XZ from the model, full 1-block height).
     * Lower and upper halves each use this in local space.
     */
    private static final VoxelShape SHAPE_NORTH_CELL = Shapes.box(
            1.0D / 16.0D, 0.0D, 0.65D / 16.0D,
            15.0D / 16.0D, 1.0D, 15.0D / 16.0D
    );
    private static final Map<Direction, VoxelShape> CELL_SHAPES = new ConcurrentHashMap<>();
    private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public ServerRackBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK), StorageKind.RACK);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER));
    }

    public static boolean isLower(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER;
    }

    public static BlockPos basePos(BlockPos pos, BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    private static VoxelShape shapeFor(Direction facing) {
        return CELL_SHAPES.computeIfAbsent(facing, direction -> rotate(SHAPE_NORTH_CELL, direction));
    }

    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        VoxelShape result = shape;
        int times = (facing.get2DDataValue() - Direction.NORTH.get2DDataValue() + 4) % 4;
        for (int i = 0; i < times; i++) {
            result = rotateOnce(result);
        }
        return result;
    }

    private static VoxelShape rotateOnce(VoxelShape shape) {
        VoxelShape[] buffer = {Shapes.empty()};
        shape.forAllBoxes((minX, minY, minZ, maxX, maxY, maxZ) ->
                buffer[0] = Shapes.or(buffer[0], Shapes.box(1.0D - maxZ, minY, minX, 1.0D - minZ, maxY, maxX)));
        return buffer[0];
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockPos origin = context.getClickedPos();
        Level level = context.getLevel();
        if (origin.getY() > level.getMaxBuildHeight() - 1 || !this.fits(level, origin)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, DoubleBlockHalf.LOWER);
    }

    private boolean fits(Level level, BlockPos origin) {
        BlockState above = level.getBlockState(origin.above());
        return above.canBeReplaced() || above.isAir();
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Match DoorBlock: place upper on client and server so prediction/collision match.
        if (isLower(state)) {
            level.setBlock(pos.above(), state.setValue(HALF, DoubleBlockHalf.UPPER), Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos base = basePos(pos, state);
            if (!isLower(state) && !player.isCreative()) {
                BlockState baseState = level.getBlockState(base);
                if (baseState.is(this) && isLower(baseState)) {
                    dropResources(baseState, level, base, level.getBlockEntity(base), player, player.getMainHandItem());
                }
            }
            this.clearSibling(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            this.clearSibling(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void clearSibling(Level level, BlockPos pos, BlockState state) {
        if (CLEARING.get()) {
            return;
        }
        CLEARING.set(Boolean.TRUE);
        try {
            BlockPos other = isLower(state) ? pos.above() : pos.below();
            BlockState otherState = level.getBlockState(other);
            if (otherState.is(this) && otherState.getValue(FACING) == state.getValue(FACING)) {
                level.setBlock(other, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
            }
        } finally {
            CLEARING.set(Boolean.FALSE);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        return super.useWithoutItem(state, level, basePos(pos, state), player, hitResult);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos base = basePos(pos, state);
        if (player.isShiftKeyDown()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (HallInsertables.isBinder(stack)) {
            return BinderItem.apply(level, base, player, stack)
                    ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (HallInsertables.insertsInto(this.kind(), stack)) {
            if (player instanceof ServerPlayer && level.getBlockEntity(base) instanceof InventoryBlockEntity inventory) {
                if (!inventory.insertOne(player.getItemInHand(hand))) {
                    this.open(level, base, player);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        this.open(level, base, player);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
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
        builder.add(FACING, HALF);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isLower(state) ? new ServerRackBlockEntity(pos, state) : null;
    }
}
