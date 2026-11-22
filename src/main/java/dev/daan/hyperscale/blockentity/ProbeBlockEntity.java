package dev.daan.hyperscale.blockentity;

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
    private BlockPos compiler;
    private int cooldown;

    public ProbeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PROBE.get(), pos, state, 9);
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

    public void notifyTransfer(ItemStack moved) {
        if (this.compiler == null || this.level == null || moved.isEmpty()) {
            return;
        }
        if (this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) {
            recorder.recordTransfer(this.mode() == ProbeMode.INPUT, moved);
        }
    }

    public void notifyRemoved() {
        if (this.compiler != null && this.level != null
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
        if (!(this.level.getBlockEntity(this.compiler) instanceof RecorderBlockEntity recorder) || !recorder.isSampling()) {
            return;
        }
        if (this.cooldown-- > 0) {
            return;
        }
        this.cooldown = 8;
        Direction facing = this.getBlockState().getValue(ProbeBlock.FACING);
        Direction back = facing.getOpposite();
        BlockPos neighbor = this.worldPosition.relative(back);
        if (this.level.getBlockState(neighbor).getBlock() instanceof ProbeBlock) {
            return;
        }
        IItemHandler target = this.level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, facing);
        if (target == null) {
            target = this.level.getCapability(Capabilities.ItemHandler.BLOCK, neighbor, null);
        }
        if (target == null) {
            return;
        }
        // Only move against claim inventories so stuffing outside the box cannot teach a recipe.
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
            if (preview.getCount() == simulated.getCount()) {
                continue;
            }
            ItemStack extracted = this.items.extractItem(slot, 1, false);
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
            if (preview.getCount() == simulated.getCount()) {
                continue;
            }
            ItemStack extracted = target.extractItem(slot, 1, false);
            ItemStack remainder = ItemHandlerHelper.insertItem(this.items, extracted, false);
            int moved = extracted.getCount() - remainder.getCount();
            if (!remainder.isEmpty()) {
                target.insertItem(slot, remainder, false);
            }
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
