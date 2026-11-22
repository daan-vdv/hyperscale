package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class HallRunner {
    private HallRunner() {
    }

    public static void tick(SwitchBlockEntity switchEntity) {
        Level level = switchEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        HallParts parts = HallFinder.find(level, switchEntity.getBlockPos());
        if (!parts.hasSwitch()) {
            return;
        }
        BlockPos primary = Collections.min(parts.switches);
        if (!primary.equals(switchEntity.getBlockPos())) {
            return;
        }

        switchEntity.clampSelection(level);
        ProcessImage image = selectedImage(level, switchEntity, parts);
        ServerRackBlockEntity rack = findReadyRack(level, parts);
        if (image == null || rack == null || !hasInputs(level, parts, image) || !canAccept(level, parts, image.outputs())) {
            if (image == null || rack == null || !hasInputs(level, parts, image)) {
                clearProgress(switchEntity);
            }
            return;
        }

        int progress = switchEntity.progress() + 1;
        if (progress >= image.cycleTicks()) {
            if (hasInputs(level, parts, image) && canAccept(level, parts, image.outputs())) {
                consume(level, parts, image.inputs());
                produce(level, parts, image.outputs());
            }
            progress = 0;
        }
        switchEntity.setProgress(progress);
        if (progress % 10 == 0) {
            switchEntity.sync();
        }
    }

    public static List<HallImage> listImages(Level level, BlockPos origin) {
        HallParts parts = HallFinder.find(level, origin);
        return listImages(level, parts);
    }

    public static List<HallImage> listImages(Level level, HallParts parts) {
        List<HallImage> images = new ArrayList<>();
        for (BlockPos pos : parts.drives) {
            if (!(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
                continue;
            }
            for (int slot = 0; slot < bay.items().getSlots(); slot++) {
                ItemStack stack = bay.items().getStackInSlot(slot);
                if (!stack.is(ModItems.IMAGE.get())) {
                    continue;
                }
                ProcessImage image = stack.get(ModDataComponents.PROCESS_IMAGE.get());
                if (image != null) {
                    images.add(new HallImage(pos, slot, image, stack.copy()));
                }
            }
        }
        return images;
    }

    public static ProcessImage selectedImage(Level level, SwitchBlockEntity switchEntity, HallParts parts) {
        List<HallImage> images = listImages(level, parts);
        if (images.isEmpty()) {
            return null;
        }
        int index = Math.min(switchEntity.selectedImage(), images.size() - 1);
        ProcessImage image = images.get(index).image();
        if (image.inputs().isEmpty() || image.outputs().isEmpty()) {
            return null;
        }
        return image;
    }

    public static ProcessImage findImage(Level level, HallParts parts) {
        for (HallImage hallImage : listImages(level, parts)) {
            ProcessImage image = hallImage.image();
            if (!image.inputs().isEmpty() && !image.outputs().isEmpty()) {
                return image;
            }
        }
        return null;
    }

    public static ServerRackBlockEntity findReadyRack(Level level, HallParts parts) {
        for (BlockPos pos : parts.racks) {
            if (level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack && rack.isReady()) {
                return rack;
            }
        }
        return null;
    }

    private static boolean hasInputs(Level level, HallParts parts, ProcessImage image) {
        for (StackCount need : image.inputs()) {
            if (count(level, parts.inputs, need.item()) < need.count()) {
                return false;
            }
        }
        return true;
    }

    private static int count(Level level, List<BlockPos> ports, ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        int total = 0;
        for (BlockPos pos : ports) {
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                ItemStack stack = port.items().getStackInSlot(0);
                if (stack.is(item)) {
                    total += stack.getCount();
                }
            }
        }
        return total;
    }

    private static boolean canAccept(Level level, HallParts parts, List<StackCount> outputs) {
        List<ItemStack> stacks = copies(level, parts.outputs);
        for (StackCount output : outputs) {
            if (place(stacks, BuiltInRegistries.ITEM.get(output.item()), output.count()) > 0) {
                return false;
            }
        }
        return true;
    }

    private static void consume(Level level, HallParts parts, List<StackCount> needs) {
        for (StackCount need : needs) {
            int left = need.count();
            Item item = BuiltInRegistries.ITEM.get(need.item());
            for (BlockPos pos : parts.inputs) {
                if (left <= 0 || !(level.getBlockEntity(pos) instanceof PortBlockEntity port)) {
                    continue;
                }
                ItemStackHandler handler = port.items();
                ItemStack stack = handler.getStackInSlot(0);
                if (!stack.is(item)) {
                    continue;
                }
                int used = Math.min(left, stack.getCount());
                stack.shrink(used);
                handler.setStackInSlot(0, stack.isEmpty() ? ItemStack.EMPTY : stack);
                left -= used;
            }
        }
    }

    private static void produce(Level level, HallParts parts, List<StackCount> outputs) {
        List<PortBlockEntity> ports = new ArrayList<>();
        List<ItemStack> stacks = new ArrayList<>();
        for (BlockPos pos : parts.outputs) {
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                ports.add(port);
                stacks.add(port.items().getStackInSlot(0).copy());
            }
        }
        for (StackCount output : outputs) {
            place(stacks, BuiltInRegistries.ITEM.get(output.item()), output.count());
        }
        for (int index = 0; index < ports.size(); index++) {
            ports.get(index).items().setStackInSlot(0, stacks.get(index));
        }
    }

    private static List<ItemStack> copies(Level level, List<BlockPos> ports) {
        List<ItemStack> stacks = new ArrayList<>();
        for (BlockPos pos : ports) {
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                stacks.add(port.items().getStackInSlot(0).copy());
            }
        }
        return stacks;
    }

    private static int place(List<ItemStack> stacks, Item item, int amount) {
        int left = amount;
        for (int index = 0; index < stacks.size() && left > 0; index++) {
            ItemStack stack = stacks.get(index);
            if (stack.isEmpty()) {
                int moved = Math.min(left, item.getDefaultMaxStackSize());
                stacks.set(index, new ItemStack(item, moved));
                left -= moved;
            } else if (stack.is(item)) {
                int moved = Math.min(left, stack.getMaxStackSize() - stack.getCount());
                if (moved > 0) {
                    stack.grow(moved);
                    left -= moved;
                }
            }
        }
        return left;
    }

    private static void clearProgress(SwitchBlockEntity switchEntity) {
        if (switchEntity.progress() != 0) {
            switchEntity.setProgress(0);
            switchEntity.sync();
        }
    }
}
