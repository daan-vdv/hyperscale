package dev.daan.hyperscale.recipe;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModRecipes;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record WaferPressRecipe(
        Ingredient base,
        int baseCount,
        Optional<Ingredient> dopant,
        int dopantCount,
        ItemStack result,
        int ticks,
        int energy
) implements Recipe<WaferPressRecipe.Input> {

    public record Input(ItemStack base, ItemStack dopant) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return index == 0 ? this.base : this.dopant;
        }

        @Override
        public int size() {
            return 2;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        if (!this.base.test(input.base()) || input.base().getCount() < this.baseCount) {
            return false;
        }
        if (this.dopant.isPresent()) {
            return this.dopant.get().test(input.dopant()) && input.dopant().getCount() >= this.dopantCount;
        }
        return true;
    }

    @Override
    public ItemStack assemble(Input input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public ItemStack getToastSymbol() {
        return new ItemStack(ModItems.WAFER_PRESS.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.WAFER_PRESS.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.WAFER_PRESS_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<WaferPressRecipe> {
        public static final MapCodec<WaferPressRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("base").forGetter(WaferPressRecipe::base),
                Codec.INT.optionalFieldOf("base_count", 1).forGetter(WaferPressRecipe::baseCount),
                Ingredient.CODEC_NONEMPTY.optionalFieldOf("dopant").forGetter(WaferPressRecipe::dopant),
                Codec.INT.optionalFieldOf("dopant_count", 1).forGetter(WaferPressRecipe::dopantCount),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(WaferPressRecipe::result),
                Codec.INT.optionalFieldOf("ticks", 80).forGetter(WaferPressRecipe::ticks),
                Codec.INT.optionalFieldOf("energy", 3200).forGetter(WaferPressRecipe::energy)
        ).apply(inst, WaferPressRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, WaferPressRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public WaferPressRecipe decode(RegistryFriendlyByteBuf buf) {
                Ingredient base = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                int baseCount = ByteBufCodecs.VAR_INT.decode(buf);
                Optional<Ingredient> dopant = ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).decode(buf);
                int dopantCount = ByteBufCodecs.VAR_INT.decode(buf);
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                int ticks = ByteBufCodecs.VAR_INT.decode(buf);
                int energy = ByteBufCodecs.VAR_INT.decode(buf);
                return new WaferPressRecipe(base, baseCount, dopant, dopantCount, result, ticks, energy);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, WaferPressRecipe value) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.base());
                ByteBufCodecs.VAR_INT.encode(buf, value.baseCount());
                ByteBufCodecs.optional(Ingredient.CONTENTS_STREAM_CODEC).encode(buf, value.dopant());
                ByteBufCodecs.VAR_INT.encode(buf, value.dopantCount());
                ItemStack.STREAM_CODEC.encode(buf, value.result());
                ByteBufCodecs.VAR_INT.encode(buf, value.ticks());
                ByteBufCodecs.VAR_INT.encode(buf, value.energy());
            }
        };

        @Override
        public MapCodec<WaferPressRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, WaferPressRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
