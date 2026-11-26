package dev.daan.hyperscale.compat.guideme;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.recipe.LithographyRecipe;
import dev.daan.hyperscale.recipe.WaferPressRecipe;
import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModRecipes;
import guideme.Guide;
import guideme.GuideItemSettings;
import guideme.Guides;
import guideme.compiler.tags.RecipeTypeMappingSupplier;
import guideme.document.block.LytSlotGrid;
import guideme.document.block.recipes.LytStandardRecipeBox;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;

/**
 * Optional GuideME integration (AE2's in-game guidebook engine).
 * Only loaded when GuideME is present at runtime.
 */
public final class HyperscaleGuideCompat {
    public static final ResourceLocation GUIDE_ID =
            ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "guide");

    private HyperscaleGuideCompat() {
    }

    public static void registerGuide() {
        GuideItemSettings itemSettings = new GuideItemSettings(
                Optional.of(Component.translatable("item.hyperscale.guide")),
                List.of(Component.translatable("tooltip.hyperscale.guide").withStyle(ChatFormatting.GRAY)),
                Optional.empty()
        );

        Guide.builder(GUIDE_ID)
                .folder("guide")
                .itemSettings(itemSettings)
                .extension(RecipeTypeMappingSupplier.EXTENSION_POINT, HyperscaleGuideCompat::registerRecipeMappings)
                .build();
    }

    public static ItemStack createGuideItem() {
        return Guides.createGuideItem(GUIDE_ID);
    }

    private static void registerRecipeMappings(RecipeTypeMappingSupplier.RecipeTypeMappings mappings) {
        mappings.add(ModRecipes.WAFER_PRESS_TYPE.get(), HyperscaleGuideCompat::buildWaferPressBox);
        mappings.add(ModRecipes.LITHOGRAPHY_TYPE.get(), HyperscaleGuideCompat::buildLithographyBox);
    }

    private static LytStandardRecipeBox<WaferPressRecipe> buildWaferPressBox(RecipeHolder<WaferPressRecipe> holder) {
        WaferPressRecipe recipe = holder.value();
        List<Ingredient> inputs = new ArrayList<>();
        inputs.add(withCount(recipe.base(), recipe.baseCount()));
        recipe.dopant().ifPresent(dopant -> inputs.add(withCount(dopant, recipe.dopantCount())));

        return LytStandardRecipeBox.builder()
                .icon(ModItems.WAFER_PRESS.get())
                .title("Wafer Press")
                .input(LytSlotGrid.row(inputs, true))
                .output(recipe.result().copy())
                .build(holder);
    }

    private static LytStandardRecipeBox<LithographyRecipe> buildLithographyBox(RecipeHolder<LithographyRecipe> holder) {
        LithographyRecipe recipe = holder.value();
        List<Ingredient> inputs = new ArrayList<>();
        inputs.add(recipe.mask());
        inputs.add(recipe.wafer());
        inputs.add(withCount(recipe.catalyst(), recipe.catalystCount()));
        if (recipe.waterMb() > 0) {
            inputs.add(Ingredient.of(Items.WATER_BUCKET));
        }

        String title = recipe.waterMb() > 0
                ? "Lithography Etcher (" + recipe.waterMb() + " mB Water)"
                : "Lithography Etcher";

        return LytStandardRecipeBox.builder()
                .icon(ModItems.LITHOGRAPHY_ETCHER.get())
                .title(title)
                .input(LytSlotGrid.row(inputs, true))
                .output(recipe.result().copy())
                .build(holder);
    }

    private static Ingredient withCount(Ingredient ingredient, int count) {
        if (count <= 1) {
            return ingredient;
        }
        ItemStack[] stacks = Arrays.stream(ingredient.getItems())
                .map(stack -> stack.copyWithCount(count))
                .toArray(ItemStack[]::new);
        return stacks.length > 0 ? Ingredient.of(stacks) : ingredient;
    }
}
