package dev.daan.hyperscale.blockentity;

import java.util.EnumMap;

import dev.daan.hyperscale.block.FluidProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class FluidProbeBlockEntity extends BlockEntity {
    public static final int CAPACITY_MB = 16_000;
    private static final int PUMP_COOLDOWN = 3;
    private static final int MAX_PUMP_MB = 1_000;

    private final FluidTank tank = new FluidTank(CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            FluidProbeBlockEntity.this.sync();
        }
    };

    private final EnumMap<Direction, IFluidHandler> sidedHandlers = new EnumMap<>(Direction.class);
    private final IFluidHandler nullSideHandler;

    private BlockPos compiler;
    private int cooldown;

    public FluidProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUID_PROBE.get(), pos, state);
        this.nullSideHandler = createHandler(null);
        for (Direction dir : Direction.values()) {
            this.sidedHandlers.put(dir, createHandler(dir));
        }
    }

    private IFluidHandler createHandler(Direction side) {
        return new IFluidHandler() {
            @Override
            public int getTanks() {
                return 1;
            }

            @Override
            public FluidStack getFluidInTank(int t) {
                return tank.getFluidInTank(t);
            }

            @Override
            public int getTankCapacity(int t) {
                return tank.getTankCapacity(t);
            }

            @Override
            public boolean isFluidValid(int t, FluidStack stack) {
                return tank.isFluidValid(t, stack);
            }

            @Override
            public int fill(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return 0;
                }
                boolean interior = facesInterior(side);
                if (interior) {
                    if (mode() != ProbeMode.OUTPUT || !isCompilerSampling()) {
                        return 0;
                    }
                    int filled = tank.fill(resource, action);
                    if (action.execute() && filled > 0) {
                        notifyFluidTransfer(resource.copyWithAmount(filled));
                    }
                    return filled;
                }
                if (isCompilerSampling()) {
                    return 0;
                }
                return mode() == ProbeMode.INPUT ? tank.fill(resource, action) : 0;
            }

            @Override
            public FluidStack drain(FluidStack resource, FluidAction action) {
                if (resource.isEmpty()) {
                    return FluidStack.EMPTY;
                }
                boolean interior = facesInterior(side);
                if (interior) {
                    if (mode() != ProbeMode.INPUT || !isCompilerSampling()) {
                        return FluidStack.EMPTY;
                    }
                    FluidStack drained = tank.drain(resource, action);
                    if (action.execute() && !drained.isEmpty()) {
                        notifyFluidTransfer(drained);
                    }
                    return drained;
                }
                if (isCompilerSampling()) {
                    return FluidStack.EMPTY;
                }
                return tank.drain(resource, action);
            }

            @Override
            public FluidStack drain(int maxDrain, FluidAction action) {
                if (maxDrain <= 0) {
                    return FluidStack.EMPTY;
                }
                boolean interior = facesInterior(side);
                if (interior) {
                    if (mode() != ProbeMode.INPUT || !isCompilerSampling()) {
                        return FluidStack.EMPTY;
                    }
                    FluidStack drained = tank.drain(maxDrain, action);
                    if (action.execute() && !drained.isEmpty()) {
                        notifyFluidTransfer(drained);
                    }
                    return drained;
                }
                if (isCompilerSampling()) {
                    return FluidStack.EMPTY;
                }
                return tank.drain(maxDrain, action);
            }
        };
    }

    public FluidTank tank() {
        return this.tank;
    }

    public IFluidHandler capability(Direction side) {
        return side == null ? this.nullSideHandler : this.sidedHandlers.get(side);
    }

    public ProbeMode mode() {
        return this.getBlockState().getBlock() instanceof FluidProbeBlock probe ? probe.mode() : ProbeMode.INPUT;
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

    public void resetCooldown() {
        this.cooldown = 0;
    }

    public boolean facesInterior(Direction side) {
        if (side == null) {
            return false;
        }
        return side == this.getBlockState().getValue(FluidProbeBlock.FACING).getOpposite();
    }

    private boolean isCompilerSampling() {
        return this.compiler != null && this.level != null && this.level.isLoaded(this.compiler)
                && this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder
                && recorder.isSampling();
    }

    public void notifyFluidTransfer(FluidStack moved) {
        if (this.compiler == null || this.level == null || moved.isEmpty() || !this.level.isLoaded(this.compiler)) {
            return;
        }
        if (this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) {
            recorder.recordFluidTransfer(this.mode() == ProbeMode.INPUT, moved);
        }
    }

    public void notifyRemoved() {
        if (this.compiler != null && this.level != null && !this.level.isClientSide
                && this.level.isLoaded(this.compiler)
                && this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) {
            recorder.unbindFluidProbe(this.worldPosition);
        }
    }

    public static void serverTick(FluidProbeBlockEntity probe) {
        probe.pump();
    }

    private void pump() {
        if (this.level == null || this.compiler == null) {
            return;
        }
        if (this.cooldown > 0) {
            this.cooldown--;
            return;
        }
        if (!this.level.isLoaded(this.compiler)
                || !(this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder)
                || !recorder.isSampling()) {
            return;
        }
        this.cooldown = PUMP_COOLDOWN;
        Direction facing = this.getBlockState().getValue(FluidProbeBlock.FACING);
        Direction back = facing.getOpposite();
        BlockPos neighbor = this.worldPosition.relative(back);
        if (!this.level.isLoaded(neighbor) || this.level.getBlockState(neighbor).getBlock() instanceof FluidProbeBlock) {
            return;
        }
        if (!recorder.isClaimFluidBlock(neighbor)) {
            return;
        }
        IFluidHandler target = this.level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, facing);
        if (target == null) {
            target = this.level.getCapability(Capabilities.FluidHandler.BLOCK, neighbor, null);
        }
        if (target == null) {
            return;
        }
        if (this.mode() == ProbeMode.INPUT) {
            this.pushInto(target);
        } else {
            this.pullFrom(target);
        }
    }

    private void pushInto(IFluidHandler target) {
        FluidStack available = this.tank.getFluid();
        if (available.isEmpty()) {
            return;
        }
        FluidStack toPush = available.copyWithAmount(Math.min(available.getAmount(), MAX_PUMP_MB));
        int accepted = target.fill(toPush, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack drained = this.tank.drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        int filled = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled < drained.getAmount()) {
            this.tank.fill(drained.copyWithAmount(drained.getAmount() - filled), IFluidHandler.FluidAction.EXECUTE);
        }
        if (filled > 0) {
            this.notifyFluidTransfer(drained.copyWithAmount(filled));
        }
    }

    private void pullFrom(IFluidHandler target) {
        int free = this.tank.getSpace();
        if (free <= 0) {
            return;
        }
        FluidStack simulated = target.drain(Math.min(free, MAX_PUMP_MB), IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty()) {
            return;
        }
        int accepted = this.tank.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return;
        }
        FluidStack drained = target.drain(simulated.copyWithAmount(accepted), IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return;
        }
        int filled = this.tank.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            this.notifyFluidTransfer(drained.copyWithAmount(filled));
        }
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
        if (this.compiler != null) {
            tag.putLong("Compiler", this.compiler.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
        this.compiler = tag.contains("Compiler") ? BlockPos.of(tag.getLong("Compiler")) : null;
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
