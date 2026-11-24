package dev.daan.hyperscale.item;

import java.util.List;

import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.blockentity.ProbeBlockEntity;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BinderItem extends Item {
    public BinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return apply(context.getLevel(), context.getClickedPos(), context.getPlayer(), context.getItemInHand())
                ? InteractionResult.sidedSuccess(context.getLevel().isClientSide)
                : InteractionResult.PASS;
    }

    public static boolean apply(Level level, BlockPos pos, Player player, ItemStack stack) {
        if (player == null || stack.isEmpty() || !(stack.getItem() instanceof BinderItem)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.is(ModBlocks.RECORDER.get())) {
            if (!level.isClientSide) {
                if (player.isShiftKeyDown()) {
                    stack.remove(ModDataComponents.BINDER_TARGET.get());
                    player.displayClientMessage(Component.translatable("message.hyperscale.binder.cleared"), true);
                } else {
                    stack.set(ModDataComponents.BINDER_TARGET.get(), GlobalPos.of(level.dimension(), pos.immutable()));
                    player.displayClientMessage(Component.translatable("message.hyperscale.binder.selected"), true);
                }
            }
            return true;
        }
        if (state.getBlock() instanceof ProbeBlock) {
            if (!level.isClientSide && level.getBlockEntity(pos) instanceof ProbeBlockEntity probe) {
                if (player.isShiftKeyDown()) {
                    unbind(level, probe, player);
                } else {
                    bind(level, stack, probe, player);
                }
            }
            return true;
        }
        return false;
    }

    private static void bind(Level level, ItemStack stack, ProbeBlockEntity probe, Player player) {
        GlobalPos target = stack.get(ModDataComponents.BINDER_TARGET.get());
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.need_compiler"), true);
            return;
        }
        if (!target.dimension().equals(level.dimension())) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.wrong_dim"), true);
            return;
        }
        BlockPos compilerPos = target.pos();
        if (compilerPos.distManhattan(probe.getBlockPos()) > RecordLimits.MAX_BIND_DISTANCE) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.too_far"), true);
            return;
        }
        if (!level.isLoaded(compilerPos) || !(level.getBlockEntity(compilerPos) instanceof RecorderBlockEntity recorder)) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.missing_compiler"), true);
            return;
        }
        if (recorder.isLearning()) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.busy"), true);
            return;
        }
        if (recorder.bindProbe(probe.getBlockPos())) {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.bound"), true);
        } else {
            player.displayClientMessage(Component.translatable("message.hyperscale.binder.failed"), true);
        }
    }

    private static void unbind(Level level, ProbeBlockEntity probe, Player player) {
        BlockPos compilerPos = probe.compiler();
        if (compilerPos == null) {
            return;
        }
        if (level.isLoaded(compilerPos) && level.getBlockEntity(compilerPos) instanceof RecorderBlockEntity recorder) {
            if (recorder.isLearning()) {
                player.displayClientMessage(Component.translatable("message.hyperscale.binder.busy"), true);
                return;
            }
            recorder.unbindProbe(probe.getBlockPos());
        } else {
            probe.clearCompiler();
        }
        player.displayClientMessage(Component.translatable("message.hyperscale.binder.unbound"), true);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hyperscale.binder"));
        GlobalPos target = stack.get(ModDataComponents.BINDER_TARGET.get());
        if (target != null) {
            BlockPos pos = target.pos();
            tooltip.add(Component.translatable("tooltip.hyperscale.binder.target", pos.getX(), pos.getY(), pos.getZ()));
        }
    }
}
