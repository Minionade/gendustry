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

public class ProteinRecipe extends ProcessorRecipe {
	private final Ingredient ingredient;

	public ProteinRecipe(ResourceLocation id, Ingredient ingredient, int amount) {
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
		return GRecipeTypes.PROTEIN.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return GRecipeTypes.PROTEIN.type();
	}

	public static class Serializer implements RecipeSerializer<ProteinRecipe> {
		private static final MapCodec<ProteinRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(ProteinRecipe::getId),
			Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ProteinRecipe::getIngredient),
			Codec.INT.fieldOf("amount").forGetter(ProteinRecipe::getAmount)
		).apply(instance, ProteinRecipe::new));
		private static final StreamCodec<RegistryFriendlyByteBuf, ProteinRecipe> STREAM_CODEC = StreamCodec.of(
			(buffer, recipe) -> {
				ResourceLocation.STREAM_CODEC.encode(buffer, recipe.getId());
				Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, recipe.ingredient);
				ByteBufCodecs.VAR_INT.encode(buffer, recipe.amount);
			},
			buffer -> new ProteinRecipe(
				ResourceLocation.STREAM_CODEC.decode(buffer),
				Ingredient.CONTENTS_STREAM_CODEC.decode(buffer),
				ByteBufCodecs.VAR_INT.decode(buffer)
			)
		);

		@Override
		public MapCodec<ProteinRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, ProteinRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
