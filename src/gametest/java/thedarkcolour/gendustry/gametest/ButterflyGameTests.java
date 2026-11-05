package thedarkcolour.gendustry.gametest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.lepidopterology.genetics.ButterflyLifeStage;
import forestry.core.platform.tile.TilePowered;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.blockentity.DnaExtractorBlockEntity;
import thedarkcolour.gendustry.blockentity.ImprinterInventory;
import thedarkcolour.gendustry.blockentity.MutatronInventory;
import thedarkcolour.gendustry.blockentity.PoweredTankBlockEntity;
import thedarkcolour.gendustry.blockentity.ProducerInventory;
import thedarkcolour.gendustry.blockentity.ReplicatorInventory;
import thedarkcolour.gendustry.blockentity.SamplerInventory;
import thedarkcolour.gendustry.item.GeneticTemplateItem;
import thedarkcolour.gendustry.item.GendustryResourceType;
import thedarkcolour.gendustry.item.SpeciesTypeItem;
import thedarkcolour.gendustry.recipe.cache.DnaRecipeCache;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GItems;

/**
 * Butterfly support. These only run for real when the Forestry Butterflies add-on is installed
 * ({@code ./gradlew runGameTestServer -PwithButterflies}); without it they check that nothing leaks in.
 */
@GameTestHolder(Gendustry.ID)
@PrefixGameTestTemplate(false)
public class ButterflyGameTests {
	private static final String EMPTY = "empty";
	private static final BlockPos POS = new BlockPos(2, 1, 2);

	private static boolean addonLoaded() {
		return ModList.get().isLoaded("forestrybutterflies");
	}

	private static ISpeciesType<?, ?> butterflyType() {
		return IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypeSafe(ForestrySpeciesTypes.BUTTERFLY);
	}

	@SuppressWarnings("unchecked")
	private static <T extends forestry.core.platform.tile.TileForestry> T place(GameTestHelper helper, GendustryMachineType type) {
		helper.setBlock(POS, GBlocks.MACHINE.get(type).block().defaultBlockState());
		T be = (T) helper.getBlockEntity(POS);
		helper.assertTrue(be != null, "No block entity for " + type);
		return be;
	}

	private static void keepPowered(GameTestHelper helper, TilePowered be) {
		helper.onEachTick(() -> be.getEnergyManager().setEnergyStored(be.getEnergyManager().getMaxEnergyStored()));
	}

	private static ItemStack labware(int count) {
		return new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE), count);
	}

	private static Map<IChromosome<?>, Allele<?>> activeAlleles(ISpecies<?> species) {
		Map<IChromosome<?>, Allele<?>> alleles = new HashMap<>();
		for (Map.Entry<IChromosome<?>, AllelePair<?>> entry : species.getDefaultGenome().getChromosomes().entrySet()) {
			alleles.put(entry.getKey(), entry.getValue().active());
		}
		return alleles;
	}

	private static ItemStack templateFor(ISpecies<?> species) {
		ItemStack template = new ItemStack(GItems.GENETIC_TEMPLATE.item());
		SpeciesTypeItem.setSpeciesType(template, species.getType());
		GeneticTemplateItem.addAlleles(template, activeAlleles(species));
		return template;
	}

	// ---- without the add-on

	@GameTest(template = EMPTY)
	public static void butterfliesAbsentMeansNoButterflyRecipes(GameTestHelper helper) {
		if (addonLoaded()) {
			helper.succeed();
			return;
		}
		helper.assertTrue(butterflyType() == null, "A butterfly species type exists without the add-on");
		helper.assertTrue(DnaRecipeCache.INSTANCE.getRecipe(ButterflyLifeStage.BUTTERFLY) == null, "Butterfly DNA recipe loaded without the add-on");
		helper.assertTrue(DnaRecipeCache.INSTANCE.getRecipe(ButterflyLifeStage.COCOON) == null, "Cocoon DNA recipe loaded without the add-on");
		helper.succeed();
	}

	// ---- with the add-on

	@GameTest(template = EMPTY)
	public static void butterflySpeciesTypeAndRecipesLoad(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		ISpeciesType<?, ?> type = butterflyType();
		helper.assertTrue(type != null, "Butterfly species type is not registered");
		helper.assertTrue(type.getSpeciesCount() > 10, "Only " + type.getSpeciesCount() + " butterfly species");

		Object[][] expected = {
				{ButterflyLifeStage.BUTTERFLY, 200},
				{ButterflyLifeStage.SERUM, 800},
				{ButterflyLifeStage.CATERPILLAR, 1000},
				{ButterflyLifeStage.COCOON, 1000},
		};
		for (Object[] e : expected) {
			var recipe = DnaRecipeCache.INSTANCE.getRecipe((ILifeStage) e[0]);
			helper.assertTrue(recipe != null, "No DNA recipe for " + e[0]);
			helper.assertValueEqual(recipe.getAmount(), (Integer) e[1], "DNA from " + e[0]);
			helper.assertTrue(recipe.getSpeciesType() == type, "DNA recipe for " + e[0] + " points at the wrong species type");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY, timeoutTicks = 4000)
	public static void dnaExtractorHandlesEveryButterflyStage(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		DnaExtractorBlockEntity be = place(helper, GendustryMachineType.DNA_EXTRACTOR);
		keepPowered(helper, be);
		ISpecies<?> species = butterflyType().getDefaultSpecies();

		Object[][] cases = {
				{ButterflyLifeStage.BUTTERFLY, 200},
				{ButterflyLifeStage.SERUM, 800},
				{ButterflyLifeStage.CATERPILLAR, 1000},
				{ButterflyLifeStage.COCOON, 1000},
		};
		int[] index = {0};
		int[] lastAmount = {0};
		Runnable feed = () -> {
			ItemStack stack = species.createStack((ILifeStage) cases[index[0]][0]);
			be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, stack);
			be.getInternalInventory().setItem(ProducerInventory.SLOT_LABWARE, labware(16));
			// empty the tank so a 1000 mB result always fits
			be.getTankManager().drain(10000, IFluidHandler.FluidAction.EXECUTE);
			lastAmount[0] = 0;
		};
		feed.run();

		helper.onEachTick(() -> {
			if (index[0] >= cases.length) return;
			int expected = (Integer) cases[index[0]][1];
			int amount = be.getTankManager().getFluidInTank(0).getAmount();
			if (amount - lastAmount[0] == expected && be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).isEmpty()) {
				index[0]++;
				if (index[0] < cases.length) feed.run();
			}
		});
		helper.succeedWhen(() -> helper.assertTrue(index[0] >= cases.length, "Only completed " + index[0] + " of " + cases.length + " butterfly stages"));
	}

	@GameTest(template = EMPTY, timeoutTicks = 400)
	public static void samplerWorksOnButterflies(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		TilePowered be = place(helper, GendustryMachineType.SAMPLER);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(SamplerInventory.SLOT_INPUT, butterflyType().getDefaultSpecies().createStack(ButterflyLifeStage.BUTTERFLY));
		be.getInternalInventory().setItem(SamplerInventory.SLOT_BLANK_SAMPLE, new ItemStack(GItems.RESOURCE.item(GendustryResourceType.BLANK_GENE_SAMPLE), 2));
		be.getInternalInventory().setItem(SamplerInventory.SLOT_LABWARE, labware(2));

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(SamplerInventory.SLOT_OUTPUT);
			helper.assertTrue(out.is(GItems.GENE_SAMPLE.item()), "No gene sample produced, got " + out);
			helper.assertTrue(SpeciesTypeItem.getSpeciesType(out) == butterflyType(), "Sample is not a butterfly sample");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void imprinterRewritesAButterfly(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		TilePowered be = place(helper, GendustryMachineType.IMPRINTER);
		keepPowered(helper, be);
		List<? extends ISpecies<?>> all = butterflyType().getAllSpecies();
		ISpecies<?> from = all.get(0);
		ISpecies<?> to = all.get(1);
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_INPUT, from.createStack(ButterflyLifeStage.BUTTERFLY));
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_TEMPLATE, templateFor(to));
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_LABWARE, labware(2));

		helper.succeedWhen(() -> {
			IIndividual individual = IIndividualHandlerItem.getIndividual(be.getInternalInventory().getItem(ImprinterInventory.SLOT_OUTPUT));
			helper.assertTrue(individual != null, "No butterfly produced");
			helper.assertTrue(individual.getSpecies().id().equals(to.id()), "Imprinted butterfly is " + individual.getSpecies().id() + ", expected " + to.id());
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void replicatorBuildsAButterfly(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.REPLICATOR);
		keepPowered(helper, be);
		ISpecies<?> species = butterflyType().getDefaultSpecies();
		helper.assertTrue(GeneticTemplateItem.isComplete(templateFor(species)), "Butterfly template is not complete");
		be.getInternalInventory().setItem(ReplicatorInventory.SLOT_TEMPLATE, templateFor(species));
		be.getTankManager().fill(new FluidStack(GFluids.LIQUID_DNA.fluid(), 1000), IFluidHandler.FluidAction.EXECUTE);
		be.getTankManager().fill(new FluidStack(GFluids.PROTEIN.fluid(), 1000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			IIndividual individual = IIndividualHandlerItem.getIndividual(be.getInternalInventory().getItem(ReplicatorInventory.SLOT_OUTPUT));
			helper.assertTrue(individual != null, "No butterfly replicated");
			helper.assertTrue(individual.getSpecies().id().equals(species.id()), "Replicated " + individual.getSpecies().id());
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 800)
	public static void mutatronMutatesButterflies(GameTestHelper helper) {
		if (!addonLoaded()) {
			helper.succeed();
			return;
		}
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTATRON);
		keepPowered(helper, be);
		ISpeciesType<?, ?> type = butterflyType();

		@SuppressWarnings({"unchecked", "rawtypes"})
		List<IMutation<?>> all = (List) ((forestry.api.core.genetics.IMutationManager) type.getMutations()).getAllMutations();
		helper.assertFalse(all.isEmpty(), "The butterfly add-on registered no mutations");
		IMutation<?> mutation = all.get(0);
		ISpecies<?> a = mutation.getFirstParent();
		ISpecies<?> b = mutation.getSecondParent();
		be.getInternalInventory().setItem(MutatronInventory.SLOT_PRIMARY, a.createStack(type.getTypeForMutation(0)));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_SECONDARY, b.createStack(type.getTypeForMutation(1)));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_LABWARE, labware(2));
		be.getTankManager().fill(new FluidStack(GFluids.MUTAGEN.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			IIndividual individual = IIndividualHandlerItem.getIndividual(be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT));
			helper.assertTrue(individual != null, "No butterfly mutation produced (" + a.id() + " + " + b.id() + ")");
			@SuppressWarnings({"unchecked", "rawtypes"})
			List<IMutation<?>> possible = (List) ((forestry.api.core.genetics.IMutationManager) type.getMutations()).getCombinations(a, b);
			boolean valid = possible.stream().anyMatch(m -> m.getResult().id().equals(individual.getSpecies().id()));
			helper.assertTrue(valid, "Result " + individual.getSpecies().id() + " is not a mutation of " + a.id() + " + " + b.id());
		});
	}
}
