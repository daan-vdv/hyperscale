package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * 2x2x2 multiblock Backup Generator. Primary cell (ox=0, oy=0, oz=0) renders the full 32x32x32 model;
 * the remaining 7 cells provide solid collision/selection and break cleanly together.
 */
public class BackupGeneratorBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 1);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 1);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 1);

    private static final ThreadLocal<Boolean> CLEARING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public BackupGeneratorBlock(Properties properties) {
        super(properties.noOcclusion().pushReaction(PushReaction.BLOCK));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(OX, 0)
                .setValue(OY, 0)
                .setValue(OZ, 0));
    }

    public static boolean isPrimary(BlockState state) {
        return state.getValue(OX) == 0 && state.getValue(OY) == 0 && state.getValue(OZ) == 0;
    }

    public static BlockPos primaryPos(BlockPos pos, BlockState state) {
        Direction facing = state.getValue(FACING);
        return pos
                .relative(facing.getCounterClockWise(), state.getValue(OX))
                .relative(facing, state.getValue(OZ))
                .below(state.getValue(OY));
    }

    private static BlockPos cellPos(BlockPos primary, Direction facing, int ox, int oy, int oz) {
        return primary
                .relative(facing.getClockWise(), ox)
                .relative(facing.getOpposite(), oz)
                .above(oy);
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
                .setValue(OX, 0)
                .setValue(OY, 0)
                .setValue(OZ, 0);
    }

    private boolean fits(BlockPlaceContext context, BlockPos origin, Direction facing) {
        Level level = context.getLevel();
        for (int ox = 0; ox <= 1; ox++) {
            for (int oy = 0; oy <= 1; oy++) {
                for (int oz = 0; oz <= 1; oz++) {
                    BlockPos pos = cellPos(origin, facing, ox, oy, oz);
                    if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)) {
                        return false;
                    }
                    BlockState state = level.getBlockState(pos);
                    if (!(pos.equals(origin) ? state.canBeReplaced(context) : state.canBeReplaced(context) || state.isAir())) {
                        return false;
                    }
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
        for (int ox = 0; ox <= 1; ox++) {
            for (int oy = 0; oy <= 1; oy++) {
                for (int oz = 0; oz <= 1; oz++) {
                    if (ox == 0 && oy == 0 && oz == 0) {
                        continue;
                    }
                    level.setBlock(
                            cellPos(pos, facing, ox, oy, oz),
                            state.setValue(OX, ox).setValue(OY, oy).setValue(OZ, oz),
                            Block.UPDATE_ALL
                    );
                }
            }
        }
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
            for (int ox = 0; ox <= 1; ox++) {
                for (int oy = 0; oy <= 1; oy++) {
                    for (int oz = 0; oz <= 1; oz++) {
                        BlockPos cell = cellPos(primary, facing, ox, oy, oz);
                        if (cell.equals(pos)) {
                            continue;
                        }
                        BlockState cellState = level.getBlockState(cell);
                        if (cellState.is(this)
                                && cellState.getValue(FACING) == facing
                                && cellState.getValue(OX) == ox
                                && cellState.getValue(OY) == oy
                                && cellState.getValue(OZ) == oz) {
                            level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                        }
                    }
                }
            }
        } finally {
            CLEARING.set(Boolean.FALSE);
        }
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
        return state.rotate(mirror.getRotation(state.getValue(FACING)))
                .setValue(OX, 1 - state.getValue(OX));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, OX, OY, OZ);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return isPrimary(state) ? new BackupGeneratorBlockEntity(pos, state) : null;
    }
}
