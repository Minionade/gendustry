package thedarkcolour.gendustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import thedarkcolour.gendustry.registry.GRecipeTypes;

public class MutagenRecipe extends ProcessorRecipe {
	private final Ingredient ingredient;

	public MutagenRecipe(ResourceLocation id, Ingredient ingredient, int amount) {
		super(id, amount);
		this.ingredient = ingredient;
	}

	public Ingredient getIngredient() {
		return this.ingredient;
	}

	@Override
	public boolean isIngredient(ItemStack stack) {
		return this.ingredient.test(stack);
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return GRecipeTypes.MUTAGEN.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return GRecipeTypes.MUTAGEN.type();
	}

	public static class Serializer implements RecipeSerializer<MutagenRecipe> {
		private static final MapCodec<MutagenRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(MutagenRecipe::getId),
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MutagenRecipe::getIngredient),
			Codec.INT.fieldOf("amount").forGetter(MutagenRecipe::getAmount)
		).apply(instance, MutagenRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, MutagenRecipe> STREAM_CODEC = StreamCodec.of(
			(buffer, recipe) -> {
				ResourceLocation.STREAM_CODEC.encode(buffer, recipe.getId());
				Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
				ByteBufCodecs.VAR_INT.encode(buffer, recipe.amount);
			},
			buffer -> new MutagenRecipe(
				ResourceLocation.STREAM_CODEC.decode(buffer),
				Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
				ByteBufCodecs.VAR_INT.decode(buffer)
			)
		);

		@Override
		public MapCodec<MutagenRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, MutagenRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
