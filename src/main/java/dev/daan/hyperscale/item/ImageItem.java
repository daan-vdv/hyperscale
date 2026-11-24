package dev.daan.hyperscale.item;

import java.util.List;
import java.util.Locale;

import dev.daan.hyperscale.hall.FluidStackCount;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.hall.StackCount;
import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

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
        tooltip.add(Component.translatable("tooltip.hyperscale.image_time", String.format(Locale.ROOT, "%.1f", image.cycleTicks() / 20.0D)));
        if (image.energyPerCycle() > 0) {
            tooltip.add(Component.translatable("tooltip.hyperscale.image_energy", image.energyPerCycle()));
        }
        tooltip.add(Component.translatable("tooltip.hyperscale.image_inputs"));
        this.addStacks(tooltip, image.inputs());
        this.addFluids(tooltip, image.fluidInputs());
        tooltip.add(Component.translatable("tooltip.hyperscale.image_outputs"));
        this.addStacks(tooltip, image.outputs());
        this.addFluids(tooltip, image.fluidOutputs());
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

    private void addFluids(List<Component> tooltip, List<FluidStackCount> fluids) {
        int shown = 0;
        for (FluidStackCount entry : fluids) {
            if (shown == 6) {
                tooltip.add(Component.translatable("tooltip.hyperscale.image_more", fluids.size() - shown));
                return;
            }
            Fluid fluid = BuiltInRegistries.FLUID.get(entry.fluid());
            Component name = new FluidStack(fluid, entry.amount()).getHoverName();
            tooltip.add(Component.translatable("tooltip.hyperscale.image_fluid", entry.amount(), name));
            shown++;
        }
    }
}
