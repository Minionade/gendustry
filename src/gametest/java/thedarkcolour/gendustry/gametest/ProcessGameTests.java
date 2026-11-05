package thedarkcolour.gendustry.gametest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.tile.TilePowered;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.blockentity.DnaExtractorBlockEntity;
import thedarkcolour.gendustry.blockentity.GeneticTransposerInventory;
import thedarkcolour.gendustry.blockentity.ImprinterInventory;
import thedarkcolour.gendustry.blockentity.MutatronInventory;
import thedarkcolour.gendustry.blockentity.PoweredTankBlockEntity;
import thedarkcolour.gendustry.blockentity.ProducerInventory;
import thedarkcolour.gendustry.blockentity.ReplicatorInventory;
import thedarkcolour.gendustry.blockentity.SamplerInventory;
import thedarkcolour.gendustry.item.GeneSampleItem;
import thedarkcolour.gendustry.item.GeneticTemplateItem;
import thedarkcolour.gendustry.item.GendustryResourceType;
import thedarkcolour.gendustry.item.SpeciesTypeItem;
import thedarkcolour.gendustry.recipe.GeneticTemplateRecipe;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GItems;

/**
 * Drives each machine through a full work cycle with real Forestry individuals.
 */
@GameTestHolder(Gendustry.ID)
@PrefixGameTestTemplate(false)
public class ProcessGameTests {
	private static final String EMPTY = "empty";
	private static final BlockPos POS = new BlockPos(2, 1, 2);

	// ---- helpers

	@SuppressWarnings("unchecked")
	private static <T extends TileForestry> T place(GameTestHelper helper, GendustryMachineType type) {
		helper.setBlock(POS, GBlocks.MACHINE.get(type).block().defaultBlockState());
		T be = (T) helper.getBlockEntity(POS);
		helper.assertTrue(be != null, "No block entity for " + type);
		return be;
	}

	/** Keeps the machine topped up with energy so only the recipe logic is under test. */
	private static void keepPowered(GameTestHelper helper, TilePowered be) {
		helper.onEachTick(() -> be.getEnergyManager().setEnergyStored(be.getEnergyManager().getMaxEnergyStored()));
	}

	private static ISpeciesType<?, ?> beeType() {
		return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
	}

	private static ISpecies<?> bee(String name) {
		return beeType().getSpecies(ForestryConstants.forestry(name));
	}

	private static ItemStack labware(int count) {
		return new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE), count);
	}

	private static ItemStack blankSample(int count) {
		return new ItemStack(GItems.RESOURCE.item(GendustryResourceType.BLANK_GENE_SAMPLE), count);
	}

	private static ItemStack blankTemplate() {
		return new ItemStack(GItems.RESOURCE.item(GendustryResourceType.BLANK_GENETIC_TEMPLATE));
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

	private static int fluidAmount(PoweredTankBlockEntity be, int tank, GFluids expected) {
		FluidStack stack = be.getTankManager().getFluidInTank(tank);
		if (stack.isEmpty()) return 0;
		if (stack.getFluid() != expected.fluid()) throw new AssertionError("Tank " + tank + " holds " + stack + ", expected " + expected);
		return stack.getAmount();
	}

	// ---- producers

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void mutagenProducerMakesMutagen(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTAGEN_PRODUCER);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, new ItemStack(Items.REDSTONE, 2));

		helper.succeedWhen(() -> {
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.MUTAGEN), 100, "mutagen from one redstone");
			helper.assertValueEqual(be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).getCount(), 1, "redstone left");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void mutagenProducerRejectsJunk(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTAGEN_PRODUCER);
		helper.assertFalse(be.getInternalInventory().canSlotAccept(ProducerInventory.SLOT_INPUT, new ItemStack(Items.DIRT)), "Dirt accepted");
		helper.assertTrue(be.getInternalInventory().canSlotAccept(ProducerInventory.SLOT_INPUT, new ItemStack(Items.GLOWSTONE_DUST)), "Glowstone dust rejected");
		helper.succeed();
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void proteinLiquefierMakesProtein(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.PROTEIN_LIQUEFIER);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, new ItemStack(Items.PORKCHOP, 2));

		helper.succeedWhen(() -> {
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.PROTEIN), 500, "protein from one porkchop");
			helper.assertValueEqual(be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).getCount(), 1, "porkchops left");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void dnaExtractorMakesDna(GameTestHelper helper) {
		DnaExtractorBlockEntity be = place(helper, GendustryMachineType.DNA_EXTRACTOR);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, bee("forest").createStack(BeeLifeStage.DRONE));
		be.getInternalInventory().setItem(ProducerInventory.SLOT_LABWARE, labware(8));

		helper.succeedWhen(() -> {
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.LIQUID_DNA), 100, "DNA from one drone");
			helper.assertTrue(be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).isEmpty(), "Drone not consumed");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void dnaExtractorNeedsLabware(GameTestHelper helper) {
		DnaExtractorBlockEntity be = place(helper, GendustryMachineType.DNA_EXTRACTOR);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, bee("forest").createStack(BeeLifeStage.DRONE));

		helper.runAfterDelay(120, () -> {
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.LIQUID_DNA), 0, "DNA made without labware");
			helper.assertFalse(be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).isEmpty(), "Drone consumed without labware");
			helper.succeed();
		});
	}

	// ---- samples, templates and imprinting

	@GameTest(template = EMPTY, timeoutTicks = 400)
	public static void samplerTakesGeneSample(GameTestHelper helper) {
		TilePowered be = place(helper, GendustryMachineType.SAMPLER);
		keepPowered(helper, be);
		ISpecies<?> forest = bee("forest");
		be.getInternalInventory().setItem(SamplerInventory.SLOT_INPUT, forest.createStack(BeeLifeStage.DRONE));
		be.getInternalInventory().setItem(SamplerInventory.SLOT_BLANK_SAMPLE, blankSample(2));
		be.getInternalInventory().setItem(SamplerInventory.SLOT_LABWARE, labware(2));

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(SamplerInventory.SLOT_OUTPUT);
			helper.assertTrue(out.is(GItems.GENE_SAMPLE.item()), "No gene sample produced, got " + out);
			helper.assertTrue(SpeciesTypeItem.getSpeciesType(out) == beeType(), "Sample has the wrong species type");
			helper.assertTrue(GeneSampleItem.getChromosome(out) != null && GeneSampleItem.getAllele(out) != null, "Sample is missing its gene");
			helper.assertValueEqual(be.getInternalInventory().getItem(SamplerInventory.SLOT_BLANK_SAMPLE).getCount(), 1, "blank samples left");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void imprinterWritesTemplateIntoBee(GameTestHelper helper) {
		TilePowered be = place(helper, GendustryMachineType.IMPRINTER);
		keepPowered(helper, be);
		ISpecies<?> forest = bee("forest");
		ISpecies<?> marshy = bee("marshy");
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_INPUT, forest.createStack(BeeLifeStage.DRONE));
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_TEMPLATE, templateFor(marshy));
		be.getInternalInventory().setItem(ImprinterInventory.SLOT_LABWARE, labware(2));

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(ImprinterInventory.SLOT_OUTPUT);
			IIndividual individual = IIndividualHandlerItem.getIndividual(out);
			helper.assertTrue(individual != null, "No individual produced, got " + out);
			helper.assertTrue(individual.getSpecies().id().equals(marshy.id()), "Imprinted bee is " + individual.getSpecies().id() + ", expected marshy");
			helper.assertTrue(IIndividualHandlerItem.getLifeStage(out) == BeeLifeStage.DRONE, "Life stage changed");
			helper.assertTrue(be.getInternalInventory().getItem(ImprinterInventory.SLOT_INPUT).isEmpty(), "Input not consumed");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 400)
	public static void transposerCopiesSamples(GameTestHelper helper) {
		TilePowered be = place(helper, GendustryMachineType.GENETIC_TRANSPOSER);
		keepPowered(helper, be);
		ISpecies<?> forest = bee("forest");
		IChromosome<?> speciesChromosome = beeType().getKaryotype().getSpeciesChromosome();
		ItemStack sample = GeneSampleItem.createStack(beeType(), speciesChromosome, forest.getDefaultGenome().getActiveAllele(speciesChromosome));
		be.getInternalInventory().setItem(GeneticTransposerInventory.SLOT_SOURCE, sample);
		be.getInternalInventory().setItem(GeneticTransposerInventory.SLOT_INPUT, blankSample(2));
		be.getInternalInventory().setItem(GeneticTransposerInventory.SLOT_LABWARE, labware(4));

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(GeneticTransposerInventory.SLOT_OUTPUT);
			helper.assertTrue(ItemStack.isSameItemSameComponents(out, sample.copyWithCount(1)), "Copy differs from source: " + out);
			helper.assertTrue(be.getInternalInventory().getItem(GeneticTransposerInventory.SLOT_SOURCE).is(GItems.GENE_SAMPLE.item()), "Source sample was consumed");
		});
	}

	@GameTest(template = EMPTY)
	public static void transposerRejectsMismatchedBlanks(GameTestHelper helper) {
		TilePowered be = place(helper, GendustryMachineType.GENETIC_TRANSPOSER);
		be.getInternalInventory().setItem(GeneticTransposerInventory.SLOT_SOURCE, templateFor(bee("forest")));
		helper.assertFalse(be.getInternalInventory().canSlotAccept(GeneticTransposerInventory.SLOT_INPUT, blankSample(1)), "Blank sample accepted for a template source");
		helper.assertTrue(be.getInternalInventory().canSlotAccept(GeneticTransposerInventory.SLOT_INPUT, blankTemplate()), "Blank template rejected for a template source");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void geneticTemplateCraftingCombinesSamples(GameTestHelper helper) {
		ISpecies<?> forest = bee("forest");
		List<ItemStack> grid = new java.util.ArrayList<>();
		grid.add(blankTemplate());
		int samples = 0;
		for (Map.Entry<IChromosome<?>, Allele<?>> entry : activeAlleles(forest).entrySet()) {
			grid.add(GeneSampleItem.createStack(beeType(), entry.getKey(), entry.getValue()));
			samples++;
		}
		// 3x3 holds the template + 8 samples, which is plenty to check the recipe logic
		while (grid.size() > 9) grid.remove(grid.size() - 1);
		while (grid.size() < 9) grid.add(ItemStack.EMPTY);

		CraftingInput input = CraftingInput.of(3, 3, grid);
		GeneticTemplateRecipe recipe = new GeneticTemplateRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
		helper.assertTrue(recipe.matches(input, helper.getLevel()), "Recipe does not match a blank template plus samples (" + samples + " chromosomes)");
		ItemStack result = recipe.assemble(input, helper.getLevel().registryAccess());
		helper.assertTrue(result.is(GItems.GENETIC_TEMPLATE.item()), "Result is not a genetic template: " + result);
		helper.assertTrue(SpeciesTypeItem.getSpeciesType(result) == beeType(), "Template has the wrong species type");
		helper.assertValueEqual(GeneticTemplateItem.getAlleles(result).size(), 8, "alleles stored");
		helper.succeed();
	}

	// ---- replicator

	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void replicatorBuildsBeeFromTemplate(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.REPLICATOR);
		keepPowered(helper, be);
		ISpecies<?> forest = bee("forest");
		helper.assertTrue(GeneticTemplateItem.isComplete(templateFor(forest)), "Test template is not complete");
		be.getInternalInventory().setItem(ReplicatorInventory.SLOT_TEMPLATE, templateFor(forest));
		be.getTankManager().fill(new FluidStack(GFluids.LIQUID_DNA.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);
		be.getTankManager().fill(new FluidStack(GFluids.PROTEIN.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(ReplicatorInventory.SLOT_OUTPUT);
			IIndividual individual = IIndividualHandlerItem.getIndividual(out);
			helper.assertTrue(individual != null, "No bee produced, got " + out);
			helper.assertTrue(individual.getSpecies().id().equals(forest.id()), "Replicated " + individual.getSpecies().id());
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.LIQUID_DNA), 1000, "DNA left");
			helper.assertValueEqual(fluidAmount(be, 1, GFluids.PROTEIN), 1000, "protein left");
		});
	}

	@GameTest(template = EMPTY)
	public static void replicatorRejectsIncompleteTemplate(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.REPLICATOR);
		ItemStack partial = new ItemStack(GItems.GENETIC_TEMPLATE.item());
		SpeciesTypeItem.setSpeciesType(partial, beeType());
		IChromosome<?> species = beeType().getKaryotype().getSpeciesChromosome();
		GeneticTemplateItem.addAlleles(partial, Map.of(species, bee("forest").getDefaultGenome().getActiveAllele(species)));
		helper.assertFalse(be.getInternalInventory().canSlotAccept(ReplicatorInventory.SLOT_TEMPLATE, partial), "Incomplete template accepted");
		helper.assertTrue(be.getInternalInventory().canSlotAccept(ReplicatorInventory.SLOT_TEMPLATE, templateFor(bee("forest"))), "Complete template rejected");
		helper.succeed();
	}

	// ---- mutatrons

	@GameTest(template = EMPTY, timeoutTicks = 800)
	public static void mutatronMutatesForestAndMarshy(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTATRON);
		keepPowered(helper, be);
		ISpeciesType<?, ?> type = beeType();
		ISpecies<?> forest = bee("forest");
		ISpecies<?> marshy = bee("marshy");
		ILifeStage primaryStage = type.getTypeForMutation(0);
		ILifeStage secondaryStage = type.getTypeForMutation(1);
		be.getInternalInventory().setItem(MutatronInventory.SLOT_PRIMARY, forest.createStack(primaryStage));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_SECONDARY, marshy.createStack(secondaryStage));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_LABWARE, labware(2));
		be.getTankManager().fill(new FluidStack(GFluids.MUTAGEN.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT);
			IIndividual individual = IIndividualHandlerItem.getIndividual(out);
			helper.assertTrue(individual != null, "No mutation produced, got " + out);
			boolean expected = false;
			for (IMutation<?> mutation : mutationsBetween(type, forest, marshy)) {
				expected |= mutation.getResult().id().equals(individual.getSpecies().id());
			}
			helper.assertTrue(expected, "Mutated into " + individual.getSpecies().id() + " which is not a forest + marshy result");
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.MUTAGEN), 1000, "mutagen left");
			helper.assertTrue(be.getInternalInventory().getItem(MutatronInventory.SLOT_PRIMARY).isEmpty(), "Primary not consumed");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 400)
	public static void mutatronRejectsUnrelatedPair(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTATRON);
		keepPowered(helper, be);
		ISpeciesType<?, ?> type = beeType();
		// a species paired with itself has no mutation
		ISpecies<?> forest = bee("forest");
		helper.assertTrue(mutationsBetween(type, forest, forest).isEmpty(), "Forest + forest unexpectedly mutates");
		be.getInternalInventory().setItem(MutatronInventory.SLOT_PRIMARY, forest.createStack(type.getTypeForMutation(0)));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_SECONDARY, forest.createStack(type.getTypeForMutation(1)));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_LABWARE, labware(2));
		be.getTankManager().fill(new FluidStack(GFluids.MUTAGEN.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);

		helper.runAfterDelay(300, () -> {
			helper.assertTrue(be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT).isEmpty(), "Mutatron produced a result for a pair with no mutation");
			helper.assertValueEqual(fluidAmount(be, 0, GFluids.MUTAGEN), 2000, "mutagen consumed");
			helper.succeed();
		});
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static List<IMutation<?>> mutationsBetween(ISpeciesType<?, ?> type, ISpecies<?> a, ISpecies<?> b) {
		return (List) ((forestry.api.core.genetics.IMutationManager) type.getMutations()).getCombinations(a, b);
	}
}
