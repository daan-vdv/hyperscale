package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.block.PowerPortBlock;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

public class PowerPortBlockEntity extends BlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_TRANSFER = 50_000;

    private BlockPos compiler;

    private final EnergyStorage buffer = new EnergyStorage(CAPACITY, MAX_TRANSFER, MAX_TRANSFER) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate && accepted > 0) {
                PowerPortBlockEntity.this.setChanged();
            }
            return accepted;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = super.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) {
                PowerPortBlockEntity.this.setChanged();
            }
            return extracted;
        }
    };

    private final IEnergyStorage exteriorInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return PowerPortBlockEntity.this.buffer.receiveEnergy(toReceive, simulate);
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return PowerPortBlockEntity.this.buffer.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return PowerPortBlockEntity.this.buffer.getMaxEnergyStored();
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

    private final IEnergyStorage interiorOutput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int toExtract, boolean simulate) {
            int extracted = PowerPortBlockEntity.this.buffer.extractEnergy(toExtract, simulate);
            if (!simulate && extracted > 0) {
                PowerPortBlockEntity.this.notifyEnergyMoved(extracted);
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return PowerPortBlockEntity.this.buffer.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return PowerPortBlockEntity.this.buffer.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT.get(), pos, state);
    }

    public IEnergyStorage capability(Direction side) {
        Direction interiorSide = this.getBlockState().getValue(PowerPortBlock.FACING).getOpposite();
        if (side == interiorSide) {
            return this.interiorOutput;
        }
        return this.exteriorInput;
    }

    private void notifyEnergyMoved(int amount) {
        if (this.level != null && this.compiler != null && this.level.isLoaded(this.compiler)
                && this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder
                && recorder.isSampling()) {
            recorder.recordEnergy(amount);
        }
    }

    public BlockPos compiler() {
        return this.compiler;
    }

    public void setCompiler(BlockPos compiler) {
        this.compiler = compiler == null ? null : compiler.immutable();
        this.setChanged();
    }

    public void clearCompiler() {
        this.compiler = null;
        this.setChanged();
    }

    public static void serverTick(PowerPortBlockEntity port) {
        port.pushEnergy();
    }

    private void pushEnergy() {
        if (this.level == null || this.buffer.getEnergyStored() <= 0) {
            return;
        }
        Direction facing = this.getBlockState().getValue(PowerPortBlock.FACING);
        Direction interiorDir = facing.getOpposite();
        BlockPos neighbor = this.worldPosition.relative(interiorDir);
        if (!this.level.isLoaded(neighbor)) {
            return;
        }
        IEnergyStorage target = this.level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, facing);
        if (target == null) {
            target = this.level.getCapability(Capabilities.EnergyStorage.BLOCK, neighbor, null);
        }
        if (target == null || !target.canReceive()) {
            return;
        }
        int available = Math.min(this.buffer.getEnergyStored(), MAX_TRANSFER);
        int accepted = target.receiveEnergy(available, false);
        if (accepted > 0) {
            this.buffer.extractEnergy(accepted, false);
            this.notifyEnergyMoved(accepted);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", this.buffer.getEnergyStored());
        if (this.compiler != null) {
            tag.putLong("Compiler", this.compiler.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int stored = Math.max(0, Math.min(CAPACITY, tag.getInt("Energy")));
        this.buffer.receiveEnergy(stored - this.buffer.getEnergyStored(), false);
        this.compiler = tag.contains("Compiler") ? BlockPos.of(tag.getLong("Compiler")) : null;
    }
}
