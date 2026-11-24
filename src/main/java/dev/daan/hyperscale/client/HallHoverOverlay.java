package dev.daan.hyperscale.client;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.hall.HallStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = Hyperscale.MOD_ID, value = Dist.CLIENT)
public final class HallHoverOverlay {
    private static final long CACHE_TICKS = 5L;

    private static BlockPos cachedPos;
    private static long cachedTick = Long.MIN_VALUE;
    private static Component cachedLine;

    private HallHoverOverlay() {
    }

    @SubscribeEvent
    public static void onRenderGui(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || minecraft.screen != null || minecraft.options.hideGui) {
            return;
        }
        HitResult hit = minecraft.hitResult;
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            cachedPos = null;
            cachedLine = null;
            return;
        }
        BlockPos pos = blockHit.getBlockPos();
        long now = minecraft.level.getGameTime();
        if (!pos.equals(cachedPos) || now - cachedTick >= CACHE_TICKS || now < cachedTick) {
            cachedPos = pos.immutable();
            cachedTick = now;
            cachedLine = HallStatus.hover(minecraft.level, pos);
        }
        Component line = cachedLine;
        if (line == null) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int width = minecraft.getWindow().getGuiScaledWidth();
        int height = minecraft.getWindow().getGuiScaledHeight();
        int textWidth = minecraft.font.width(line);
        int x = (width - textWidth) / 2;
        int y = height / 2 + 16;
        graphics.fill(x - 3, y - 2, x + textWidth + 3, y + 10, 0x88000000);
        graphics.drawString(minecraft.font, line, x, y, 0xA0E0FF, false);
    }
}
