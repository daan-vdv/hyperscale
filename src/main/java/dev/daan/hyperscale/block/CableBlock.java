package dev.daan.hyperscale.block;

import java.util.EnumMap;
import java.util.Map;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

public class CableBlock extends Block implements SimpleWaterloggedBlock {
    public static final EnumProperty<CableConnection> NORTH = EnumProperty.create("north", CableConnection.class);
    public static final EnumProperty<CableConnection> EAST = EnumProperty.create("east", CableConnection.class);
    public static final EnumProperty<CableConnection> SOUTH = EnumProperty.create("south", CableConnection.class);
    public static final EnumProperty<CableConnection> WEST = EnumProperty.create("west", CableConnection.class);
    public static final EnumProperty<CableConnection> UP = EnumProperty.create("up", CableConnection.class);
    public static final EnumProperty<CableConnection> DOWN = EnumProperty.create("down", CableConnection.class);
    public static final EnumProperty<CableStraight> STRAIGHT = EnumProperty.create("straight", CableStraight.class);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    public static final Map<Direction, EnumProperty<CableConnection>> PROPERTY_BY_DIRECTION = createPropertyMap();

    private static final VoxelShape CORE_SHAPE = Block.box(5.0, 5.0, 5.0, 11.0, 11.0, 11.0);
    private static final VoxelShape STRAIGHT_X_SHAPE = Block.box(0.0, 5.5, 5.5, 16.0, 10.5, 10.5);
    private static final VoxelShape STRAIGHT_Y_SHAPE = Block.box(5.5, 0.0, 5.5, 10.5, 16.0, 10.5);
    private static final VoxelShape STRAIGHT_Z_SHAPE = Block.box(5.5, 5.5, 0.0, 10.5, 10.5, 16.0);
    private static final Map<Direction, VoxelShape> ARM_SHAPES = createArmShapes();
    private static final Map<Direction, VoxelShape> CONNECTOR_SHAPES = createConnectorShapes();
    private static final VoxelShape[] SHAPE_CACHE = new VoxelShape[729];

    public CableBlock(Properties properties) {
        super(properties.noOcclusion());
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(NORTH, CableConnection.NONE)
                .setValue(EAST, CableConnection.NONE)
                .setValue(SOUTH, CableConnection.NONE)
                .setValue(WEST, CableConnection.NONE)
                .setValue(UP, CableConnection.NONE)
                .setValue(DOWN, CableConnection.NONE)
                .setValue(STRAIGHT, CableStraight.NONE)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN, STRAIGHT, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        FluidState fluidState = level.getFluidState(pos);
        BlockState state = this.defaultBlockState().setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
        return updateConnections(level, pos, state);
    }

    @Override
    protected BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }
        CableConnection connection = getConnection(level, neighborPos, direction, neighborState);
        BlockState updated = state.setValue(PROPERTY_BY_DIRECTION.get(direction), connection);
        return updated.setValue(STRAIGHT, computeStraight(updated));
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide) {
            BlockState updated = updateConnections(level, pos, state);
            if (updated != state) {
                level.setBlock(pos, updated, Block.UPDATE_ALL);
            }
        }
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int index = shapeIndex(state);
        VoxelShape cached = SHAPE_CACHE[index];
        if (cached != null) {
            return cached;
        }
        CableStraight straight = computeStraight(state);
        VoxelShape shape;
        if (straight != CableStraight.NONE) {
            shape = switch (straight) {
                case X -> STRAIGHT_X_SHAPE;
                case Y -> STRAIGHT_Y_SHAPE;
                case Z -> STRAIGHT_Z_SHAPE;
                case NONE -> CORE_SHAPE;
            };
            for (Direction direction : Direction.values()) {
                if (state.getValue(PROPERTY_BY_DIRECTION.get(direction)) == CableConnection.MACHINE) {
                    shape = Shapes.or(shape, CONNECTOR_SHAPES.get(direction));
                }
            }
        } else {
            shape = CORE_SHAPE;
            for (Direction direction : Direction.values()) {
                CableConnection connection = state.getValue(PROPERTY_BY_DIRECTION.get(direction));
                if (connection == CableConnection.CABLE) {
                    shape = Shapes.or(shape, ARM_SHAPES.get(direction));
                } else if (connection == CableConnection.MACHINE) {
                    shape = Shapes.or(shape, ARM_SHAPES.get(direction), CONNECTOR_SHAPES.get(direction));
                }
            }
        }
        SHAPE_CACHE[index] = shape;
        return shape;
    }

    @Override
    protected VoxelShape getOcclusionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return !state.getValue(WATERLOGGED);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = switch (rotation) {
            case CLOCKWISE_180 -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(SOUTH, state.getValue(NORTH))
                    .setValue(WEST, state.getValue(EAST));
            case COUNTERCLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(EAST))
                    .setValue(EAST, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(WEST))
                    .setValue(WEST, state.getValue(NORTH));
            case CLOCKWISE_90 -> state
                    .setValue(NORTH, state.getValue(WEST))
                    .setValue(EAST, state.getValue(NORTH))
                    .setValue(SOUTH, state.getValue(EAST))
                    .setValue(WEST, state.getValue(SOUTH));
            default -> state;
        };
        return rotated.setValue(STRAIGHT, computeStraight(rotated));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = switch (mirror) {
            case LEFT_RIGHT -> state
                    .setValue(NORTH, state.getValue(SOUTH))
                    .setValue(SOUTH, state.getValue(NORTH));
            case FRONT_BACK -> state
                    .setValue(EAST, state.getValue(WEST))
                    .setValue(WEST, state.getValue(EAST));
            default -> super.mirror(state, mirror);
        };
        return mirrored.setValue(STRAIGHT, computeStraight(mirrored));
    }

    private static CableStraight computeStraight(BlockState state) {
        boolean n = state.getValue(NORTH) != CableConnection.NONE;
        boolean s = state.getValue(SOUTH) != CableConnection.NONE;
        boolean e = state.getValue(EAST) != CableConnection.NONE;
        boolean w = state.getValue(WEST) != CableConnection.NONE;
        boolean u = state.getValue(UP) != CableConnection.NONE;
        boolean d = state.getValue(DOWN) != CableConnection.NONE;

        if (n && s && !e && !w && !u && !d) {
            return CableStraight.Z;
        }
        if (e && w && !n && !s && !u && !d) {
            return CableStraight.X;
        }
        if (u && d && !n && !s && !e && !w) {
            return CableStraight.Y;
        }
        return CableStraight.NONE;
    }

    private static BlockState updateConnections(LevelAccessor level, BlockPos pos, BlockState state) {
        BlockState result = state;
        for (Direction direction : Direction.values()) {
            BlockPos neighborPos = pos.relative(direction);
            BlockState neighborState = level.getBlockState(neighborPos);
            CableConnection connection = getConnection(level, neighborPos, direction, neighborState);
            result = result.setValue(PROPERTY_BY_DIRECTION.get(direction), connection);
        }
        return result.setValue(STRAIGHT, computeStraight(result));
    }

    private static CableConnection getConnection(
            LevelAccessor level,
            BlockPos neighborPos,
            Direction direction,
            BlockState neighborState
    ) {
        if (neighborState.isAir()) {
            return CableConnection.NONE;
        }
        if (neighborState.getBlock() instanceof CableBlock) {
            return CableConnection.CABLE;
        }
        if (isConnectableMachine(level, neighborPos, direction, neighborState)) {
            return CableConnection.MACHINE;
        }
        return CableConnection.NONE;
    }

    private static boolean isConnectableMachine(
            LevelAccessor level,
            BlockPos neighborPos,
            Direction directionFromCable,
            BlockState neighborState
    ) {
        if (neighborState.is(ModBlocks.RECORDER_CASING.get())) {
            return false;
        }
        if (Hyperscale.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(neighborState.getBlock()).getNamespace())) {
            return true;
        }
        if (level instanceof Level realLevel) {
            Direction sideOnNeighbor = directionFromCable.getOpposite();
            if (realLevel.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, sideOnNeighbor) != null) {
                return true;
            }
            if (realLevel.getCapability(Capabilities.ItemHandler.BLOCK, neighborPos, sideOnNeighbor) != null) {
                return true;
            }
            if (realLevel.getCapability(Capabilities.FluidHandler.BLOCK, neighborPos, sideOnNeighbor) != null) {
                return true;
            }
        }
        return false;
    }

    private static int shapeIndex(BlockState state) {
        int index = state.getValue(NORTH).ordinal();
        index = index * 3 + state.getValue(EAST).ordinal();
        index = index * 3 + state.getValue(SOUTH).ordinal();
        index = index * 3 + state.getValue(WEST).ordinal();
        index = index * 3 + state.getValue(UP).ordinal();
        index = index * 3 + state.getValue(DOWN).ordinal();
        return index;
    }

    private static Map<Direction, EnumProperty<CableConnection>> createPropertyMap() {
        Map<Direction, EnumProperty<CableConnection>> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, NORTH);
        map.put(Direction.EAST, EAST);
        map.put(Direction.SOUTH, SOUTH);
        map.put(Direction.WEST, WEST);
        map.put(Direction.UP, UP);
        map.put(Direction.DOWN, DOWN);
        return map;
    }

    private static Map<Direction, VoxelShape> createArmShapes() {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, Block.box(5.25, 5.25, 0.0, 10.75, 10.75, 5.0));
        map.put(Direction.SOUTH, Block.box(5.25, 5.25, 11.0, 10.75, 10.75, 16.0));
        map.put(Direction.WEST, Block.box(0.0, 5.25, 5.25, 5.0, 10.75, 10.75));
        map.put(Direction.EAST, Block.box(11.0, 5.25, 5.25, 16.0, 10.75, 10.75));
        map.put(Direction.DOWN, Block.box(5.25, 0.0, 5.25, 10.75, 5.0, 10.75));
        map.put(Direction.UP, Block.box(5.25, 11.0, 5.25, 10.75, 16.0, 10.75));
        return map;
    }

    private static Map<Direction, VoxelShape> createConnectorShapes() {
        Map<Direction, VoxelShape> map = new EnumMap<>(Direction.class);
        map.put(Direction.NORTH, Block.box(4.5, 4.5, 0.0, 11.5, 11.5, 2.5));
        map.put(Direction.SOUTH, Block.box(4.5, 4.5, 13.5, 11.5, 11.5, 16.0));
        map.put(Direction.WEST, Block.box(0.0, 4.5, 4.5, 2.5, 11.5, 11.5));
        map.put(Direction.EAST, Block.box(13.5, 4.5, 4.5, 16.0, 11.5, 11.5));
        map.put(Direction.DOWN, Block.box(4.5, 0.0, 4.5, 11.5, 2.5, 11.5));
        map.put(Direction.UP, Block.box(4.5, 13.5, 4.5, 11.5, 16.0, 11.5));
        return map;
    }
}
