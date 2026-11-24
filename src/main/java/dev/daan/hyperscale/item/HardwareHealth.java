package dev.daan.hyperscale.item;

import java.util.List;

import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;

/**
 * Tracks hardware wear/MTBF health (0..10000 = 0.00%..100.00%) on CPUs, RAM, GPUs, and Storage Drives.
 */
public final class HardwareHealth {
    public static final int MAX_HEALTH = 10_000;
    public static final int DEGRADED_THRESHOLD = 2_000; // 20%

    private HardwareHealth() {
    }

    public static int get(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }
        Integer stored = stack.get(ModDataComponents.HARDWARE_HEALTH.get());
        return stored == null ? MAX_HEALTH : Math.max(0, Math.min(MAX_HEALTH, stored));
    }

    public static int percent(ItemStack stack) {
        return (int) Math.round(get(stack) * 100.0D / MAX_HEALTH);
    }

    public static boolean isFailed(ItemStack stack) {
        return get(stack) <= 0;
    }

    public static boolean isDegraded(ItemStack stack) {
        int h = get(stack);
        return h > 0 && h <= DEGRADED_THRESHOLD;
    }

    public static void set(ItemStack stack, int health) {
        int clamped = Math.max(0, Math.min(MAX_HEALTH, health));
        if (clamped >= MAX_HEALTH) {
            stack.remove(ModDataComponents.HARDWARE_HEALTH.get());
        } else {
            stack.set(ModDataComponents.HARDWARE_HEALTH.get(), clamped);
        }
    }

    public static boolean damage(ItemStack stack, int points) {
        if (points <= 0 || stack.isEmpty()) {
            return false;
        }
        int before = get(stack);
        if (before <= 0) {
            return false;
        }
        int after = Math.max(0, before - points);
        set(stack, after);
        return after != before;
    }

    public static void repair(ItemStack stack) {
        set(stack, MAX_HEALTH);
    }

    public static boolean isBarVisible(ItemStack stack) {
        return get(stack) < MAX_HEALTH;
    }

    public static int barWidth(ItemStack stack) {
        return Math.round(13.0F * get(stack) / MAX_HEALTH);
    }

    public static int barColor(ItemStack stack) {
        float ratio = (float) get(stack) / MAX_HEALTH;
        return Mth.hsvToRgb(Math.max(0.0F, ratio / 3.0F), 1.0F, 1.0F);
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip, boolean isDrive) {
        int pct = percent(stack);
        if (pct <= 0) {
            tooltip.add(Component.translatable(isDrive ? "tooltip.hyperscale.health.failed_drive" : "tooltip.hyperscale.health.failed")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        } else if (pct <= 20) {
            tooltip.add(Component.translatable("tooltip.hyperscale.health.degraded", pct)
                    .withStyle(ChatFormatting.GOLD));
        } else if (pct < 100) {
            tooltip.add(Component.translatable("tooltip.hyperscale.health.ok", pct)
                    .withStyle(ChatFormatting.GREEN));
        }
    }
}
