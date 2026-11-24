package dev.daan.hyperscale.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class GpuItem extends Item {
    private final GpuSpec spec;

    public GpuItem(Properties properties, GpuSpec spec) {
        super(properties);
        this.spec = spec;
    }

    public GpuSpec spec() {
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
        tooltip.add(Component.translatable("tooltip.hyperscale.gpu.compute", this.spec.tflops(), String.format(Locale.ROOT, "%.0f", this.spec.speedBonus() * 100.0D))
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.add(Component.translatable("tooltip.hyperscale.gpu.overlap", this.spec.vramGb(), this.spec.computeThreads())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.hyperscale.gpu.power", this.spec.activeFePerTick())
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.hyperscale.gpu.role")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
