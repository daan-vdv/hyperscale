package dev.daan.hyperscale.blockentity;

import java.util.EnumMap;

import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

public class ProbeBlockEntity extends InventoryBlockEntity {
    private static final int PUMP_COOLDOWN = 3;

    private final EnumMap<Direction, ProbeItemHandler> sidedHandlers = new EnumMap<>(Direction.class);
    private final ProbeItemHandler nullSideHandler = new ProbeItemHandler(this, this.items, null);

    private BlockPos compiler;
    private int cooldown;

    public ProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROBE.get(), pos, state, 9);
        for (Direction dir : Direction.values()) {
            this.sidedHandlers.put(dir, new ProbeItemHandler(this, this.items, dir));
        }
    }

    public IItemHandler capability(Direction side) {
        return side == null ? this.nullSideHandler : this.sidedHandlers.get(side);
    }

    @Override
    protected void onItemsChanged(int slot) {
        this.setChanged();
    }

    public ProbeMode mode() {
        return this.getBlockState().getBlock() instanceof ProbeBlock probe ? probe.mode() : ProbeMode.INPUT;
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
        return side == this.getBlockState().getValue(ProbeBlock.FACING).getOpposite();
    }

    public boolean countsAsInput(Direction side) {
        return this.mode() == ProbeMode.INPUT && this.facesInterior(side);
    }

    public boolean countsAsOutput(Direction side) {
        return this.mode() == ProbeMode.OUTPUT && this.facesInterior(side);
    }

    public boolean isSampling() {
        return this.compiler != null && this.level != null && this.level.isLoaded(this.compiler)
                && this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder
                && recorder.isSampling();
    }

    public void notifyTransfer(ItemStack moved) {
        if (this.compiler == null || this.level == null || moved.isEmpty() || !this.level.isLoaded(this.compiler)) {
            return;
        }
        if (this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) {
            recorder.recordTransfer(this.mode() == ProbeMode.INPUT, moved);
        }
    }

    public void notifyRemoved() {
        if (this.compiler != null && this.level != null && !this.level.isClientSide
                && this.level.isLoaded(this.compiler)
                && this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) {
            recorder.unbindProbe(this.worldPosition);
        }
    }

    public static void serverTick(ProbeBlockEntity probe) {
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
        Direction facing = this.getBlockState().getValue(ProbeBlock.FACING);
        Direction back = facing.getOpposite();
        BlockPos neighbor = this.worldPosition.relative(back);
        if (!this.level.isLoaded(neighbor) || this.level.getBlockState(neighbor).getBlock() instanceof ProbeBlock) {
            return;
        }
        IItemHandler target = this.level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, facing);
        if (target == null) {
            target = this.level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, null);
        }
        if (target == null) {
            return;
        }
        if (!recorder.isClaimInventory(neighbor)) {
            return;
        }
        if (this.mode() == ProbeMode.INPUT) {
            this.pushInto(target);
        } else {
            this.pullFrom(target);
        }
    }

    private void pushInto(IItemHandler target) {
        for (int slot = 0; slot < this.items.getSlots(); slot++) {
            ItemStack simulated = this.items.extractItem(slot, 1, true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack preview = ItemHandlerHelper.insertItem(target, simulated, true);
            int accepted = simulated.getCount() - preview.getCount();
            if (accepted <= 0) {
                continue;
            }
            ItemStack extracted = this.items.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(target, extracted, false);
            int moved = extracted.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) {
                ItemHandlerHelper.insertItem(this.items, remainder, false);
            }
            if (moved > 0) {
                this.notifyTransfer(extracted.copyWithCount(moved));
            }
            return;
        }
    }

    private void pullFrom(IItemHandler target) {
        for (int slot = 0; slot < target.getSlots(); slot++) {
            ItemStack simulated = target.extractItem(slot, 1, true);
            if (simulated.isEmpty()) {
                continue;
            }
            ItemStack preview = ItemHandlerHelper.insertItem(this.items, simulated, true);
            int accepted = simulated.getCount() - preview.getCount();
            if (accepted <= 0) {
                continue;
            }
            // Extract only what our probe is guaranteed to accept so output-only slots never void items.
            ItemStack extracted = target.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack remainder = ItemHandlerHelper.insertItem(this.items, extracted, false);
            int moved = extracted.getCount() - remainder.getCount();
            if (moved > 0) {
                this.notifyTransfer(extracted.copyWithCount(moved));
            }
            return;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (this.compiler != null) {
            tag.putLong("Compiler", this.compiler.asLong());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.compiler = tag.contains("Compiler") ? BlockPos.of(tag.getLong("Compiler")) : null;
    }
}
