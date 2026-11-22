package dev.daan.hyperscale.record;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * While a compiler is sampling, players cannot open inventories inside its claim
 * (or the bound probes). Stops stuffing a chest the output probe would then pull.
 */
@EventBusSubscriber(modid = Hyperscale.MOD_ID)
public final class ClaimAccessGuard {
    private ClaimAccessGuard() {
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (!RecorderBlockEntity.blocksPlayerAccess(level, event.getPos())) {
            return;
        }
        event.setCanceled(true);
        Player player = event.getEntity();
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable("gui.hyperscale.recorder.status.claim_locked"), true);
        }
    }
}
