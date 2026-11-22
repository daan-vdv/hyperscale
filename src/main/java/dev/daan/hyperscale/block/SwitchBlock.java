package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class SwitchBlock extends Block implements EntityBlock {
    public SwitchBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SwitchBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) {
            return null;
        }
        return type == ModBlockEntities.SWITCH.get() ? (level1, pos, state1, blockEntity) -> {
            if (blockEntity instanceof SwitchBlockEntity switchEntity) {
                SwitchBlockEntity.serverTick(switchEntity);
            }
        } : null;
    }
}
