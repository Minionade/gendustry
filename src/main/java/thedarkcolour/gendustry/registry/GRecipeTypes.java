package thedarkcolour.gendustry.registry;

import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.item.crafting.RecipeSerializer;

import net.neoforged.neoforge.registries.DeferredHolder;

import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.FeatureRecipeType;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import thedarkcolour.gendustry.GendustryModule;
import thedarkcolour.gendustry.recipe.DnaRecipe;
import thedarkcolour.gendustry.recipe.GeneticTemplateRecipe;
import thedarkcolour.gendustry.recipe.MutagenRecipe;
import thedarkcolour.gendustry.recipe.ProteinRecipe;

@FeatureProvider
public class GRecipeTypes {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(GendustryModule.MODULE_ID);

	public static final FeatureRecipeType<MutagenRecipe> MUTAGEN = REGISTRY.recipeType("mutagen", MutagenRecipe.Serializer::new);
	public static final FeatureRecipeType<ProteinRecipe> PROTEIN = REGISTRY.recipeType("protein", ProteinRecipe.Serializer::new);
	public static final FeatureRecipeType<DnaRecipe> DNA = REGISTRY.recipeType("dna", DnaRecipe.Serializer::new);

	public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<?>> GENETIC_TEMPLATE_SERIALIZER = REGISTRY.getRegistry(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER).register("genetic_template", () -> new SimpleCraftingRecipeSerializer<>(GeneticTemplateRecipe::new));
}
