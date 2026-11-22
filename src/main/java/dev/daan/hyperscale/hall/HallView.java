package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public record HallView(List<ImageRow> images, int selected, List<Component> detail, int progress, int cookingTime) {
    public record ImageRow(int index, Component label, boolean selected) {
    }

    public static HallView inspect(Level level, BlockPos origin) {
        return inspect(level, origin, -1, -1, -1);
    }

    public static HallView inspect(Level level, BlockPos origin, int selectedOverride, int progressOverride, int cycleOverride) {
        HallParts parts = HallFinder.find(level, origin);
        if (!parts.hasSwitch()) {
            return new HallView(List.of(), 0, List.of(Component.translatable("gui.hyperscale.interface.no_switch")), 0, 0);
        }

        BlockPos primary = Collections.min(parts.switches);
        SwitchBlockEntity switchEntity = level.getBlockEntity(primary) instanceof SwitchBlockEntity found ? found : null;
        List<HallImage> hallImages = HallRunner.listImages(level, parts);
        int selected;
        if (selectedOverride >= 0) {
            selected = hallImages.isEmpty() ? 0 : Math.min(selectedOverride, hallImages.size() - 1);
        } else if (switchEntity == null || hallImages.isEmpty()) {
            selected = 0;
        } else {
            selected = Math.min(switchEntity.selectedImage(), hallImages.size() - 1);
        }

        List<ImageRow> rows = new ArrayList<>();
        for (int index = 0; index < hallImages.size(); index++) {
            rows.add(new ImageRow(index, imageLabel(index, hallImages.get(index).image()), index == selected));
        }

        List<Component> detail = new ArrayList<>();
        ProcessImage image = hallImages.isEmpty() ? null : hallImages.get(selected).image();
        if (image == null) {
            detail.add(Component.translatable("gui.hyperscale.interface.empty"));
            if (!parts.drives.isEmpty()) {
                detail.add(Component.translatable("gui.hyperscale.interface.need_image"));
            }
        } else {
            detail.add(Component.translatable(
                    "gui.hyperscale.interface.runtime",
                    String.format("%.1f", image.cycleTicks() / 20.0D)
            ));
            addStacks(detail, "gui.hyperscale.interface.inputs", image.inputs());
            addStacks(detail, "gui.hyperscale.interface.outputs", image.outputs());
        }

        List<String> missing = new ArrayList<>();
        if (parts.inputs.isEmpty()) {
            missing.add(Component.translatable("gui.hyperscale.part.input").getString());
        }
        if (parts.outputs.isEmpty()) {
            missing.add(Component.translatable("gui.hyperscale.part.output").getString());
        }
        if (parts.racks.isEmpty()) {
            missing.add(Component.translatable("block.hyperscale.server_rack").getString());
        }
        if (parts.drives.isEmpty()) {
            missing.add(Component.translatable("block.hyperscale.drive_bay").getString());
        }
        if (!missing.isEmpty()) {
            detail.add(Component.translatable("gui.hyperscale.interface.missing", String.join(", ", missing)));
        }

        if (!parts.racks.isEmpty()) {
            detail.add(serverLine(level, parts));
        }

        int progress = progressOverride >= 0
                ? progressOverride
                : switchEntity == null ? 0 : switchEntity.progress();
        int cookingTime = cycleOverride > 0
                ? cycleOverride
                : image == null ? 0 : image.cycleTicks();
        if (image != null && progress > 0) {
            detail.add(Component.translatable("gui.hyperscale.interface.job", progress, cookingTime));
            detail.add(Component.translatable("gui.hyperscale.interface.heat_warm"));
            detail.add(Component.translatable("gui.hyperscale.interface.power_drawing"));
        } else if (image != null && HallRunner.findReadyRack(level, parts) != null && !parts.inputs.isEmpty() && !parts.outputs.isEmpty()) {
            detail.add(waitingOrIdle(level, parts, image));
            detail.add(Component.translatable("gui.hyperscale.interface.heat_cool"));
            detail.add(Component.translatable("gui.hyperscale.interface.power_idle"));
        }

        return new HallView(List.copyOf(rows), selected, List.copyOf(detail), progress, cookingTime);
    }

    private static Component imageLabel(int index, ProcessImage image) {
        if (!image.inputs().isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(image.inputs().get(0).item());
            return Component.translatable(
                    "gui.hyperscale.interface.image_named",
                    Component.translatable(item.getDescriptionId()),
                    String.format("%.1f", image.cycleTicks() / 20.0D)
            );
        }
        return Component.translatable(
                "gui.hyperscale.interface.image_row",
                index + 1,
                String.format("%.1f", image.cycleTicks() / 20.0D)
        );
    }

    private static void addStacks(List<Component> lines, String key, List<StackCount> stacks) {
        lines.add(Component.translatable(key));
        int shown = 0;
        for (StackCount stack : stacks) {
            if (shown == 3) {
                lines.add(Component.translatable("gui.hyperscale.interface.more"));
                return;
            }
            Item item = BuiltInRegistries.ITEM.get(stack.item());
            lines.add(Component.translatable("gui.hyperscale.interface.stack", stack.count(), Component.translatable(item.getDescriptionId())));
            shown++;
        }
    }

    private static Component serverLine(Level level, HallParts parts) {
        ServerRackBlockEntity rack = null;
        for (BlockPos pos : parts.racks) {
            if (level.getBlockEntity(pos) instanceof ServerRackBlockEntity found) {
                rack = found;
                if (found.isReady()) {
                    break;
                }
            }
        }
        if (rack == null) {
            return Component.translatable("gui.hyperscale.interface.server_missing", Component.translatable("item.hyperscale.server").getString());
        }
        if (rack.isReady()) {
            return Component.translatable("gui.hyperscale.interface.server_ready");
        }
        List<String> missing = new ArrayList<>();
        if (!rack.hasServer()) {
            missing.add(Component.translatable("item.hyperscale.server").getString());
        }
        if (!rack.hasCpu()) {
            missing.add(Component.translatable("item.hyperscale.cpu").getString());
        }
        if (!rack.hasRam()) {
            missing.add(Component.translatable("item.hyperscale.ram").getString());
        }
        return Component.translatable("gui.hyperscale.interface.server_missing", String.join(", ", missing));
    }

    private static Component waitingOrIdle(Level level, HallParts parts, ProcessImage image) {
        for (StackCount need : image.inputs()) {
            int found = 0;
            Item item = BuiltInRegistries.ITEM.get(need.item());
            for (BlockPos pos : parts.inputs) {
                if (level.getBlockEntity(pos) instanceof PortBlockEntity port && port.items().getStackInSlot(0).is(item)) {
                    found += port.items().getStackInSlot(0).getCount();
                }
            }
            if (found < need.count()) {
                return Component.translatable("gui.hyperscale.interface.waiting_input", Component.translatable(item.getDescriptionId()));
            }
        }
        if (!fits(level, parts, image.outputs())) {
            return Component.translatable("gui.hyperscale.interface.output_full");
        }
        return Component.translatable("gui.hyperscale.interface.idle");
    }

    private static boolean fits(Level level, HallParts parts, List<StackCount> outputs) {
        List<ItemStack> stacks = new ArrayList<>();
        for (BlockPos pos : parts.outputs) {
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                stacks.add(port.items().getStackInSlot(0).copy());
            }
        }
        for (StackCount output : outputs) {
            int left = output.count();
            Item item = BuiltInRegistries.ITEM.get(output.item());
            for (int index = 0; index < stacks.size() && left > 0; index++) {
                ItemStack stack = stacks.get(index);
                if (stack.isEmpty()) {
                    int moved = Math.min(left, item.getDefaultMaxStackSize());
                    stacks.set(index, new ItemStack(item, moved));
                    left -= moved;
                } else if (stack.is(item)) {
                    int moved = Math.min(left, stack.getMaxStackSize() - stack.getCount());
                    stack.grow(Math.max(0, moved));
                    left -= Math.max(0, moved);
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }
}
