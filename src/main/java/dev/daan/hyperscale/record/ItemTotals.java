package dev.daan.hyperscale.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.daan.hyperscale.hall.FluidStackCount;
import dev.daan.hyperscale.hall.StackCount;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;

public final class ItemTotals {
    private ItemTotals() {
    }

    public static Map<ResourceLocation, Integer> snapshot(Level level, Set<BlockPos> positions) {
        Map<ResourceLocation, Integer> totals = new HashMap<>();
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            // Vanilla Container BEs (Furnace, Chest, Hopper, Barrel, Brewing Stand, etc.) expose their
            // exact slots directly without creating duplicate SidedInvWrappers per face.
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
                    }
                }
                continue;
            }
            // Modded BEs: prefer the unsided (null) capability; only fall back to directional sides if absent.
            IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (unsided != null) {
                if (seenHandlers.add(unsided)) {
                    addHandler(totals, unsided);
                }
                continue;
            }
            for (Direction direction : Direction.values()) {
                IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
                if (sided != null && seenHandlers.add(sided)) {
                    addHandler(totals, sided);
                }
            }
        }
        totals.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return totals;
    }

    private static void addHandler(Map<ResourceLocation, Integer> totals, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
            }
        }
    }

    public static void add(Map<ResourceLocation, Integer> totals, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
    }

    public static void addFluid(Map<ResourceLocation, Integer> totals, FluidStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        totals.merge(BuiltInRegistries.FLUID.getKey(stack.getFluid()), stack.getAmount(), Integer::sum);
    }

    public static boolean same(Map<ResourceLocation, Integer> left, Map<ResourceLocation, Integer> right) {
        return firstDiff(left, right) == null;
    }

    public static ResourceLocation firstDiff(Map<ResourceLocation, Integer> start, Map<ResourceLocation, Integer> now) {
        for (Map.Entry<ResourceLocation, Integer> entry : start.entrySet()) {
            int current = now.getOrDefault(entry.getKey(), 0);
            if (current != entry.getValue()) {
                return entry.getKey();
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : now.entrySet()) {
            if (!start.containsKey(entry.getKey()) && entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static List<StackCount> freeze(Map<ResourceLocation, Integer> totals) {
        List<StackCount> stacks = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                stacks.add(new StackCount(entry.getKey(), entry.getValue()));
            }
        }
        stacks.sort(Comparator.comparing(stack -> stack.item().toString()));
        return List.copyOf(stacks);
    }

    public static List<FluidStackCount> freezeFluids(Map<ResourceLocation, Integer> totals) {
        List<FluidStackCount> stacks = new ArrayList<>();
        for (Map.Entry<ResourceLocation, Integer> entry : totals.entrySet()) {
            if (entry.getValue() > 0) {
                stacks.add(new FluidStackCount(entry.getKey(), entry.getValue()));
            }
        }
        stacks.sort(Comparator.comparing(stack -> stack.fluid().toString()));
        return List.copyOf(stacks);
    }
}
