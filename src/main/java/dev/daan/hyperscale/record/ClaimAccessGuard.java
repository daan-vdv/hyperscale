package dev.daan.hyperscale.record;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

/**
 * Protects active sealed compiler rooms from player tampering (right-click, block breaking,
 * block placement) while sampling, and cleans up static registrations on world/server unload.
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

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (RecorderBlockEntity.blocksPlayerAccess(level, event.getPos())) {
            event.setCanceled(true);
            Player player = event.getPlayer();
            if (player != null && !level.isClientSide) {
                player.displayClientMessage(Component.translatable("gui.hyperscale.recorder.status.claim_locked"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof Level level)) {
            return;
        }
        if (RecorderBlockEntity.blocksPlayerAccess(level, event.getPos())) {
            event.setCanceled(true);
            if (event.getEntity() instanceof Player player && !level.isClientSide) {
                player.displayClientMessage(Component.translatable("gui.hyperscale.recorder.status.claim_locked"), true);
            }
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel() instanceof Level level) {
            RecorderBlockEntity.clearSamplingFor(level);
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        RecorderBlockEntity.clearAllSampling();
    }
}
