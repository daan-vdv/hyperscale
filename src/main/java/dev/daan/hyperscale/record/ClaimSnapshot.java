package dev.daan.hyperscale.record;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Full steady-state snapshot of a sealed casing's interior:
 * - Items keyed by (Item ID + DataComponentPatch) across all interior inventories AND dropped ItemEntities
 * - Fluids keyed by Fluid ID (mB)
 * - Total stored Forge Energy (FE)
 * - 64-bit hash of interior Block identities (catches TNT/auto-breaker destruction while allowing LIT/ACTIVE state flips)
 */
public record ClaimSnapshot(
        Map<ItemKey, Integer> items,
        Map<ResourceLocation, Integer> fluids,
        long energy,
        long blockHash
) {
    public record ItemKey(ResourceLocation item, DataComponentPatch components) {
        public static ItemKey of(ItemStack stack) {
            return new ItemKey(BuiltInRegistries.ITEM.getKey(stack.getItem()), stack.getComponentsPatch());
        }
    }

    public record Diff(RecordStatus status, String detail) {
    }

    public static final ClaimSnapshot EMPTY = new ClaimSnapshot(Map.of(), Map.of(), 0L, 0L);

    public static ClaimSnapshot capture(
            Level level,
            Set<BlockPos> inventories,
            Set<BlockPos> energyBlocks,
            Set<BlockPos> fluidBlocks,
            BlockPos innerMin,
            BlockPos innerMax
    ) {
        Map<ItemKey, Integer> items = captureItems(level, inventories, innerMin, innerMax);
        Map<ResourceLocation, Integer> fluids = captureFluids(level, fluidBlocks);
        long energy = captureEnergy(level, energyBlocks);
        long blockHash = captureBlockHash(level, innerMin, innerMax);
        return new ClaimSnapshot(items, fluids, energy, blockHash);
    }

    public static Diff diff(ClaimSnapshot baseline, ClaimSnapshot now, int energyTolerance) {
        if (baseline.blockHash != now.blockHash) {
            return new Diff(RecordStatus.BLOCK_DRIFT, "");
        }
        for (Map.Entry<ItemKey, Integer> entry : baseline.items.entrySet()) {
            int current = now.items.getOrDefault(entry.getKey(), 0);
            if (current != entry.getValue()) {
                return new Diff(RecordStatus.DRIFT, entry.getKey().item().toString());
            }
        }
        for (Map.Entry<ItemKey, Integer> entry : now.items.entrySet()) {
            if (!baseline.items.containsKey(entry.getKey()) && entry.getValue() > 0) {
                return new Diff(RecordStatus.DRIFT, entry.getKey().item().toString());
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : baseline.fluids.entrySet()) {
            int current = now.fluids.getOrDefault(entry.getKey(), 0);
            if (current != entry.getValue()) {
                return new Diff(RecordStatus.FLUID_DRIFT, entry.getKey().toString());
            }
        }
        for (Map.Entry<ResourceLocation, Integer> entry : now.fluids.entrySet()) {
            if (!baseline.fluids.containsKey(entry.getKey()) && entry.getValue() > 0) {
                return new Diff(RecordStatus.FLUID_DRIFT, entry.getKey().toString());
            }
        }
        if (baseline.energy - now.energy > Math.max(0, energyTolerance)) {
            return new Diff(RecordStatus.ENERGY_DRIFT, "");
        }
        return null;
    }

    private static long captureBlockHash(Level level, BlockPos innerMin, BlockPos innerMax) {
        long hash = 0xcbf29ce484222325L;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = innerMin.getX(); x <= innerMax.getX(); x++) {
            for (int y = innerMin.getY(); y <= innerMax.getY(); y++) {
                for (int z = innerMin.getZ(); z <= innerMax.getZ(); z++) {
                    cursor.set(x, y, z);
                    int blockId = level.isLoaded(cursor)
                            ? BuiltInRegistries.BLOCK.getId(level.getBlockState(cursor).getBlock())
                            : -1;
                    hash ^= blockId;
                    hash *= 0x100000001b3L;
                }
            }
        }
        return hash;
    }

    private static Map<ItemKey, Integer> captureItems(Level level, Set<BlockPos> positions, BlockPos innerMin, BlockPos innerMax) {
        Map<ItemKey, Integer> totals = new HashMap<>();
        Set<IItemHandler> seenHandlers = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container) {
                for (int slot = 0; slot < container.getContainerSize(); slot++) {
                    ItemStack stack = container.getItem(slot);
                    if (!stack.isEmpty()) {
                        totals.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
                    }
                }
                continue;
            }
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
        // Include dropped ItemEntities inside the sealed interior so loose stacks cannot be used to dupe
        AABB interior = new AABB(
                innerMin.getX(), innerMin.getY(), innerMin.getZ(),
                innerMax.getX() + 1.0D, innerMax.getY() + 1.0D, innerMax.getZ() + 1.0D
        );
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, interior, e -> e.isAlive() && !e.getItem().isEmpty())) {
            ItemStack stack = entity.getItem();
            totals.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
        }
        totals.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return totals;
    }

    private static void addHandler(Map<ItemKey, Integer> totals, IItemHandler handler) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                totals.merge(ItemKey.of(stack), stack.getCount(), Integer::sum);
            }
        }
    }

    private static Map<ResourceLocation, Integer> captureFluids(Level level, Set<BlockPos> positions) {
        Map<ResourceLocation, Integer> totals = new HashMap<>();
        Set<IFluidHandler> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            IFluidHandler unsided = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, null);
            if (unsided != null) {
                if (seen.add(unsided)) {
                    addFluidHandler(totals, unsided);
                }
                continue;
            }
            for (Direction direction : Direction.values()) {
                IFluidHandler sided = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, direction);
                if (sided != null && seen.add(sided)) {
                    addFluidHandler(totals, sided);
                }
            }
        }
        totals.entrySet().removeIf(entry -> entry.getValue() <= 0);
        return totals;
    }

    private static void addFluidHandler(Map<ResourceLocation, Integer> totals, IFluidHandler handler) {
        for (int tank = 0; tank < handler.getTanks(); tank++) {
            FluidStack fluid = handler.getFluidInTank(tank);
            if (!fluid.isEmpty()) {
                totals.merge(BuiltInRegistries.FLUID.getKey(fluid.getFluid()), fluid.getAmount(), Integer::sum);
            }
        }
    }

    private static long captureEnergy(Level level, Set<BlockPos> positions) {
        long total = 0L;
        Set<IEnergyStorage> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BlockPos pos : positions) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            IEnergyStorage unsided = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, null);
            if (unsided != null) {
                if (seen.add(unsided)) {
                    total += Math.max(0, unsided.getEnergyStored());
                }
                continue;
            }
            for (Direction direction : Direction.values()) {
                IEnergyStorage sided = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, direction);
                if (sided != null && seen.add(sided)) {
                    total += Math.max(0, sided.getEnergyStored());
                }
            }
        }
        return total;
    }
}
