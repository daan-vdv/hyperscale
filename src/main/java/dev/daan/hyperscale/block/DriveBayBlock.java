package dev.daan.hyperscale.block;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.menu.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class DriveBayBlock extends InventoryMachineBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Drive-bay v1 model opens west; ~1 block with a 1px west overhang. */
    private static final VoxelShape SHAPE_WEST = Shapes.box(-1.0D / 16.0D, 0.0D, 0.0D, 15.0D / 16.0D, 1.0D, 1.0D);
    private static final Map<Direction, VoxelShape> SHAPES = new ConcurrentHashMap<>();

    public DriveBayBlock(Properties properties) {
        super(properties, StorageKind.DRIVE);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    private static VoxelShape shapeFor(Direction facing) {
        return SHAPES.computeIfAbsent(facing, direction -> rotate(SHAPE_WEST, direction));
    }

    private static VoxelShape rotate(VoxelShape shape, Direction facing) {
        VoxelShape result = shape;
        int times = (facing.get2DDataValue() - Direction.WEST.get2DDataValue() + 4) % 4;
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
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DriveBayBlockEntity(pos, state);
    }
}
