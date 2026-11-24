package dev.daan.hyperscale.compat.jei;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.recipe.LithographyRecipe;
import dev.daan.hyperscale.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

public final class LithographyCategory implements IRecipeCategory<LithographyRecipe> {
    public static final RecipeType<LithographyRecipe> RECIPE_TYPE =
            RecipeType.create(Hyperscale.MOD_ID, "lithography", LithographyRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public LithographyCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(164, 68);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.LITHOGRAPHY_ETCHER.get()));
    }

    @Override
    public RecipeType<LithographyRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hyperscale.lithography_etcher");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, LithographyRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.CATALYST, 12, 8)
                .addIngredients(recipe.mask());
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 32)
                .addIngredients(recipe.wafer());
        builder.addSlot(RecipeIngredientRole.INPUT, 36, 32)
                .addItemStacks(withCount(recipe.catalyst(), recipe.catalystCount()));
        if (recipe.waterMb() > 0) {
            builder.addSlot(RecipeIngredientRole.INPUT, 60, 32)
                    .addIngredient(NeoForgeTypes.FLUID_STACK, new FluidStack(Fluids.WATER, recipe.waterMb()))
                    .setFluidRenderer(Math.max(1000, recipe.waterMb()), false, 16, 16);
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 130, 24)
                .addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(LithographyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, 164, 68, 0xFF101820);
        graphics.fill(2, 2, 162, 66, 0xFF173042);

        // Mask slot with purple border
        graphics.fill(11, 7, 29, 25, 0xFFC084FC);
        graphics.fill(12, 8, 28, 24, 0xFF373737);

        drawSlotBg(graphics, 12, 32);
        drawSlotBg(graphics, 36, 32);
        if (recipe.waterMb() > 0) {
            drawSlotBg(graphics, 60, 32);
        }
        drawSlotBg(graphics, 130, 24);

        // Arrow bar
        graphics.fill(86, 30, 118, 34, 0xFFC084FC);
        graphics.fill(114, 27, 119, 37, 0xFFC084FC);

        var font = Minecraft.getInstance().font;
        graphics.drawString(font, "Mask (reusable)", 34, 12, 0xC084FC, false);
        String stats = recipe.waterMb() > 0
                ? String.format(Locale.ROOT, "%,d FE · %.1fs · %d mB", recipe.energy(), recipe.ticks() / 20.0D, recipe.waterMb())
                : String.format(Locale.ROOT, "%,d FE · %.1fs", recipe.energy(), recipe.ticks() / 20.0D);
        graphics.drawString(font, stats, 10, 54, 0x94A3B8, false);
    }

    private static void drawSlotBg(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
        graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
    }

    private static List<ItemStack> withCount(Ingredient ingredient, int count) {
        return Arrays.stream(ingredient.getItems())
                .map(s -> s.copyWithCount(Math.max(1, count)))
                .toList();
    }
}
