package dev.daan.hyperscale.blockentity;

import java.util.Optional;

import dev.daan.hyperscale.recipe.WaferPressRecipe;
import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

public class WaferPressBlockEntity extends InventoryBlockEntity {
    public static final int SLOT_BASE = 0;
    public static final int SLOT_DOPANT = 1;
    public static final int SLOT_OUTPUT = 2;
    public static final int ENERGY_CAPACITY = 50_000;
    public static final int MAX_RECEIVE = 1_000;

    private int progress;
    private int maxProgress = 80;

    private final EnergyStorage energy = new EnergyStorage(ENERGY_CAPACITY, MAX_RECEIVE, ENERGY_CAPACITY) {
        @Override
        public int receiveEnergy(int toReceive, boolean simulate) {
            int accepted = super.receiveEnergy(toReceive, simulate);
            if (!simulate && accepted > 0) {
                WaferPressBlockEntity.this.setChanged();
            }
            return accepted;
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
        @Override public int getSlots() { return 3; }
        @Override public ItemStack getStackInSlot(int slot) { return items.getStackInSlot(slot); }
        @Override public int getSlotLimit(int slot) { return items.getSlotLimit(slot); }
        @Override public boolean isItemValid(int slot, ItemStack stack) { return slot != SLOT_OUTPUT; }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (slot == SLOT_OUTPUT || stack.isEmpty()) {
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
            return 4;
        }
    };

    public WaferPressBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WAFER_PRESS.get(), pos, state, 3);
    }

    @Override
    protected boolean isItemValid(int slot, ItemStack stack) {
        return slot != SLOT_OUTPUT;
    }

    public IItemHandler itemCapability() {
        return this.automationHandler;
    }

    public IEnergyStorage energyCapability() {
        return this.energyInput;
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

    public static void serverTick(WaferPressBlockEntity press) {
        press.tickServer();
    }

    private void tickServer() {
        if (this.level == null) {
            return;
        }
        WaferPressRecipe.Input input = new WaferPressRecipe.Input(
                this.items.getStackInSlot(SLOT_BASE),
                this.items.getStackInSlot(SLOT_DOPANT)
        );
        Optional<RecipeHolder<WaferPressRecipe>> match = this.level.getRecipeManager()
                .getRecipeFor(ModRecipes.WAFER_PRESS_TYPE.get(), input, this.level);
        if (match.isEmpty()) {
            if (this.progress > 0) {
                this.progress = 0;
                this.setChanged();
            }
            return;
        }

        WaferPressRecipe recipe = match.get().value();
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
            this.items.extractItem(SLOT_BASE, recipe.baseCount(), false);
            if (recipe.dopant().isPresent()) {
                this.items.extractItem(SLOT_DOPANT, recipe.dopantCount(), false);
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = Math.max(0, tag.getInt("Progress"));
        this.maxProgress = Math.max(1, tag.contains("MaxProgress") ? tag.getInt("MaxProgress") : 80);
        int stored = Math.max(0, Math.min(ENERGY_CAPACITY, tag.getInt("Energy")));
        while (this.energy.getEnergyStored() < stored) {
            int added = this.energy.receiveEnergy(stored - this.energy.getEnergyStored(), false);
            if (added <= 0) {
                break;
            }
        }
    }
}
