package dev.daan.hyperscale.record;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.daan.hyperscale.hall.StackCount;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class ItemTotals {
    private ItemTotals() {
    }

    public static Map<ResourceLocation, Integer> snapshot(Level level, Set<BlockPos> positions) {
        Map<ResourceLocation, Integer> totals = new HashMap<>();
        Set<IItemHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions) {
            List<IItemHandler> handlers = new ArrayList<>();
            IItemHandler unsided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
            if (unsided != null) {
                handlers.add(unsided);
            }
            for (Direction direction : Direction.values()) {
                IItemHandler sided = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, direction);
                if (sided != null) {
                    handlers.add(sided);
                }
            }
            for (IItemHandler handler : handlers) {
                if (!seen.add(handler)) {
                    continue;
                }
                for (int slot = 0; slot < handler.getSlots(); slot++) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty()) {
                        continue;
                    }
                    totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
                }
            }
        }
        totals.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return totals;
    }

    public static Map<ResourceLocation, Integer> copy(Map<ResourceLocation, Integer> totals) {
        return new HashMap<>(totals);
    }

    public static void add(Map<ResourceLocation, Integer> totals, ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        totals.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getCount(), Integer::sum);
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
}
