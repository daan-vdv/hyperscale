package dev.daan.hyperscale.recipe;

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

public record LithographyRecipe(
        Ingredient mask,
        Ingredient wafer,
        Ingredient catalyst,
        int catalystCount,
        int waterMb,
        ItemStack result,
        int ticks,
        int energy
) implements Recipe<LithographyRecipe.Input> {

    public record Input(ItemStack mask, ItemStack wafer, ItemStack catalyst) implements RecipeInput {
        @Override
        public ItemStack getItem(int index) {
            return switch (index) {
                case 0 -> this.mask;
                case 1 -> this.wafer;
                default -> this.catalyst;
            };
        }

        @Override
        public int size() {
            return 3;
        }
    }

    @Override
    public boolean matches(Input input, Level level) {
        return this.mask.test(input.mask())
                && this.wafer.test(input.wafer())
                && this.catalyst.test(input.catalyst())
                && input.catalyst().getCount() >= this.catalystCount;
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
        return new ItemStack(ModItems.LITHOGRAPHY_ETCHER.get());
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.LITHOGRAPHY.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.LITHOGRAPHY_TYPE.get();
    }

    public static final class Serializer implements RecipeSerializer<LithographyRecipe> {
        public static final MapCodec<LithographyRecipe> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
                Ingredient.CODEC_NONEMPTY.fieldOf("mask").forGetter(LithographyRecipe::mask),
                Ingredient.CODEC_NONEMPTY.fieldOf("wafer").forGetter(LithographyRecipe::wafer),
                Ingredient.CODEC_NONEMPTY.fieldOf("catalyst").forGetter(LithographyRecipe::catalyst),
                Codec.INT.optionalFieldOf("catalyst_count", 1).forGetter(LithographyRecipe::catalystCount),
                Codec.INT.optionalFieldOf("water_mb", 0).forGetter(LithographyRecipe::waterMb),
                ItemStack.STRICT_CODEC.fieldOf("result").forGetter(LithographyRecipe::result),
                Codec.INT.optionalFieldOf("ticks", 100).forGetter(LithographyRecipe::ticks),
                Codec.INT.optionalFieldOf("energy", 5000).forGetter(LithographyRecipe::energy)
        ).apply(inst, LithographyRecipe::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, LithographyRecipe> STREAM_CODEC = new StreamCodec<>() {
            @Override
            public LithographyRecipe decode(RegistryFriendlyByteBuf buf) {
                Ingredient mask = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Ingredient wafer = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                Ingredient catalyst = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
                int catalystCount = ByteBufCodecs.VAR_INT.decode(buf);
                int waterMb = ByteBufCodecs.VAR_INT.decode(buf);
                ItemStack result = ItemStack.STREAM_CODEC.decode(buf);
                int ticks = ByteBufCodecs.VAR_INT.decode(buf);
                int energy = ByteBufCodecs.VAR_INT.decode(buf);
                return new LithographyRecipe(mask, wafer, catalyst, catalystCount, waterMb, result, ticks, energy);
            }

            @Override
            public void encode(RegistryFriendlyByteBuf buf, LithographyRecipe value) {
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.mask());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.wafer());
                Ingredient.CONTENTS_STREAM_CODEC.encode(buf, value.catalyst());
                ByteBufCodecs.VAR_INT.encode(buf, value.catalystCount());
                ByteBufCodecs.VAR_INT.encode(buf, value.waterMb());
                ItemStack.STREAM_CODEC.encode(buf, value.result());
                ByteBufCodecs.VAR_INT.encode(buf, value.ticks());
                ByteBufCodecs.VAR_INT.encode(buf, value.energy());
            }
        };

        @Override
        public MapCodec<LithographyRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LithographyRecipe> streamCodec() {
            return STREAM_CODEC;
        }
    }
}
