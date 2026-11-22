package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class PortBlockEntity extends InventoryBlockEntity {
    public PortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PORT.get(), pos, state, 1);
    }
}
