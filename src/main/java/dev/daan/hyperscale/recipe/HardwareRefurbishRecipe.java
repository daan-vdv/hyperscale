package dev.daan.hyperscale.recipe;

import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * Shapeless refurbish recipe: any damaged/failed CPU, RAM, GPU, or Storage Drive + 1 Redstone + 1 Gold Ingot -> 100% Health.
 */
public class HardwareRefurbishRecipe extends CustomRecipe {
    public HardwareRefurbishRecipe(CraftingBookCategory category) {
        super(category);
    }

    private static boolean isRepairableHardware(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        boolean isHardware = item instanceof CpuItem
                || item instanceof RamItem
                || item instanceof GpuItem
                || item instanceof StorageDriveItem;
        return isHardware && HardwareHealth.get(stack) < HardwareHealth.MAX_HEALTH;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        int hardware = 0;
        int redstone = 0;
        int gold = 0;
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (isRepairableHardware(stack)) {
                hardware++;
            } else if (stack.is(Items.REDSTONE)) {
                redstone++;
            } else if (stack.is(Items.GOLD_INGOT)) {
                gold++;
            } else {
                return false;
            }
        }
        return hardware == 1 && redstone == 1 && gold == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (isRepairableHardware(stack)) {
                ItemStack repaired = stack.copyWithCount(1);
                HardwareHealth.repair(repaired);
                return repaired;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.HARDWARE_REFURBISH.get();
    }
}
