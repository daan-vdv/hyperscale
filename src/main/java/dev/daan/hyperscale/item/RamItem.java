package dev.daan.hyperscale.item;

import java.util.List;
import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class RamItem extends Item {
    private final RamSpec spec;

    public RamItem(Properties properties, RamSpec spec) {
        super(properties);
        this.spec = spec;
    }

    public RamSpec spec() {
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
        tooltip.add(Component.translatable("tooltip.hyperscale.ram.capacity", this.spec.capacityGb(), this.spec.generation())
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("tooltip.hyperscale.ram.bandwidth", String.format(Locale.ROOT, "%.2f", this.spec.speedMultiplier()))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hyperscale.ram.role")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
