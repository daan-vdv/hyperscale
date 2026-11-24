package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.item.DriveContents;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.item.StorageDriveSpec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Unified AE2-style distributed storage pool backed by HDD/SSD/NVMe drives inside connected Drive Bays.
 */
public final class HallStorage {
    private final List<DriveSlot> drives;
    private final Set<DriveBayBlockEntity> dirtyBays = new HashSet<>();
    private final int totalCapacity;
    private final int totalIoPerTick;
    private int usedItems;

    private record DriveSlot(DriveBayBlockEntity bay, int slot, ItemStack stack, StorageDriveSpec spec, int effectiveMaxItems) {
    }

    private HallStorage(List<DriveSlot> drives, int totalCapacity, int usedItems, int totalIoPerTick) {
        this.drives = drives;
        this.totalCapacity = totalCapacity;
        this.usedItems = usedItems;
        this.totalIoPerTick = totalIoPerTick;
    }

    public static HallStorage scan(Level level, HallParts parts) {
        List<DriveSlot> drives = new ArrayList<>();
        int capacity = 0;
        int used = 0;
        int io = 0;

        for (BlockPos pos : parts.drives) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
                continue;
            }
            double raidMult = bay.capacityMultiplier();
            boolean raidProtected = bay.hasRaidFaultTolerance();
            ItemStackHandler handler = bay.items();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                ItemStack stack = handler.getStackInSlot(slot);
                if (stack.getItem() instanceof StorageDriveItem driveItem) {
                    if (HardwareHealth.isFailed(stack) && !raidProtected) {
                        continue;
                    }
                    StorageDriveSpec spec = driveItem.spec();
                    int effMax = Math.max(1, (int) Math.floor(spec.maxItems() * raidMult));
                    DriveContents contents = StorageDriveItem.getContents(stack);
                    drives.add(new DriveSlot(bay, slot, stack, spec, effMax));
                    capacity += effMax;
                    used += contents.totalItems();
                    io += spec.ioPerTick();
                }
            }
        }
        return new HallStorage(drives, capacity, used, io);
    }

    public boolean hasDrives() {
        return !this.drives.isEmpty();
    }

    public int totalCapacity() {
        return this.totalCapacity;
    }

    public int usedItems() {
        return this.usedItems;
    }

    public int totalIoPerTick() {
        return this.totalIoPerTick;
    }

    public int count(ResourceLocation itemId) {
        int total = 0;
        for (DriveSlot slot : this.drives) {
            total += StorageDriveItem.getContents(slot.stack()).countOf(itemId);
        }
        return total;
    }

    public boolean hasInputs(ProcessImage image) {
        if (image.inputs().isEmpty()) {
            return !image.fluidInputs().isEmpty();
        }
        for (StackCount need : image.inputs()) {
            if (this.count(need.item()) < need.count()) {
                return false;
            }
        }
        return true;
    }

    public boolean canInsertAll(List<StackCount> outputs) {
        if (outputs.isEmpty()) {
            return true;
        }
        if (this.drives.isEmpty()) {
            return false;
        }
        List<DriveContents> sim = new ArrayList<>(this.drives.size());
        for (DriveSlot slot : this.drives) {
            sim.add(StorageDriveItem.getContents(slot.stack()));
        }
        for (StackCount output : outputs) {
            int left = output.count();
            // Pass 1: drives that already contain this item type
            for (int i = 0; i < this.drives.size() && left > 0; i++) {
                DriveContents c = sim.get(i);
                if (c.containsType(output.item())) {
                    DriveSlot ds = this.drives.get(i);
                    var res = c.insert(output.item(), left, ds.effectiveMaxItems(), ds.spec().maxTypes());
                    sim.set(i, res.contents());
                    left -= res.accepted();
                }
            }
            // Pass 2: any drive with free type slot & space
            for (int i = 0; i < this.drives.size() && left > 0; i++) {
                DriveContents c = sim.get(i);
                if (!c.containsType(output.item())) {
                    DriveSlot ds = this.drives.get(i);
                    var res = c.insert(output.item(), left, ds.effectiveMaxItems(), ds.spec().maxTypes());
                    sim.set(i, res.contents());
                    left -= res.accepted();
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    public int insert(ResourceLocation itemId, int amount) {
        if (amount <= 0 || this.drives.isEmpty()) {
            return 0;
        }
        int left = amount;
        // Pass 1: pack into drives already holding this item type
        for (DriveSlot ds : this.drives) {
            if (left <= 0) {
                break;
            }
            DriveContents current = StorageDriveItem.getContents(ds.stack());
            if (!current.containsType(itemId)) {
                continue;
            }
            var res = current.insert(itemId, left, ds.effectiveMaxItems(), ds.spec().maxTypes());
            if (res.accepted() > 0) {
                StorageDriveItem.setContents(ds.stack(), res.contents());
                ds.bay().setChanged();
                this.dirtyBays.add(ds.bay());
                left -= res.accepted();
                this.usedItems += res.accepted();
            }
        }
        // Pass 2: allocate into drives with free type slots
        for (DriveSlot ds : this.drives) {
            if (left <= 0) {
                break;
            }
            DriveContents current = StorageDriveItem.getContents(ds.stack());
            if (current.containsType(itemId)) {
                continue;
            }
            var res = current.insert(itemId, left, ds.effectiveMaxItems(), ds.spec().maxTypes());
            if (res.accepted() > 0) {
                StorageDriveItem.setContents(ds.stack(), res.contents());
                ds.bay().setChanged();
                this.dirtyBays.add(ds.bay());
                left -= res.accepted();
                this.usedItems += res.accepted();
            }
        }
        return amount - left;
    }

    public int extract(ResourceLocation itemId, int amount) {
        if (amount <= 0 || this.drives.isEmpty()) {
            return 0;
        }
        int left = amount;
        for (DriveSlot ds : this.drives) {
            if (left <= 0) {
                break;
            }
            DriveContents current = StorageDriveItem.getContents(ds.stack());
            var res = current.extract(itemId, left);
            if (res.extracted() > 0) {
                StorageDriveItem.setContents(ds.stack(), res.contents());
                ds.bay().setChanged();
                this.dirtyBays.add(ds.bay());
                left -= res.extracted();
                this.usedItems = Math.max(0, this.usedItems - res.extracted());
            }
        }
        return amount - left;
    }

    public boolean hasDirtyBays() {
        return !this.dirtyBays.isEmpty();
    }

    public void syncDirtyBays() {
        for (DriveBayBlockEntity bay : this.dirtyBays) {
            bay.sync();
        }
        this.dirtyBays.clear();
    }

    public List<StackCount> aggregatedItems() {
        Map<ResourceLocation, Integer> merged = new LinkedHashMap<>();
        for (DriveSlot ds : this.drives) {
            for (StackCount entry : StorageDriveItem.getContents(ds.stack()).items()) {
                merged.merge(entry.item(), entry.count(), Integer::sum);
            }
        }
        List<StackCount> list = new ArrayList<>(merged.size());
        for (Map.Entry<ResourceLocation, Integer> e : merged.entrySet()) {
            list.add(new StackCount(e.getKey(), e.getValue()));
        }
        list.sort((a, b) -> Integer.compare(b.count(), a.count()));
        return list;
    }

    /**
     * Pulls items from connected Input Ports into Drive Bay storage, bounded by cluster I/O bandwidth.
     */
    public void ingestInputPorts(Level level, HallParts parts) {
        int budget = this.totalIoPerTick;
        if (budget <= 0 || this.drives.isEmpty()) {
            return;
        }
        for (BlockPos pos : parts.inputs) {
            if (budget <= 0) {
                break;
            }
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof PortBlockEntity port)) {
                continue;
            }
            ItemStack stack = port.items().getStackInSlot(0);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
            int toMove = Math.min(stack.getCount(), budget);
            int accepted = this.insert(itemId, toMove);
            if (accepted > 0) {
                ItemStack updated = stack.copy();
                updated.shrink(accepted);
                port.items().setStackInSlot(0, updated.isEmpty() ? ItemStack.EMPTY : updated);
                budget -= accepted;
            }
        }
    }

    /**
     * Pushes finished container outputs from Drive Bay storage into connected Output Ports, bounded by cluster I/O bandwidth.
     * - If an Output Port already holds an item, tops up that exact item type (acts as a manual filter).
     * - If an Output Port is empty, exports items produced by registered images that are not active inputs.
     */
    public void exportOutputPorts(Level level, HallParts parts, Map<String, ProcessImage> registry, List<HallDeployment> deployments) {
        int budget = this.totalIoPerTick;
        if (budget <= 0 || this.drives.isEmpty() || parts.outputs.isEmpty()) {
            return;
        }

        Set<ResourceLocation> activeInputs = new HashSet<>();
        Set<ResourceLocation> producedOutputs = new HashSet<>();
        for (HallDeployment d : deployments) {
            ProcessImage img = registry.get(d.imageKey());
            if (img == null) {
                continue;
            }
            for (StackCount in : img.inputs()) {
                activeInputs.add(in.item());
            }
            for (StackCount out : img.outputs()) {
                producedOutputs.add(out.item());
            }
        }
        if (producedOutputs.isEmpty()) {
            for (ProcessImage img : registry.values()) {
                for (StackCount out : img.outputs()) {
                    producedOutputs.add(out.item());
                }
            }
        }

        for (BlockPos pos : parts.outputs) {
            if (budget <= 0) {
                break;
            }
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof PortBlockEntity port)) {
                continue;
            }
            ItemStack current = port.items().getStackInSlot(0);
            boolean hasFilter = port.hasFilter();
            if (!current.isEmpty()) {
                int space = current.getMaxStackSize() - current.getCount();
                if (space <= 0) {
                    continue;
                }
                ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(current.getItem());
                if (hasFilter && !port.allowsExport(itemId)) {
                    continue;
                }
                int extracted = this.extract(itemId, Math.min(space, budget));
                if (extracted > 0) {
                    ItemStack updated = current.copy();
                    updated.grow(extracted);
                    port.items().setStackInSlot(0, updated);
                    budget -= extracted;
                }
            } else {
                ResourceLocation chosen = null;
                if (hasFilter) {
                    List<ResourceLocation> whitelist = port.filterItemIds();
                    int start = port.nextFilterCursor(whitelist.size());
                    for (int i = 0; i < whitelist.size(); i++) {
                        ResourceLocation candidate = whitelist.get((start + i) % whitelist.size());
                        if (this.count(candidate) > 0) {
                            chosen = candidate;
                            break;
                        }
                    }
                } else {
                    // Unfiltered: pick the first available container output that is not an active ingredient
                    for (ResourceLocation candidate : producedOutputs) {
                        if (!activeInputs.contains(candidate) && this.count(candidate) > 0) {
                            chosen = candidate;
                            break;
                        }
                    }
                }
                if (chosen == null) {
                    continue;
                }
                Item item = BuiltInRegistries.ITEM.get(chosen);
                if (item == Items.AIR) {
                    continue;
                }
                int maxMove = Math.min(item.getDefaultMaxStackSize(), budget);
                int extracted = this.extract(chosen, maxMove);
                if (extracted > 0) {
                    port.items().setStackInSlot(0, new ItemStack(item, extracted));
                    budget -= extracted;
                }
            }
        }
    }
}
