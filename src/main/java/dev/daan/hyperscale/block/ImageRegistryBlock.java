package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.ImageRegistryBlockEntity;
import dev.daan.hyperscale.menu.StorageKind;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ImageRegistryBlock extends InventoryMachineBlock {
    public ImageRegistryBlock(Properties properties) {
        super(properties, StorageKind.REGISTRY);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ImageRegistryBlockEntity(pos, state);
    }
}
