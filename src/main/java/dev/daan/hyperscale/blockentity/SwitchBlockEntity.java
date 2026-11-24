package dev.daan.hyperscale.blockentity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dev.daan.hyperscale.hall.HallCapacity;
import dev.daan.hyperscale.hall.HallDeployment;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallParts;
import dev.daan.hyperscale.hall.HallRunner;
import dev.daan.hyperscale.hall.HallThermal;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.ItemStackHandler;

public class SwitchBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 1_000_000;
    public static final int MAX_ENERGY_RECEIVE = 100_000;
    public static final int MAX_DEPLOYMENTS = 32;
    public static final int IMAGE_SLOTS = 6;
    private static final long TOPOLOGY_TTL_TICKS = 10L;

    private final List<HallDeployment> deployments = new ArrayList<>();
    private int usedRamGb;
    private int usedVcpu;
    private int lastTotalFePerTick;
    private int lastComputeFePerTick;
    private int lastCoolingFePerTick;
    private int usedStorageItems;
    private int totalStorageCapacity;
    private int storageIoPerTick;
    private double hallTempC = HallThermal.AMBIENT_TEMP_C;
    private int heatWatts;
    private int coolingWatts = HallThermal.PASSIVE_COOLING_W;
    private int noiseDb = 24;
    private boolean thermalShutdown;
    private boolean liquidCooled;
    private boolean suppressImageCallbacks;

    private final ItemStackHandler imageCards = new ItemStackHandler(IMAGE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            if (!SwitchBlockEntity.this.suppressImageCallbacks) {
                SwitchBlockEntity.this.sync();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(ModItems.IMAGE.get());
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };

    private HallParts cachedParts;
    private long cachedPartsTick = Long.MIN_VALUE;
    private HallCapacity cachedCapacity;
    private long cachedCapacityTick = Long.MIN_VALUE;
    private Map<String, ProcessImage> cachedImages;
    private long cachedImagesTick = Long.MIN_VALUE;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_ENERGY_RECEIVE, ENERGY_CAPACITY) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate && accepted > 0) {
                SwitchBlockEntity.this.setChanged();
            }
            return accepted;
        }
    };

    private final IEnergyStorage exteriorEnergy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = SwitchBlockEntity.this.energy.receiveEnergy(toReceive, simulate);
            int remaining = toReceive - accepted;
            if (remaining > 0 && SwitchBlockEntity.this.level != null && !SwitchBlockEntity.this.level.isClientSide) {
                HallParts parts = SwitchBlockEntity.this.hallParts(SwitchBlockEntity.this.level);
                for (BlockPos genPos : parts.generators) {
                    if (remaining <= 0) {
                        break;
                    }
                    if (SwitchBlockEntity.this.level.isLoaded(genPos)
                            && SwitchBlockEntity.this.level.getBlockEntity(genPos) instanceof BackupGeneratorBlockEntity gen) {
                        int moved = gen.energyCapability().receiveEnergy(remaining, simulate);
                        accepted += moved;
                        remaining -= moved;
                    }
                }
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return SwitchBlockEntity.this.energy.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return SwitchBlockEntity.this.energy.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH.get(), pos, state);
    }

    public static void serverTick(SwitchBlockEntity blockEntity) {
        HallRunner.tick(blockEntity);
    }

    public ItemStackHandler imageCards() {
        return this.imageCards;
    }

    public boolean insertImageCard(ItemStack stack, boolean creative) {
        if (stack.isEmpty() || !stack.is(ModItems.IMAGE.get())) {
            return false;
        }
        for (int slot = 0; slot < this.imageCards.getSlots(); slot++) {
            if (this.imageCards.getStackInSlot(slot).isEmpty()) {
                ItemStack single = creative ? stack.copyWithCount(1) : stack.split(1);
                this.imageCards.setStackInSlot(slot, single);
                return true;
            }
        }
        return false;
    }

    public int imageCardCount() {
        int count = 0;
        for (int slot = 0; slot < this.imageCards.getSlots(); slot++) {
            ItemStack stack = this.imageCards.getStackInSlot(slot);
            if (stack.is(ModItems.IMAGE.get()) && stack.has(ModDataComponents.PROCESS_IMAGE.get())) {
                count++;
            }
        }
        return count;
    }

    public void dropImageCards() {
        if (this.level == null || this.level.isClientSide) {
            return;
        }
        this.suppressImageCallbacks = true;
        try {
            for (int slot = 0; slot < this.imageCards.getSlots(); slot++) {
                ItemStack stack = this.imageCards.getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    Block.popResource(this.level, this.worldPosition, stack.copy());
                    this.imageCards.setStackInSlot(slot, ItemStack.EMPTY);
                }
            }
        } finally {
            this.suppressImageCallbacks = false;
        }
    }

    public IEnergyStorage energyCapability() {
        return this.exteriorEnergy;
    }

    public int energyStored() {
        return this.energy.getEnergyStored();
    }

    public int maxEnergyStored() {
        return this.energy.getMaxEnergyStored();
    }

    public boolean consumeEnergy(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (this.energy.getEnergyStored() < amount) {
            return false;
        }
        this.energy.extractEnergy(amount, false);
        this.setChanged();
        return true;
    }

    public HallParts hallParts(Level level) {
        long now = level.getGameTime();
        if (this.cachedParts == null || now - this.cachedPartsTick >= TOPOLOGY_TTL_TICKS || now < this.cachedPartsTick) {
            this.cachedParts = HallFinder.find(level, this.worldPosition);
            this.cachedPartsTick = now;
        }
        return this.cachedParts;
    }

    public HallCapacity cachedCapacity(Level level, HallParts parts) {
        long now = level.getGameTime();
        if (this.cachedCapacity == null || now - this.cachedCapacityTick >= TOPOLOGY_TTL_TICKS || now < this.cachedCapacityTick) {
            this.cachedCapacity = HallCapacity.compute(level, parts);
            this.cachedCapacityTick = now;
        }
        return this.cachedCapacity;
    }

    public Map<String, ProcessImage> cachedImages(Level level, HallParts parts) {
        long now = level.getGameTime();
        if (this.cachedImages == null || now - this.cachedImagesTick >= TOPOLOGY_TTL_TICKS || now < this.cachedImagesTick) {
            this.cachedImages = HallRunner.indexImages(level, parts);
            this.cachedImagesTick = now;
        }
        return this.cachedImages;
    }

    public List<HallDeployment> deployments() {
        return Collections.unmodifiableList(this.deployments);
    }

    public HallDeployment findDeployment(String imageKey) {
        for (HallDeployment deployment : this.deployments) {
            if (deployment.imageKey().equals(imageKey)) {
                return deployment;
            }
        }
        return null;
    }

    public boolean deployOrScaleUp(String imageKey, int delta) {
        if (imageKey == null || imageKey.isEmpty() || delta <= 0) {
            return false;
        }
        HallDeployment existing = this.findDeployment(imageKey);
        if (existing != null) {
            existing.setReplicas(existing.replicas() + delta);
            existing.setPaused(false);
            this.sync();
            return true;
        }
        if (this.deployments.size() >= MAX_DEPLOYMENTS) {
            return false;
        }
        this.deployments.add(new HallDeployment(imageKey, delta));
        this.sync();
        return true;
    }

    public boolean scaleDeployment(String imageKey, int delta) {
        HallDeployment existing = this.findDeployment(imageKey);
        if (existing == null || delta == 0) {
            return false;
        }
        int next = Math.max(1, Math.min(HallDeployment.MAX_REPLICAS, existing.replicas() + delta));
        if (next != existing.replicas()) {
            existing.setReplicas(next);
            this.sync();
            return true;
        }
        return false;
    }

    public boolean togglePause(String imageKey) {
        HallDeployment existing = this.findDeployment(imageKey);
        if (existing == null) {
            return false;
        }
        existing.setPaused(!existing.paused());
        this.sync();
        return true;
    }

    public boolean cycleProfile(String imageKey) {
        HallDeployment existing = this.findDeployment(imageKey);
        if (existing == null) {
            return false;
        }
        existing.setProfile(existing.profile().next());
        this.sync();
        return true;
    }

    public boolean removeDeployment(String imageKey) {
        boolean removed = this.deployments.removeIf(d -> d.imageKey().equals(imageKey));
        if (removed) {
            this.sync();
        }
        return removed;
    }

    public int usedRamGb() {
        return this.usedRamGb;
    }

    public int usedVcpu() {
        return this.usedVcpu;
    }

    public int lastTotalFePerTick() {
        return this.lastTotalFePerTick;
    }

    public int lastComputeFePerTick() {
        return this.lastComputeFePerTick;
    }

    public int lastCoolingFePerTick() {
        return this.lastCoolingFePerTick;
    }

    public double pue() {
        if (this.lastComputeFePerTick <= 0) {
            return 1.00D;
        }
        return (double) (this.lastComputeFePerTick + this.lastCoolingFePerTick) / this.lastComputeFePerTick;
    }

    public int usedStorageItems() {
        return this.usedStorageItems;
    }

    public int totalStorageCapacity() {
        return this.totalStorageCapacity;
    }

    public int storageIoPerTick() {
        return this.storageIoPerTick;
    }

    public double hallTempC() {
        return this.hallTempC;
    }

    public int heatWatts() {
        return this.heatWatts;
    }

    public int coolingWatts() {
        return this.coolingWatts;
    }

    public int noiseDb() {
        return this.noiseDb;
    }

    public boolean thermalShutdown() {
        return this.thermalShutdown;
    }

    public boolean liquidCooled() {
        return this.liquidCooled;
    }

    public void setThermalState(double tempC, int heatWatts, int coolingWatts, int noiseDb, boolean thermalShutdown, boolean liquidCooled) {
        this.hallTempC = Math.max(HallThermal.AMBIENT_TEMP_C, Math.min(120.0D, tempC));
        this.heatWatts = Math.max(0, heatWatts);
        this.coolingWatts = Math.max(HallThermal.PASSIVE_COOLING_W, coolingWatts);
        this.noiseDb = Math.max(20, noiseDb);
        this.thermalShutdown = thermalShutdown;
        this.liquidCooled = liquidCooled;
    }

    public void setTelemetry(
            int usedRamGb,
            int usedVcpu,
            int lastTotalFePerTick,
            int lastComputeFePerTick,
            int lastCoolingFePerTick,
            int usedStorageItems,
            int totalStorageCapacity,
            int storageIoPerTick
    ) {
        this.usedRamGb = Math.max(0, usedRamGb);
        this.usedVcpu = Math.max(0, usedVcpu);
        this.lastTotalFePerTick = Math.max(0, lastTotalFePerTick);
        this.lastComputeFePerTick = Math.max(0, lastComputeFePerTick);
        this.lastCoolingFePerTick = Math.max(0, lastCoolingFePerTick);
        this.usedStorageItems = Math.max(0, usedStorageItems);
        this.totalStorageCapacity = Math.max(0, totalStorageCapacity);
        this.storageIoPerTick = Math.max(0, storageIoPerTick);
    }

    public void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", this.energy.getEnergyStored());
        tag.putInt("UsedRam", this.usedRamGb);
        tag.putInt("UsedVcpu", this.usedVcpu);
        tag.putInt("LastTotalFe", this.lastTotalFePerTick);
        tag.putInt("LastComputeFe", this.lastComputeFePerTick);
        tag.putInt("LastCoolingFe", this.lastCoolingFePerTick);
        tag.putInt("UsedStorage", this.usedStorageItems);
        tag.putInt("TotalStorage", this.totalStorageCapacity);
        tag.putInt("StorageIo", this.storageIoPerTick);
        tag.putDouble("HallTempC", this.hallTempC);
        tag.putInt("HeatWatts", this.heatWatts);
        tag.putInt("CoolingWatts", this.coolingWatts);
        tag.putInt("NoiseDb", this.noiseDb);
        tag.putBoolean("ThermalShutdown", this.thermalShutdown);
        tag.putBoolean("LiquidCooled", this.liquidCooled);
        tag.put("ImageCards", this.imageCards.serializeNBT(registries));
        ListTag list = new ListTag();
        for (HallDeployment deployment : this.deployments) {
            list.add(deployment.save());
        }
        tag.put("Deployments", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int stored = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getInt("Energy")));
        int delta = stored - this.energy.getEnergyStored();
        if (delta > 0) {
            this.energy.receiveEnergy(delta, false);
        } else if (delta < 0) {
            this.energy.extractEnergy(-delta, false);
        }
        this.usedRamGb = Math.max(0, tag.getInt("UsedRam"));
        this.usedVcpu = Math.max(0, tag.getInt("UsedVcpu"));
        this.lastTotalFePerTick = Math.max(0, tag.getInt("LastTotalFe"));
        this.lastComputeFePerTick = Math.max(0, tag.getInt("LastComputeFe"));
        this.lastCoolingFePerTick = Math.max(0, tag.getInt("LastCoolingFe"));
        this.usedStorageItems = Math.max(0, tag.getInt("UsedStorage"));
        this.totalStorageCapacity = Math.max(0, tag.getInt("TotalStorage"));
        this.storageIoPerTick = Math.max(0, tag.getInt("StorageIo"));
        this.hallTempC = tag.contains("HallTempC") ? Math.max(HallThermal.AMBIENT_TEMP_C, tag.getDouble("HallTempC")) : HallThermal.AMBIENT_TEMP_C;
        this.heatWatts = Math.max(0, tag.getInt("HeatWatts"));
        this.coolingWatts = tag.contains("CoolingWatts") ? Math.max(HallThermal.PASSIVE_COOLING_W, tag.getInt("CoolingWatts")) : HallThermal.PASSIVE_COOLING_W;
        this.noiseDb = tag.contains("NoiseDb") ? Math.max(20, tag.getInt("NoiseDb")) : 24;
        this.thermalShutdown = tag.getBoolean("ThermalShutdown");
        this.liquidCooled = tag.getBoolean("LiquidCooled");

        if (tag.contains("ImageCards", Tag.TAG_COMPOUND)) {
            CompoundTag invTag = tag.getCompound("ImageCards").copy();
            invTag.putInt("Size", IMAGE_SLOTS);
            this.imageCards.deserializeNBT(registries, invTag);
        }

        this.deployments.clear();
        if (tag.contains("Deployments", Tag.TAG_LIST)) {
            ListTag list = tag.getList("Deployments", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size() && this.deployments.size() < MAX_DEPLOYMENTS; i++) {
                HallDeployment loaded = HallDeployment.load(list.getCompound(i));
                if (loaded != null) {
                    this.deployments.add(loaded);
                }
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
