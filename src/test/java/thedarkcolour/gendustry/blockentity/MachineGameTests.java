package thedarkcolour.gendustry.blockentity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import forestry.api.IForestryApi;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.Allele;
import forestry.core.platform.tile.TilePowered;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.item.GendustryResourceType;
import thedarkcolour.gendustry.item.GeneSampleItem;
import thedarkcolour.gendustry.item.GeneticTemplateItem;
import thedarkcolour.gendustry.item.SpeciesTypeItem;
import thedarkcolour.gendustry.recipe.DnaRecipe;
import thedarkcolour.gendustry.recipe.MutagenRecipe;
import thedarkcolour.gendustry.recipe.ProteinRecipe;
import thedarkcolour.gendustry.recipe.cache.DnaRecipeCache;
import thedarkcolour.gendustry.recipe.cache.MutagenRecipeCache;
import thedarkcolour.gendustry.recipe.cache.ProteinRecipeCache;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GItems;
import thedarkcolour.gendustry.registry.GRecipeTypes;

@GameTestHolder(Gendustry.ID)
public class MachineGameTests {
	private static final BlockPos MACHINE_POS = new BlockPos(1, 1, 1);

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void allMachinesPlaceAndRegister(GameTestHelper helper) {
		int index = 0;
		for (GendustryMachineType type : GendustryMachineType.values()) {
			BlockPos relativePos = new BlockPos(index++ * 2, 1, 1);
			helper.setBlock(relativePos, GBlocks.MACHINE.get(type).block());
			BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(relativePos));
			require(matchesBlockEntity(type, blockEntity), "Wrong block entity for " + type);
		}
		helper.succeed();
	}

	@GameTest(template = "empty", timeoutTicks = 300)
	@PrefixGameTestTemplate(false)
	public static void mutagenProducerProcessesRedstone(GameTestHelper helper) {
		MutagenProducerBlockEntity machine = place(helper, GendustryMachineType.MUTAGEN_PRODUCER, MutagenProducerBlockEntity.class);
		insert(machine.getItemHandler(null), ProducerInventory.SLOT_INPUT, new ItemStack(Items.REDSTONE));
		power(helper, machine);
		require(machine.hasWork(), "Mutagen producer did not find a redstone recipe");

		helper.succeedWhen(() -> helper.assertTrue(machine.outputTank.getFluid().getFluid() == GFluids.MUTAGEN.fluid()
						&& machine.outputTank.getFluid().getAmount() == 100,
				"Mutagen producer did not produce 100 mB; progress=" + machine.getWorkCounter() + ", energy=" + machine.getEnergyManager().getEnergyStored()));
	}

	@GameTest(template = "empty", timeoutTicks = 100)
	@PrefixGameTestTemplate(false)
	public static void proteinLiquefierProcessesBeef(GameTestHelper helper) {
		ProteinLiquefierBlockEntity machine = place(helper, GendustryMachineType.PROTEIN_LIQUEFIER, ProteinLiquefierBlockEntity.class);
		insert(machine.getItemHandler(null), ProducerInventory.SLOT_INPUT, new ItemStack(Items.BEEF));
		power(helper, machine);
		require(machine.hasWork(), "Protein liquefier did not find a beef recipe");

		helper.succeedWhen(() -> helper.assertTrue(machine.outputTank.getFluid().getFluid() == GFluids.PROTEIN.fluid()
						&& machine.outputTank.getFluid().getAmount() == 500,
				"Protein liquefier did not produce 500 mB; progress=" + machine.getWorkCounter() + ", energy=" + machine.getEnergyManager().getEnergyStored()));
	}

	@GameTest(template = "empty", timeoutTicks = 100)
	@PrefixGameTestTemplate(false)
	public static void dnaExtractorProcessesBee(GameTestHelper helper) {
		DnaExtractorBlockEntity machine = place(helper, GendustryMachineType.DNA_EXTRACTOR, DnaExtractorBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		insert(machine.getItemHandler(null), ProducerInventory.SLOT_INPUT, individual(beeType, 0));
		insert(machine.getItemHandler(null), ProducerInventory.SLOT_LABWARE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE)));
		power(helper, machine);

		helper.succeedWhen(() -> helper.assertTrue(machine.outputTank.getFluid().getFluid() == GFluids.LIQUID_DNA.fluid()
						&& machine.outputTank.getFluid().getAmount() > 0,
				"DNA extractor did not produce liquid DNA"));
	}

	@GameTest(template = "empty", timeoutTicks = 100)
	@PrefixGameTestTemplate(false)
	public static void samplerCreatesGeneSample(GameTestHelper helper) {
		SamplerBlockEntity machine = place(helper, GendustryMachineType.SAMPLER, SamplerBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		insert(machine.getItemHandler(null), SamplerInventory.SLOT_INPUT, individual(beeType, 0));
		insert(machine.getItemHandler(null), SamplerInventory.SLOT_BLANK_SAMPLE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.BLANK_GENE_SAMPLE)));
		insert(machine.getItemHandler(null), SamplerInventory.SLOT_LABWARE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE)));
		power(helper, machine);

		helper.succeedWhen(() -> helper.assertTrue(
				machine.getItemHandler(null).getStackInSlot(SamplerInventory.SLOT_OUTPUT).is(GItems.GENE_SAMPLE.item()),
				"Sampler did not create a gene sample"));
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void mutatronConsumesMutagenAndProducesMutation(GameTestHelper helper) {
		MutatronBlockEntity machine = place(helper, GendustryMachineType.MUTATRON, MutatronBlockEntity.class);
		IMutation<?> mutation = mutation();
		insertMutationInputs(machine.getItemHandler(null), mutation);
		require(machine.getTankManager().fill(GFluids.MUTAGEN.fluidStack(1000), FluidAction.EXECUTE) == 1000, "Mutatron rejected mutagen");
		require(machine.hasWork(), "Mutatron did not accept valid mutation inputs");
		require(machine.getCurrentMutation() != null, "Mutatron did not select a mutation");
		require(machine.workCycle(), "Mutatron work cycle failed");
		require(!machine.getItemHandler(null).getStackInSlot(MutatronInventory.SLOT_RESULT).isEmpty(), "Mutatron produced no offspring");
		require(machine.getTankManager().getTank(0).getFluid().isEmpty(), "Mutatron did not consume mutagen");
		helper.succeed();
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void advancedMutatronRequiresSelectionAndProducesMutation(GameTestHelper helper) {
		AdvancedMutatronBlockEntity machine = place(helper, GendustryMachineType.ADVANCED_MUTATRON, AdvancedMutatronBlockEntity.class);
		IMutation<?> mutation = mutation();
		insertMutationInputs(machine.getItemHandler(null), mutation);
		require(machine.getTankManager().fill(GFluids.MUTAGEN.fluidStack(1000), FluidAction.EXECUTE) == 1000, "Advanced mutatron rejected mutagen");
		require(!machine.hasWork(), "Advanced mutatron worked without a selected mutation");
		require(!machine.getPossibilities().isEmpty(), "Advanced mutatron found no available mutations");
		machine.setCurrentMutation(machine.getPossibilities().get(0),
				machine.getItemHandler(null).getStackInSlot(MutatronInventory.SLOT_PRIMARY),
				machine.getItemHandler(null).getStackInSlot(MutatronInventory.SLOT_SECONDARY));
		require(machine.hasWork(), "Advanced mutatron rejected the selected mutation");
		require(machine.workCycle(), "Advanced mutatron work cycle failed");
		require(!machine.getItemHandler(null).getStackInSlot(MutatronInventory.SLOT_RESULT).isEmpty(), "Advanced mutatron produced no offspring");
		require(machine.getTankManager().getTank(0).getFluid().isEmpty(), "Advanced mutatron did not consume mutagen");
		helper.succeed();
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void imprinterAppliesGeneticTemplate(GameTestHelper helper) {
		ImprinterBlockEntity machine = place(helper, GendustryMachineType.IMPRINTER, ImprinterBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		ItemStack bee = individual(beeType, 0);
		ItemStack template = completeTemplate(beeType);
		insert(machine.getItemHandler(null), ImprinterInventory.SLOT_INPUT, bee);
		insert(machine.getItemHandler(null), ImprinterInventory.SLOT_TEMPLATE, template);
		insert(machine.getItemHandler(null), ImprinterInventory.SLOT_LABWARE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE)));
		require(machine.hasWork(), "Imprinter did not accept valid inputs");
		require(machine.workCycle(), "Imprinter work cycle failed");
		require(!machine.getItemHandler(null).getStackInSlot(ImprinterInventory.SLOT_OUTPUT).isEmpty(), "Imprinter produced no specimen");
		helper.succeed();
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void geneticTransposerCopiesGeneSample(GameTestHelper helper) {
		GeneticTransposerBlockEntity machine = place(helper, GendustryMachineType.GENETIC_TRANSPOSER, GeneticTransposerBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		IChromosome<?> chromosome = beeType.getKaryotype().getChromosomes().get(0);
		ItemStack sample = GeneSampleItem.createStack(beeType, chromosome, defaultAllele(beeType, chromosome));
		insert(machine.getItemHandler(null), GeneticTransposerInventory.SLOT_INPUT,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.BLANK_GENE_SAMPLE)));
		insert(machine.getItemHandler(null), GeneticTransposerInventory.SLOT_SOURCE, sample);
		insert(machine.getItemHandler(null), GeneticTransposerInventory.SLOT_LABWARE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE)));
		require(machine.hasWork(), "Genetic transposer did not accept valid inputs");
		require(machine.workCycle(), "Genetic transposer work cycle failed");
		ItemStack output = machine.getItemHandler(null).getStackInSlot(GeneticTransposerInventory.SLOT_OUTPUT);
		require(output.is(GItems.GENE_SAMPLE.item()), "Genetic transposer did not copy the gene sample");
		helper.succeed();
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void replicatorConsumesFluidsAndCreatesIndividual(GameTestHelper helper) {
		ReplicatorBlockEntity machine = place(helper, GendustryMachineType.REPLICATOR, ReplicatorBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		insert(machine.getItemHandler(null), ReplicatorInventory.SLOT_TEMPLATE, completeTemplate(beeType));
		require(machine.getTankManager().fill(GFluids.LIQUID_DNA.fluidStack(1000), FluidAction.EXECUTE) == 1000, "Replicator rejected liquid DNA");
		require(machine.getTankManager().fill(GFluids.PROTEIN.fluidStack(1000), FluidAction.EXECUTE) == 1000, "Replicator rejected protein");
		require(machine.hasWork(), "Replicator did not accept valid template and fluids");
		require(machine.workCycle(), "Replicator work cycle failed");
		ItemStack output = machine.getItemHandler(null).getStackInSlot(ReplicatorInventory.SLOT_OUTPUT);
		require(forestry.api.core.genetics.capability.IIndividualHandlerItem.isIndividual(output), "Replicator produced no individual");
		require(machine.getTankManager().getTank(0).getFluid().isEmpty(), "Replicator did not consume liquid DNA");
		require(machine.getTankManager().getTank(1).getFluid().isEmpty(), "Replicator did not consume protein");
		helper.succeed();
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void industrialApiaryStoresBeesAndReceivesPower(GameTestHelper helper) {
		IndustrialApiaryBlockEntity machine = place(helper, GendustryMachineType.INDUSTRIAL_APIARY, IndustrialApiaryBlockEntity.class);
		ISpeciesType<?, ?> beeType = beeType();
		var queen = beeType.getDefaultSpecies().createIndividual();
		var drone = beeType.getDefaultSpecies().createIndividual();
		queen.setMate(drone.getGenome());
		machine.getBeeInventory().setQueen(queen.createStack(beeType.getTypeForMutation(2)));
		machine.getBeeInventory().setDrone(drone.createStack(beeType.getTypeForMutation(1)));
		require(!machine.getBeeInventory().getQueen().isEmpty(), "Industrial apiary did not store its queen");
		require(!machine.getBeeInventory().getDrone().isEmpty(), "Industrial apiary did not store its drone");
		require(machine.getEnergyHandler(null).receiveEnergy(1000, false) == 1000, "Industrial apiary did not accept energy");
		for (int x = -3; x <= 3; x++) {
			for (int z = -3; z <= 3; z++) {
				if (x != 0 || z != 0) {
					helper.setBlock(MACHINE_POS.offset(x, -1, z), Blocks.GRASS_BLOCK);
					helper.setBlock(MACHINE_POS.offset(x, 0, z), Blocks.DANDELION);
				}
			}
		}
		machine.getBeekeepingLogic().setWorkThrottle(5);
		helper.onEachTick(() -> machine.getEnergyHandler(null).receiveEnergy(10_000, false));

		helper.succeedWhen(() -> helper.assertTrue(machine.getBeekeepingLogic().getBeeProgressPercent() > 0,
				"Industrial apiary did not make bee-work progress; canWork=" + machine.getBeekeepingLogic().canWork()
						+ ", flowers=" + machine.getBeekeepingLogic().getFlowerPositions().size()));
	}

	@GameTest(template = "empty")
	@PrefixGameTestTemplate(false)
	public static void customRecipeFormatsLoadAndPopulateCaches(GameTestHelper helper) {
		List<RecipeHolder<MutagenRecipe>> mutagenRecipes = helper.getLevel().getRecipeManager().getAllRecipesFor(GRecipeTypes.MUTAGEN.type());
		List<RecipeHolder<ProteinRecipe>> proteinRecipes = helper.getLevel().getRecipeManager().getAllRecipesFor(GRecipeTypes.PROTEIN.type());
		List<RecipeHolder<DnaRecipe>> dnaRecipes = helper.getLevel().getRecipeManager().getAllRecipesFor(GRecipeTypes.DNA.type());

		require(!mutagenRecipes.isEmpty(), "No mutagen recipes loaded");
		require(!proteinRecipes.isEmpty(), "No protein recipes loaded");
		require(!dnaRecipes.isEmpty(), "No DNA recipes loaded");
		for (RecipeHolder<MutagenRecipe> holder : mutagenRecipes) {
			MutagenRecipe recipe = holder.value();
			require(recipe.getAmount() > 0 && !recipe.getIngredient().isEmpty(), "Invalid mutagen recipe " + recipe.getId());
		}
		for (RecipeHolder<ProteinRecipe> holder : proteinRecipes) {
			ProteinRecipe recipe = holder.value();
			require(recipe.getAmount() > 0 && !recipe.getIngredient().isEmpty(), "Invalid protein recipe " + recipe.getId());
		}
		for (RecipeHolder<DnaRecipe> holder : dnaRecipes) {
			DnaRecipe recipe = holder.value();
			require(recipe.getAmount() > 0 && recipe.getStage() != null, "Invalid DNA recipe " + recipe.getId());
		}
		require(MutagenRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.REDSTONE)) != null, "Mutagen recipe cache is empty");
		require(ProteinRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.BEEF)) != null, "Protein recipe cache is empty");
		require(DnaRecipeCache.INSTANCE.getRecipe(beeType().getTypeForMutation(0)) != null, "DNA recipe cache is missing a bee stage");
		helper.succeed();
	}

	private static <T extends BlockEntity> T place(GameTestHelper helper, GendustryMachineType type, Class<T> expectedType) {
		helper.setBlock(MACHINE_POS, GBlocks.MACHINE.get(type).block());
		BlockEntity blockEntity = helper.getLevel().getBlockEntity(helper.absolutePos(MACHINE_POS));
		require(expectedType.isInstance(blockEntity), "Wrong block entity for " + type);
		return expectedType.cast(blockEntity);
	}

	private static boolean matchesBlockEntity(GendustryMachineType type, BlockEntity blockEntity) {
		return switch (type) {
			case INDUSTRIAL_APIARY -> blockEntity instanceof IndustrialApiaryBlockEntity;
			case MUTAGEN_PRODUCER -> blockEntity instanceof MutagenProducerBlockEntity;
			case DNA_EXTRACTOR -> blockEntity instanceof DnaExtractorBlockEntity;
			case PROTEIN_LIQUEFIER -> blockEntity instanceof ProteinLiquefierBlockEntity;
			case SAMPLER -> blockEntity instanceof SamplerBlockEntity;
			case MUTATRON -> blockEntity instanceof MutatronBlockEntity;
			case ADVANCED_MUTATRON -> blockEntity instanceof AdvancedMutatronBlockEntity;
			case IMPRINTER -> blockEntity instanceof ImprinterBlockEntity;
			case GENETIC_TRANSPOSER -> blockEntity instanceof GeneticTransposerBlockEntity;
			case REPLICATOR -> blockEntity instanceof ReplicatorBlockEntity;
		};
	}

	private static ISpeciesType<?, ?> beeType() {
		return IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes().stream()
				.filter(type -> type.getDefaultSpecies().createIndividual() instanceof IBee)
				.findFirst()
				.orElseThrow(() -> new AssertionError("Forestry bee species type is not registered"));
	}

	private static ItemStack individual(ISpeciesType<?, ?> type, int mutationStage) {
		return type.getDefaultSpecies().createIndividual().createStack(type.getTypeForMutation(mutationStage));
	}

	private static ItemStack completeTemplate(ISpeciesType<?, ?> type) {
		ItemStack template = new ItemStack(GItems.GENETIC_TEMPLATE.item());
		SpeciesTypeItem.setSpeciesType(template, type);
		GeneticTemplateItem.addAlleles(template, type.getKaryotype().getDefaultAlleles());
		require(GeneticTemplateItem.isComplete(template), "Could not create a complete genetic template");
		return template;
	}

	private static Allele<?> defaultAllele(ISpeciesType<?, ?> type, IChromosome<?> chromosome) {
		return type.getKaryotype().getDefaultAllele(chromosome);
	}

	private static IMutation<?> mutation() {
		return IForestryApi.INSTANCE.getGeneticManager().getMutations(beeType()).getAllMutations().stream()
				.findFirst()
				.orElseThrow(() -> new AssertionError("No bee mutations are registered"));
	}

	private static void insert(IItemHandler inventory, int slot, ItemStack stack) {
		ItemStack remainder = inventory.insertItem(slot, stack, false);
		require(remainder.isEmpty(), "Machine rejected item in slot " + slot + ": " + stack);
		inventory.insertItem(slot, stack, true);
	}

	private static void insertMutationInputs(IItemHandler inventory, IMutation<?> mutation) {
		ISpeciesType<?, ?> type = mutation.getType();
		insert(inventory, MutatronInventory.SLOT_PRIMARY,
				mutation.getFirstParent().createIndividual().createStack(type.getTypeForMutation(0)));
		insert(inventory, MutatronInventory.SLOT_SECONDARY,
				mutation.getSecondParent().createIndividual().createStack(type.getTypeForMutation(1)));
		insert(inventory, MutatronInventory.SLOT_LABWARE,
				new ItemStack(GItems.RESOURCE.item(GendustryResourceType.LABWARE)));
	}

	private static void power(GameTestHelper helper, TilePowered machine) {
		require(machine.getEnergyHandler(null).receiveEnergy(10_000, false) > 0, "Machine rejected energy");
		helper.onEachTick(() -> machine.getEnergyHandler(null).receiveEnergy(10_000, false));
	}

	private static void require(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}
}
