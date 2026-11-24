package dev.daan.hyperscale.blockentity;

import java.util.Optional;

import dev.daan.hyperscale.recipe.LithographyRecipe;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

public class LithographyEtcherBlockEntity extends InventoryBlockEntity {
    public static final int SLOT_MASK = 0;
    public static final int SLOT_WAFER = 1;
    public static final int SLOT_CATALYST = 2;
    public static final int SLOT_OUTPUT = 3;
    public static final int ENERGY_CAPACITY = 100_000;
    public static final int MAX_RECEIVE = 2_000;
    public static final int TANK_CAPACITY_MB = 8_000;

    private int progress;
    private int maxProgress = 100;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, ENERGY_CAPACITY) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate && accepted > 0) {
                LithographyEtcherBlockEntity.this.setChanged();
            }
            return accepted;
        }
    };

    private final FluidTank tank = new FluidTank(TANK_CAPACITY_MB, fluid -> fluid.is(Fluids.WATER)) {
        @Override
        protected void onContentsChanged() {
            LithographyEtcherBlockEntity.this.sync();
        }
    };

    private final IEnergyStorage energyInput = new IEnergyStorage() {
        @Override public int receiveEnergy(int toReceive, boolean simulate) { return energy.receiveEnergy(toReceive, simulate); }
        @Override public int extractEnergy(int toExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return energy.getEnergyStored(); }
        @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    private final IItemHandler automationHandler = new IItemHandler() {
        @Override public int getSlots() { return 4; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return LithographyEtcherBlockEntity.this.isItemValid(slot, stack);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_OUTPUT || stack.isEmpty() || !isItemValid(slot, stack)) {
                return stack;
            }
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != SLOT_OUTPUT) {
                return ItemStack.EMPTY;
            }
            return items.extractItem(slot, amount, simulate);
        }
    };

    public final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy.getEnergyStored() / 10;
                case 3 -> ENERGY_CAPACITY / 10;
                case 4 -> tank.getFluidAmount();
                case 5 -> TANK_CAPACITY_MB;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = Math.max(1, value);
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public LithographyEtcherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LITHOGRAPHY_ETCHER.get(), pos, state, 4);
    }

    public static boolean isPhotomask(ItemStack stack) {
        return stack.is(ModItems.MASK_LOGIC.get())
                || stack.is(ModItems.MASK_MEMORY.get())
                || stack.is(ModItems.MASK_STORAGE.get())
                || stack.is(ModItems.MASK_TENSOR.get());
    }

    public static boolean isWafer(ItemStack stack) {
        return stack.is(ModItems.SILICON_WAFER.get())
                || stack.is(ModItems.DOPED_WAFER.get())
                || stack.is(ModItems.QUANTUM_WAFER.get());
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_MASK -> isPhotomask(stack);
            case SLOT_WAFER -> isWafer(stack);
            case SLOT_CATALYST -> !isPhotomask(stack) && !isWafer(stack);
            default -> false;
        };
    }

    @Override
    protected int slotLimit(int slot) {
        return slot == SLOT_MASK ? 1 : super.slotLimit(slot);
    }

    public IItemHandler itemCapability() {
        return this.automationHandler;
    }

    public IEnergyStorage energyCapability() {
        return this.energyInput;
    }

    public FluidTank tank() {
        return this.tank;
    }

    public IFluidHandler fluidCapability() {
        return this.tank;
    }

    public int progress() {
        return this.progress;
    }

    public int maxProgress() {
        return this.maxProgress;
    }

    public int energyStored() {
        return this.energy.getEnergyStored();
    }

    public static void serverTick(LithographyEtcherBlockEntity etcher) {
        etcher.tickServer();
    }

    private void tickServer() {
        if (this.level == null) {
            return;
        }
        LithographyRecipe.Input input = new LithographyRecipe.Input(
                this.items.getStackInSlot(SLOT_MASK),
                this.items.getStackInSlot(SLOT_WAFER),
                this.items.getStackInSlot(SLOT_CATALYST)
        );
        Optional<RecipeHolder<LithographyRecipe>> match = this.level.getRecipeManager()
                .getRecipeFor(ModRecipes.LITHOGRAPHY_TYPE.get(), input, this.level);
        if (match.isEmpty()) {
            if (this.progress > 0) {
                this.progress = 0;
                this.setChanged();
            }
            return;
        }

        LithographyRecipe recipe = match.get().value();
        if (recipe.waterMb() > 0 && this.tank.getFluidAmount() < recipe.waterMb()) {
            return;
        }
        ItemStack result = recipe.result();
        if (!canAcceptOutput(result)) {
            return;
        }

        this.maxProgress = Math.max(1, recipe.ticks());
        int fePerTick = Math.max(1, (int) Math.ceil((double) recipe.energy() / this.maxProgress));
        if (this.energy.getEnergyStored() < fePerTick) {
            return;
        }

        this.energy.extractEnergy(fePerTick, false);
        this.progress++;
        this.setChanged();

        if (this.progress >= this.maxProgress) {
            // Photomask is reusable and NOT consumed!
            this.items.extractItem(SLOT_WAFER, 1, false);
            this.items.extractItem(SLOT_CATALYST, recipe.catalystCount(), false);
            if (recipe.waterMb() > 0) {
                this.tank.drain(recipe.waterMb(), IFluidHandler.FluidAction.EXECUTE);
            }
            insertOutput(result.copy());
            this.progress = 0;
            this.sync();
        }
    }

    private boolean canAcceptOutput(ItemStack result) {
        ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(current, result)
                && current.getCount() + result.getCount() <= current.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        ItemStack current = this.items.getStackInSlot(SLOT_OUTPUT);
        if (current.isEmpty()) {
            this.items.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            ItemStack merged = current.copy();
            merged.grow(result.getCount());
            this.items.setStackInSlot(SLOT_OUTPUT, merged);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", this.progress);
        tag.putInt("MaxProgress", this.maxProgress);
        tag.putInt("Energy", this.energy.getEnergyStored());
        CompoundTag tankTag = new CompoundTag();
        this.tank.writeToNBT(registries, tankTag);
        tag.put("Tank", tankTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = Math.max(0, tag.getInt("Progress"));
        this.maxProgress = Math.max(1, tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 100);
        int stored = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getInt("Energy")));
        while (this.energy.getEnergyStored() < stored) {
            int added = this.energy.receiveEnergy(stored - this.energy.getEnergyStored(), false);
            if (added <= 0) {
                break;
            }
        }
        if (tag.contains("Tank")) {
            this.tank.readFromNBT(registries, tag.getCompound("Tank"));
        }
    }
}
