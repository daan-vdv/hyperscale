package dev.daan.hyperscale.item;

import java.util.List;

import dev.daan.hyperscale.hall.StackCount;
import dev.daan.hyperscale.registry.ModDataComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public class StorageDriveItem extends Item {
    private final StorageDriveSpec spec;

    public StorageDriveItem(Properties properties, StorageDriveSpec spec) {
        super(properties.stacksTo(1));
        this.spec = spec;
    }

    public StorageDriveSpec spec() {
        return this.spec;
    }

    public static DriveContents getContents(ItemStack stack) {
        DriveContents contents = stack.get(ModDataComponents.DRIVE_CONTENTS.get());
        return contents == null ? DriveContents.EMPTY : contents;
    }

    public static void setContents(ItemStack stack, DriveContents contents) {
        if (contents == null || contents.items().isEmpty()) {
            stack.remove(ModDataComponents.DRIVE_CONTENTS.get());
        } else {
            stack.set(ModDataComponents.DRIVE_CONTENTS.get(), contents);
        }
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
        HardwareHealth.appendTooltip(stack, tooltip, true);
        DriveContents contents = getContents(stack);
        tooltip.add(Component.translatable(
                "tooltip.hyperscale.drive.usage",
                contents.totalItems(),
                this.spec.maxItems(),
                contents.typeCount(),
                this.spec.maxTypes()
        ).withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable(
                "tooltip.hyperscale.drive.speed",
                this.spec.driveType(),
                this.spec.ioPerTick()
        ).withStyle(ChatFormatting.GRAY));

        if (!contents.items().isEmpty()) {
            int shown = 0;
            for (StackCount entry : contents.items()) {
                if (shown >= 4) {
                    tooltip.add(Component.translatable("tooltip.hyperscale.image_more", contents.items().size() - shown)
                            .withStyle(ChatFormatting.DARK_GRAY));
                    break;
                }
                Item item = BuiltInRegistries.ITEM.get(entry.item());
                tooltip.add(Component.literal("  " + entry.count() + "x ")
                        .append(Component.translatable(item.getDescriptionId()))
                        .withStyle(ChatFormatting.GRAY));
                shown++;
            }
        } else {
            tooltip.add(Component.translatable("tooltip.hyperscale.drive.role")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
