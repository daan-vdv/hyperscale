package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * 2x2x2 Backup Generator (UPS) providing a 10M FE shared battery buffer for the datacenter hall.
 */
public class BackupGeneratorBlockEntity extends BlockEntity {
    public static final int ENERGY_CAPACITY = 10_000_000;
    public static final int MAX_TRANSFER = 250_000;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate && accepted > 0) {
                BackupGeneratorBlockEntity.this.setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) {
                BackupGeneratorBlockEntity.this.setChanged();
            }
            return extracted;
        }
    };

    public BackupGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BACKUP_GENERATOR.get(), pos, state);
    }

    public IEnergyStorage energyCapability() {
        return this.energy;
    }

    public int energyStored() {
        return this.energy.getEnergyStored();
    }

    public int maxEnergyStored() {
        return this.energy.getMaxEnergyStored();
    }

    public int extractForHall(int requested) {
        if (requested <= 0) {
            return 0;
        }
        int taken = this.energy.extractEnergy(requested, false);
        if (taken > 0) {
            this.setChanged();
        }
        return taken;
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
