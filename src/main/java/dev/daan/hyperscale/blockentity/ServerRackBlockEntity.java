package dev.daan.hyperscale.blockentity;

import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.CpuSpec;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.GpuSpec;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.RamSpec;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class ServerRackBlockEntity extends InventoryBlockEntity {
    public static final int SLOT_SERVER = 0;
    public static final int SLOT_CPU = 1;
    public static final int SLOT_RAM = 2;
    public static final int SLOT_GPU = 3;

    public ServerRackBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SERVER_RACK.get(), pos, state, 4);
    }

    public boolean hasServer() {
        return this.items.getStackInSlot(SLOT_SERVER).is(ModItems.SERVER.get());
    }

    public boolean hasCpu() {
        return this.cpuSpec() != null;
    }

    public boolean hasRam() {
        return this.ramSpec() != null;
    }

    public boolean hasGpu() {
        return this.gpuSpec() != null;
    }

    public CpuSpec cpuSpec() {
        ItemStack stack = this.items.getStackInSlot(SLOT_CPU);
        return (stack.getItem() instanceof CpuItem cpu && !HardwareHealth.isFailed(stack)) ? cpu.spec() : null;
    }

    public RamSpec ramSpec() {
        ItemStack stack = this.items.getStackInSlot(SLOT_RAM);
        return (stack.getItem() instanceof RamItem ram && !HardwareHealth.isFailed(stack)) ? ram.spec() : null;
    }

    public GpuSpec gpuSpec() {
        ItemStack stack = this.items.getStackInSlot(SLOT_GPU);
        return (stack.getItem() instanceof GpuItem gpu && !HardwareHealth.isFailed(stack)) ? gpu.spec() : null;
    }

    public boolean hasFailedComponent() {
        for (int slot = SLOT_CPU; slot <= SLOT_GPU; slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (!stack.isEmpty() && HardwareHealth.isFailed(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDegradedComponent() {
        for (int slot = SLOT_CPU; slot <= SLOT_GPU; slot++) {
            ItemStack stack = this.items.getStackInSlot(slot);
            if (!stack.isEmpty() && HardwareHealth.isDegraded(stack)) {
                return true;
            }
        }
        return false;
    }

    public boolean isReady() {
        return this.hasServer() && this.hasCpu() && this.hasRam();
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_SERVER -> stack.is(ModItems.SERVER.get());
            case SLOT_CPU -> stack.getItem() instanceof CpuItem;
            case SLOT_RAM -> stack.getItem() instanceof RamItem;
            case SLOT_GPU -> stack.getItem() instanceof GpuItem;
            default -> false;
        };
    }
}
