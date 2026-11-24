package dev.daan.hyperscale.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class CpuItem extends Item {
    private final CpuSpec spec;

    public CpuItem(Properties properties, CpuSpec spec) {
        super(properties);
        this.spec = spec;
    }

    public CpuSpec spec() {
        return this.spec;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return HardwareHealth.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return HardwareHealth.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return HardwareHealth.barColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        HardwareHealth.appendTooltip(stack, tooltip, false);
        tooltip.add(Component.translatable("tooltip.hyperscale.cpu.cores", this.spec.cores(), String.format(Locale.ROOT, "%.2f", this.spec.clockSpeed()))
                .withStyle(ChatFormatting.AQUA));
        if (this.spec.cacheRamGb() > 0) {
            tooltip.add(Component.translatable("tooltip.hyperscale.cpu.cache", this.spec.cacheRamGb())
                    .withStyle(ChatFormatting.GREEN));
        }
        if (this.spec.energyEfficiency() < 0.999D) {
            int savedPct = (int) Math.round((1.0D - this.spec.energyEfficiency()) * 100.0D);
            tooltip.add(Component.translatable("tooltip.hyperscale.cpu.efficiency", savedPct)
                    .withStyle(ChatFormatting.YELLOW));
        }
        tooltip.add(Component.translatable("tooltip.hyperscale.cpu.role")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
