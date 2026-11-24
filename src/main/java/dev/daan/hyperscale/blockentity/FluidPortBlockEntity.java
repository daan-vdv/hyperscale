package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.block.FluidPortBlock;
import dev.daan.hyperscale.block.PortMode;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidPortBlockEntity extends BlockEntity {
    public static final int CAPACITY_MB = 64_000;

    private final FluidTank tank = new FluidTank(CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            FluidPortBlockEntity.this.sync();
        }
    };

    private final IFluidHandler inputOnly = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int t) { return tank.getFluidInTank(t); }
        @Override public int getTankCapacity(int t) { return tank.getTankCapacity(t); }
        @Override public boolean isFluidValid(int t, FluidStack stack) { return tank.isFluidValid(t, stack); }
        @Override public int fill(FluidStack resource, FluidAction action) { return tank.fill(resource, action); }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return FluidStack.EMPTY; }
    };

    private final IFluidHandler outputOnly = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int t) { return tank.getFluidInTank(t); }
        @Override public int getTankCapacity(int t) { return tank.getTankCapacity(t); }
        @Override public boolean isFluidValid(int t, FluidStack stack) { return false; }
        @Override public int fill(FluidStack resource, FluidAction action) { return 0; }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return tank.drain(resource, action); }
        @Override public FluidStack drain(int maxDrain, FluidAction action) { return tank.drain(maxDrain, action); }
    };

    public FluidPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PORT.get(), pos, state);
    }

    public FluidTank tank() {
        return this.tank;
    }

    public IFluidHandler capability() {
        return this.getBlockState().getValue(FluidPortBlock.MODE) == PortMode.INPUT
                ? this.inputOnly
                : this.outputOnly;
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
        CompoundTag tankTag = new CompoundTag();
        this.tank.writeToNBT(registries, tankTag);
        tag.put("Tank", tankTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
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
