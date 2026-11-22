package dev.daan.hyperscale.item;

import java.util.List;

import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.hall.StackCount;
import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class ImageItem extends Item {
    public ImageItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ProcessImage image = stack.get(ModDataComponents.PROCESS_IMAGE.get());
        if (image == null) {
            tooltip.add(Component.translatable("tooltip.hyperscale.image_blank"));
            return;
        }
        tooltip.add(Component.translatable("tooltip.hyperscale.image_time", String.format("%.1f", image.cycleTicks() / 20.0D)));
        tooltip.add(Component.translatable("tooltip.hyperscale.image_inputs"));
        this.addStacks(tooltip, image.inputs());
        tooltip.add(Component.translatable("tooltip.hyperscale.image_outputs"));
        this.addStacks(tooltip, image.outputs());
    }

    private void addStacks(List<Component> tooltip, List<StackCount> stacks) {
        int shown = 0;
        for (StackCount stack : stacks) {
            if (shown == 6) {
                tooltip.add(Component.translatable("tooltip.hyperscale.image_more", stacks.size() - shown));
                return;
            }
            Item item = BuiltInRegistries.ITEM.get(stack.item());
            tooltip.add(Component.translatable("tooltip.hyperscale.image_stack", stack.count(), Component.translatable(item.getDescriptionId())));
            shown++;
        }
    }
}
