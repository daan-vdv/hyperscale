package dev.daan.hyperscale.block;

import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity sw && sw.imageCardCount() > 0) {
            this.open(level, pos, player);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof SwitchBlockEntity sw && sw.imageCardCount() > 0) {
            this.open(level, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof SwitchBlockEntity switchEntity) {
            switchEntity.dropImageCards();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof SwitchBlockEntity sw)) {
            return;
        }
        double x = pos.getX() + 0.5D;
        double y = pos.getY() + 1.02D;
        double z = pos.getZ() + 0.5D;
        if (sw.thermalShutdown() || sw.hallTempC() >= 85.0D) {
            level.addParticle(ParticleTypes.SMOKE, x + (random.nextDouble() - 0.5D) * 0.4D, y, z + (random.nextDouble() - 0.5D) * 0.4D, 0.0D, 0.03D, 0.0D);
        } else if (sw.liquidCooled() && sw.hallTempC() <= 50.0D && sw.lastTotalFePerTick() > 0 && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.GLOW, x + (random.nextDouble() - 0.5D) * 0.5D, y, z + (random.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.02D, 0.0D);
        }
        if (sw.lastTotalFePerTick() > 0 && sw.noiseDb() >= 40 && random.nextInt(8) == 0) {
            level.playLocalSound(x, y, z, SoundEvents.BEACON_AMBIENT, SoundSource.BLOCKS, 0.08F, 0.65F, false);
        }
    }

    private void open(Level level, BlockPos pos, Player player) {
        if (player instanceof ServerPlayer serverPlayer && level.getBlockEntity(pos) instanceof SwitchBlockEntity switchEntity) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> new StorageMenu(containerId, inventory, pos, switchEntity.imageCards(), StorageKind.SWITCH),
                            Component.translatable(StorageKind.SWITCH.titleKey())
                    ),
                    buffer -> buffer.writeBlockPos(pos)
            );
        }
    }
}
