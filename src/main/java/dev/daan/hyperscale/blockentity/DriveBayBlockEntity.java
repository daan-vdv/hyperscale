package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.hall.RaidMode;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class DriveBayBlockEntity extends InventoryBlockEntity {
    public static final int SLOTS = 6;

    private RaidMode raidMode = RaidMode.JBOD;

    public DriveBayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DRIVE_BAY.get(), pos, state, SLOTS);
    }

    public RaidMode raidMode() {
        return this.raidMode;
    }

    public void cycleRaidMode() {
        this.raidMode = this.raidMode.next();
        this.sync();
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return stack.getItem() instanceof StorageDriveItem;
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    public int driveCount() {
        int count = 0;
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            if (this.items.getStackInSlot(slot).getItem() instanceof StorageDriveItem) {
                count++;
            }
        }
        return count;
    }

    public int failedDriveCount() {
        int count = 0;
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.getItem() instanceof StorageDriveItem && HardwareHealth.isFailed(stack)) {
                count++;
            }
        }
        return count;
    }

    public int degradedDriveCount() {
        int count = 0;
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.getItem() instanceof StorageDriveItem && HardwareHealth.isDegraded(stack)) {
                count++;
            }
        }
        return count;
    }

    /**
     * Returns true if this bay's RAID mode provides 1-drive fault tolerance with the currently installed drives.
     */
    public boolean hasRaidFaultTolerance() {
        int total = this.driveCount();
        int failed = this.failedDriveCount();
        if (this.raidMode == RaidMode.RAID1) {
            return total >= 2 && failed <= 1;
        }
        if (this.raidMode == RaidMode.RAID5) {
            return total >= 3 && failed <= 1;
        }
        return false;
    }

    /**
     * Usable capacity multiplier for each drive in this bay under the active RAID mode.
     */
    public double capacityMultiplier() {
        int total = this.driveCount();
        if (this.raidMode == RaidMode.RAID1 && total >= 2) {
            return 0.50D;
        }
        if (this.raidMode == RaidMode.RAID5 && total >= 3) {
            return (double) (total - 1) / total;
        }
        return 1.00D;
    }

    public int usedItems() {
        int sum = 0;
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.getItem() instanceof StorageDriveItem) {
                sum += StorageDriveItem.getContents(stack).totalItems();
            }
        }
        return sum;
    }

    public int totalCapacity() {
        int sum = 0;
        double mult = this.capacityMultiplier();
        boolean raidProtected = this.hasRaidFaultTolerance();
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (stack.getItem() instanceof StorageDriveItem drive) {
                if (!HardwareHealth.isFailed(stack) || raidProtected) {
                    sum += (int) Math.floor(drive.spec().maxItems() * mult);
                }
            }
        }
        return sum;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("RaidMode", this.raidMode.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.raidMode = RaidMode.byId(tag.getString("RaidMode"));
    }
}
