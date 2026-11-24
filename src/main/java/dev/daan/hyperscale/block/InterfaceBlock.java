package dev.daan.hyperscale.block;

import dev.daan.hyperscale.menu.InterfaceMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2-wide workbench occupying the placed block and the next block along model +X
 * (clock-wise from facing). Sibling cells carry clipped collision so overhangs are solid.
 */
public class InterfaceBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<InterfacePart> PART = EnumProperty.create("part", InterfacePart.class);

    /** Exported workbench: 2 wide (0–32), ~1.28 tall, depth 1–15. Front faces north. */
    private static final VoxelShape SHAPE_NORTH = Shapes.box(0.0D, 0.0D, 1.0D / 16.0D, 2.0D, 20.5D / 16.0D, 15.0D / 16.0D);
    private static final VoxelShape[] CLIPPED_SHAPES = buildClippedShapes();
    private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public InterfaceBlock(Properties properties) {
        super(properties.pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(PART, InterfacePart.PRIMARY));
    }

    private static VoxelShape[] buildClippedShapes() {
        VoxelShape[] shapes = new VoxelShape[16];
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            VoxelShape full = rotate(SHAPE_NORTH, facing);
            for (InterfacePart part : InterfacePart.values()) {
                int dx = part == InterfacePart.PRIMARY ? 0 : facing.getClockWise().getStepX();
                int dz = part == InterfacePart.PRIMARY ? 0 : facing.getClockWise().getStepZ();
                for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                    int dy = half == DoubleBlockHalf.UPPER ? 1 : 0;
                    shapes[shapeIndex(facing, part, half)] = clip(full, dx, dy, dz);
                }
            }
        }
        return shapes;
    }

    private static int shapeIndex(Direction facing, InterfacePart part, DoubleBlockHalf half) {
        return (facing.get2DDataValue() << 2) | (part.ordinal() << 1) | half.ordinal();
    }

    public static boolean isPrimary(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && state.getValue(PART) == InterfacePart.PRIMARY;
    }

    public static BlockPos primaryPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        BlockPos base = state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
        return state.getValue(PART) == InterfacePart.SECONDARY
                ? base.relative(facing.getCounterClockWise())
                : base;
    }

    private static VoxelShape shapeFor(BlockState state) {
        return CLIPPED_SHAPES[shapeIndex(state.getValue(FACING), state.getValue(PART), state.getValue(HALF))];
    }

    private static VoxelShape clip(VoxelShape fullAtPrimary, int partDx, int partDy, int partDz) {
        VoxelShape moved = fullAtPrimary.move(-partDx, -partDy, -partDz);
        return Shapes.join(moved, Shapes.block(), BooleanOp.AND);
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
        if (!this.fits(context, origin, facing)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(PART, InterfacePart.PRIMARY);
    }

    private boolean fits(BlockPlaceContext context, BlockPos origin, Direction facing) {
        Level level = context.getLevel();
        for (InterfacePart part : InterfacePart.values()) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                BlockPos pos = partPos(origin, facing, part, half);
                if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                    return false;
                }
                BlockState state = level.getBlockState(pos);
                if (!(pos.equals(origin) ? state.canBeReplaced(context) : state.canBeReplaced(context) || state.isAir())) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (!isPrimary(state)) {
            return;
        }
        Direction facing = state.getValue(FACING);
        for (InterfacePart part : InterfacePart.values()) {
            for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                if (part == InterfacePart.PRIMARY && half == DoubleBlockHalf.LOWER) {
                    continue;
                }
                level.setBlock(
                        partPos(pos, facing, part, half),
                        state.setValue(PART, part).setValue(HALF, half),
                        Block.UPDATE_ALL
                );
            }
        }
    }

    private static BlockPos partPos(BlockPos primary, Direction facing, InterfacePart part, DoubleBlockHalf half) {
        BlockPos base = part == InterfacePart.SECONDARY ? primary.relative(facing.getClockWise()) : primary;
        return half == DoubleBlockHalf.UPPER ? base.above() : base;
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide) {
            BlockPos primary = primaryPos(pos, state);
            if (!isPrimary(state) && !player.isCreative()) {
                BlockState primaryState = level.getBlockState(primary);
                if (primaryState.is(this) && isPrimary(primaryState)) {
                    dropResources(primaryState, level, primary, null, player, player.getMainHandItem());
                }
            }
            this.clearSiblings(level, pos, state);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!level.isClientSide && !state.is(newState.getBlock())) {
            this.clearSiblings(level, pos, state);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void clearSiblings(Level level, BlockPos pos, BlockState state) {
        if (level.isClientSide || CLEARING.get()) {
            return;
        }
        CLEARING.set(Boolean.TRUE);
        try {
            BlockPos primary = primaryPos(pos, state);
            Direction facing = state.getValue(FACING);
            for (InterfacePart part : InterfacePart.values()) {
                for (DoubleBlockHalf half : DoubleBlockHalf.values()) {
                    BlockPos partPos = partPos(primary, facing, part, half);
                    if (partPos.equals(pos)) {
                        continue;
                    }
                    BlockState partState = level.getBlockState(partPos);
                    if (partState.is(this)
                            && partState.getValue(FACING) == facing
                            && partState.getValue(PART) == part
                            && partState.getValue(HALF) == half) {
                        level.setBlock(partPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                    }
                }
            }
        } finally {
            CLEARING.set(Boolean.FALSE);
        }
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state);
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
        if (mirror == Mirror.NONE) {
            return state;
        }
        BlockState rotated = state.rotate(mirror.getRotation(state.getValue(FACING)));
        InterfacePart flipped = rotated.getValue(PART) == InterfacePart.PRIMARY
                ? InterfacePart.SECONDARY
                : InterfacePart.PRIMARY;
        return rotated.setValue(PART, flipped);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, HALF, PART);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        this.open(level, primaryPos(pos, state), player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        this.open(level, primaryPos(pos, state), player);
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> InterfaceMenu.create(containerId, inventory, pos),
                            Component.translatable("block.hyperscale.interface")
                    ),
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
    }
}
