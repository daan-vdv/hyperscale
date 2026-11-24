package dev.daan.hyperscale.compat.jei;

import java.util.List;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModRecipes;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public class HyperscaleJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new WaferPressCategory(guiHelper),
                new LithographyCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModItems.WAFER_PRESS.get()), WaferPressCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(new ItemStack(ModItems.LITHOGRAPHY_ETCHER.get()), LithographyCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level != null) {
            var recipeManager = level.getRecipeManager();
            registration.addRecipes(
                    WaferPressCategory.RECIPE_TYPE,
                    recipeManager.getAllRecipesFor(ModRecipes.WAFER_PRESS_TYPE.get()).stream()
                            .map(RecipeHolder::value)
                            .toList()
            );
            registration.addRecipes(
                    LithographyCategory.RECIPE_TYPE,
                    recipeManager.getAllRecipesFor(ModRecipes.LITHOGRAPHY_TYPE.get()).stream()
                            .map(RecipeHolder::value)
                            .toList()
            );
        }
        registration.addIngredientInfo(
                List.of(new ItemStack(ModItems.RECORDER.get()), new ItemStack(ModItems.RECORDER_CASING.get())),
                VanillaTypes.ITEM_STACK,
                Component.literal("Seal any working machine setup inside Compiler Casing with Recorder Input & Output probes, insert an Empty Image Card, and complete 3 calibration runs to compile an Image Card.")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.IMAGE_REGISTRY.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("Stores up to 9 compiled Image Cards for your datacenter hall. Connect to a Network Switch directly or via Hybrid Cables, then deploy containers from the Interface.")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SWITCH.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("The Network Switch is your datacenter's control plane. Supply FE power directly or via Hybrid Cables, and route cables to racks, drives, image registries, and ports.")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.CABLE.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("Hybrid Cables bridge datacenter blocks and carry FE power into your Switch & Backup Generators — as long as the cable run is plugged into a Network Switch.")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.DRIVE_BAY.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("Insert HDDs, SATA SSDs, or NVMe SSDs for persistent AE2-style item storage. Switch between JBOD, RAID 1 (mirror), and RAID 5 (parity) to protect against hardware wear, or Salvage failed drives.")
        );
        registration.addIngredientInfo(
                List.of(new ItemStack(ModItems.WAFER_PRESS.get()), new ItemStack(ModItems.LITHOGRAPHY_ETCHER.get())),
                VanillaTypes.ITEM_STACK,
                Component.literal("Semiconductor Fab pipeline: Smelt Quartz into Silicon (#c:silicon), press Silicon Wafers / PCBs / Doped & Quantum Wafers in the Wafer Press, then etch Logic, Memory, NAND, Tensor, and Quantum Dies in the Lithography Etcher using reusable Photomasks (plus Water for EUV chips). You can even encase your Fab in Compiler Casing to virtualize chip manufacturing!")
        );
        registration.addIngredientInfo(
                List.of(new ItemStack(ModItems.SERVER_FAN.get()), new ItemStack(ModItems.WATERCOOLER_CONTROLLER.get())),
                VanillaTypes.ITEM_STACK,
                Component.literal("Connect Server Fans (+600W) or Watercooler Controllers (+4,000W when supplied with Water via pipe/bucket; adjacent Server Fans form a closed-loop radiator saving up to 75% water). Staying under 50°C with liquid cooling unlocks Cold Turbo (+15%) and Overclock mode; PUE <= 1.20 grants -10% FE draw!")
        );
        registration.addIngredientInfo(
                new ItemStack(ModItems.SERVER_RACK.get()),
                VanillaTypes.ITEM_STACK,
                Component.literal("Holds 1 Server, 1 CPU, 1 RAM, and 1 optional GPU. Active hardware wears slowly over time (faster when overheating); craft any damaged component with 1 Redstone + 1 Gold Ingot to refurbish it to 100%.")
        );
    }
}
