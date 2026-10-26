package thedarkcolour.gendustry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;

import thedarkcolour.gendustry.registry.GRecipeTypes;

public class DnaRecipe extends ProcessorRecipe {
	private final ISpeciesType<?, ?> speciesType;
	private final ILifeStage stage;

	public DnaRecipe(ResourceLocation id, ISpeciesType<?, ?> speciesType, ILifeStage stage, int amount) {
		super(id, amount);

		this.speciesType = speciesType;
		this.stage = stage;
	}

	public ISpeciesType<?, ?> getSpeciesType() {
		return this.speciesType;
	}

	public ILifeStage getStage() {
		return this.stage;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return GRecipeTypes.DNA.serializer();
	}

	@Override
	public RecipeType<?> getType() {
		return GRecipeTypes.DNA.type();
	}

	@Override
	public boolean isIngredient(ItemStack stack) {
		return IIndividualHandlerItem.filter(stack, (i, stage) -> stage == this.stage);
	}

	public static class Serializer implements RecipeSerializer<DnaRecipe> {
		private static final Codec<ISpeciesType<?, ?>> SPECIES_TYPE_CODEC = ResourceLocation.CODEC.comapFlatMap(
			id -> {
				ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(id);
				return type == null ? DataResult.error(() -> "Unknown Forestry species type: " + id) : DataResult.success(type);
			}, ISpeciesType::id);
		private static final MapCodec<DnaRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(DnaRecipe::getId),
			SPECIES_TYPE_CODEC.fieldOf("species_type").forGetter(DnaRecipe::getSpeciesType),
			Codec.STRING.fieldOf("stage").forGetter(recipe -> recipe.stage.getSerializedName()),
			Codec.INT.fieldOf("amount").forGetter(DnaRecipe::getAmount)
		).apply(instance, Serializer::create));
		private static final StreamCodec<RegistryFriendlyByteBuf, DnaRecipe> STREAM_CODEC = StreamCodec.of(
			(buffer, recipe) -> {
				ResourceLocation.STREAM_CODEC.encode(buffer, recipe.getId());
				ResourceLocation.STREAM_CODEC.encode(buffer, recipe.speciesType.id());
				ByteBufCodecs.STRING_UTF8.encode(buffer, recipe.stage.getSerializedName());
				ByteBufCodecs.VAR_INT.encode(buffer, recipe.amount);
			},
			buffer -> {
				ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buffer);
				ISpeciesType<?, ?> type = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ResourceLocation.STREAM_CODEC.decode(buffer));
				String stage = ByteBufCodecs.STRING_UTF8.decode(buffer);
				int amount = ByteBufCodecs.VAR_INT.decode(buffer);
				return create(id, type, stage, amount);
			}
		);

		private static DnaRecipe create(ResourceLocation id, ISpeciesType<?, ?> type, String stageName, int amount) {
			for (ILifeStage stage : type.getLifeStages()) {
				if (stage.getSerializedName().equals(stageName)) return new DnaRecipe(id, type, stage, amount);
			}
			throw new IllegalArgumentException("Unknown life stage '" + stageName + "' for species type " + type.id());
		}

		@Override
		public MapCodec<DnaRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, DnaRecipe> streamCodec() {
			return STREAM_CODEC;
		}
	}
}
