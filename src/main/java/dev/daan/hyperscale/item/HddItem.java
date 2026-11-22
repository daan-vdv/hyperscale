package dev.daan.hyperscale.item;

import java.util.List;

import dev.daan.hyperscale.hall.FurnaceImage;
import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class HddItem extends Item {
    public HddItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FurnaceImage image = stack.get(ModDataComponents.FURNACE_IMAGE.get());
        if (image == null) {
            return;
        }
        Item output = BuiltInRegistries.ITEM.get(image.output());
        tooltip.add(Component.translatable("tooltip.hyperscale.image", Component.translatable(output.getDescriptionId())));
    }
}
