package dev.daan.hyperscale.compat.jei;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.recipe.WaferPressRecipe;
import dev.daan.hyperscale.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

public final class WaferPressCategory implements IRecipeCategory<WaferPressRecipe> {
    public static final RecipeType<WaferPressRecipe> RECIPE_TYPE =
            RecipeType.create(Hyperscale.MOD_ID, "wafer_press", WaferPressRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public WaferPressCategory(IGuiHelper guiHelper) {
        this.background = guiHelper.createBlankDrawable(150, 56);
        this.icon = guiHelper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModItems.WAFER_PRESS.get()));
    }

    @Override
    public RecipeType<WaferPressRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.hyperscale.wafer_press");
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
    public void setRecipe(IRecipeLayoutBuilder builder, WaferPressRecipe recipe, IFocusGroup focuses) {
        builder.addSlot(RecipeIngredientRole.INPUT, 12, 16)
                .addItemStacks(withCount(recipe.base(), recipe.baseCount()));
        if (recipe.dopant().isPresent()) {
            builder.addSlot(RecipeIngredientRole.INPUT, 38, 16)
                    .addItemStacks(withCount(recipe.dopant().get(), recipe.dopantCount()));
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 114, 16)
                .addItemStack(recipe.result().copy());
    }

    @Override
    public void draw(WaferPressRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics graphics, double mouseX, double mouseY) {
        graphics.fill(0, 0, 150, 56, 0xFF101820);
        graphics.fill(2, 2, 148, 54, 0xFF173042);
        drawSlotBg(graphics, 12, 16);
        if (recipe.dopant().isPresent()) {
            drawSlotBg(graphics, 38, 16);
        }
        drawSlotBg(graphics, 114, 16);

        // Progress arrow bar
        graphics.fill(66, 22, 102, 26, 0xFF38BDF8);
        graphics.fill(98, 19, 103, 29, 0xFF38BDF8);

        var font = Minecraft.getInstance().font;
        String stats = String.format(Locale.ROOT, "%,d FE · %.1fs", recipe.energy(), recipe.ticks() / 20.0D);
        graphics.drawString(font, stats, 10, 41, 0x94A3B8, false);
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
