package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Active liquid cooling controller with a 16,000 mB coolant tank.
 * Consumes water proportional to thermal load when the hall is warm.
 * Placing Server Fans directly adjacent acts as a closed-loop radiator, reducing water consumption
 * by 25% per fan (up to 75% savings with 3+ adjacent fans).
 */
public class WatercoolerBlockEntity extends BlockEntity {
    public static final int CAPACITY_MB = 16_000;

    private final FluidTank tank = new FluidTank(CAPACITY_MB, fluid -> fluid.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            WatercoolerBlockEntity.this.sync();
        }
    };

    public WatercoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATERCOOLER.get(), pos, state);
    }

    public FluidTank tank() {
        return this.tank;
    }

    public IFluidHandler capability() {
        return this.tank;
    }

    public int coolantAmount() {
        return this.tank.getFluidAmount();
    }

    public boolean hasCoolant() {
        return this.tank.getFluidAmount() > 0;
    }

    /**
     * Attempts to consume the requested coolant (in mB). Returns true if enough coolant was available.
     */
    public boolean consumeCoolant(int amountMb) {
        if (amountMb <= 0) {
            return this.hasCoolant();
        }
        if (this.tank.getFluidAmount() < amountMb) {
            return false;
        }
        FluidStack drained = this.tank.drain(amountMb, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount() >= amountMb;
    }

    public static int adjacentRadiatorFans(Level level, BlockPos pos) {
        int fans = 0;
        for (Direction dir : Direction.values()) {
            BlockPos next = pos.relative(dir);
            if (level.isLoaded(next) && level.getBlockState(next).is(ModBlocks.SERVER_FAN.get())) {
                fans++;
            }
        }
        return fans;
    }

    /**
     * Effective coolant consumption multiplier based on adjacent Server Fans acting as closed-loop radiators.
     * 0 fans = 1.0x, 1 fan = 0.75x, 2 fans = 0.50x, 3+ fans = 0.25x.
     */
    public static double radiatorConsumptionFactor(Level level, BlockPos pos) {
        int fans = Math.min(3, adjacentRadiatorFans(level, pos));
        return 1.0D - (fans * 0.25D);
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
