package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.recipe.HardwareRefurbishRecipe;
import dev.daan.hyperscale.recipe.LithographyRecipe;
import dev.daan.hyperscale.recipe.WaferPressRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Hyperscale.MOD_ID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Hyperscale.MOD_ID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<WaferPressRecipe>> WAFER_PRESS_TYPE =
            RECIPE_TYPES.register("wafer_press", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "wafer_press")));
    public static final DeferredHolder<RecipeType<?>, RecipeType<LithographyRecipe>> LITHOGRAPHY_TYPE =
            RECIPE_TYPES.register("lithography", () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "lithography")));

    public static final DeferredHolder<RecipeSerializer<?>, SimpleCraftingRecipeSerializer<HardwareRefurbishRecipe>> HARDWARE_REFURBISH =
            RECIPE_SERIALIZERS.register("hardware_refurbish", () -> new SimpleCraftingRecipeSerializer<>(HardwareRefurbishRecipe::new));
    public static final DeferredHolder<RecipeSerializer<?>, WaferPressRecipe.Serializer> WAFER_PRESS =
            RECIPE_SERIALIZERS.register("wafer_press", WaferPressRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, LithographyRecipe.Serializer> LITHOGRAPHY =
            RECIPE_SERIALIZERS.register("lithography", LithographyRecipe.Serializer::new);

    private ModRecipes() {
    }
}
