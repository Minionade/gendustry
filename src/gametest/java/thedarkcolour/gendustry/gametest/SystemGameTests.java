package thedarkcolour.gendustry.gametest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import forestry.api.IForestryApi;
import forestry.api.ForestryConstants;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.arboriculture.genetics.TreeLifeStage;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IIndividual;
import forestry.api.core.genetics.IMutation;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.ILifeStage;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.AllelePair;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.tile.TilePowered;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.blockentity.AdvancedMutatronBlockEntity;
import thedarkcolour.gendustry.blockentity.DnaExtractorBlockEntity;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryBlockEntity;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryInventory;
import thedarkcolour.gendustry.blockentity.MutatronInventory;
import thedarkcolour.gendustry.blockentity.PoweredTankBlockEntity;
import thedarkcolour.gendustry.blockentity.ProducerInventory;
import thedarkcolour.gendustry.item.EliteGendustryUpgradeType;
import thedarkcolour.gendustry.item.GeneSampleItem;
import thedarkcolour.gendustry.item.GeneticTemplateItem;
import thedarkcolour.gendustry.item.GendustryResourceType;
import thedarkcolour.gendustry.item.GendustryUpgradeType;
import thedarkcolour.gendustry.item.SpeciesTypeItem;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GItems;

/**
 * Automation (capabilities), persistence, upgrades, genetics encoding and the machines with more involved behaviour.
 */
@GameTestHolder(Gendustry.ID)
@PrefixGameTestTemplate(false)
public class SystemGameTests {
	private static final String EMPTY = "empty";
	private static final BlockPos POS = new BlockPos(2, 1, 2);

	@SuppressWarnings("unchecked")
	private static <T extends TileForestry> T place(GameTestHelper helper, GendustryMachineType type) {
		helper.setBlock(POS, GBlocks.MACHINE.get(type).block().defaultBlockState());
		T be = (T) helper.getBlockEntity(POS);
		helper.assertTrue(be != null, "No block entity for " + type);
		return be;
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

	private static void keepPowered(GameTestHelper helper, TilePowered be) {
		helper.onEachTick(() -> be.getEnergyManager().setEnergyStored(be.getEnergyManager().getMaxEnergyStored()));
	}

	// ---- automation

	@GameTest(template = EMPTY)
	public static void everyMachineExposesItemAndEnergyCapabilities(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (GendustryMachineType type : GendustryMachineType.values()) {
			place(helper, type);
			BlockPos abs = helper.absolutePos(POS);
			for (Direction side : new Direction[]{null, Direction.NORTH, Direction.UP, Direction.DOWN}) {
				if (helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, abs, side) == null) problems.add(type + ": no item handler on " + side);
				if (helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, abs, side) == null) problems.add(type + ": no energy storage on " + side);
			}
		}
		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void fluidMachinesExposeFluidCapability(GameTestHelper helper) {
		GendustryMachineType[] withTanks = {GendustryMachineType.MUTAGEN_PRODUCER, GendustryMachineType.DNA_EXTRACTOR, GendustryMachineType.PROTEIN_LIQUEFIER,
				GendustryMachineType.MUTATRON, GendustryMachineType.ADVANCED_MUTATRON, GendustryMachineType.REPLICATOR};
		for (GendustryMachineType type : withTanks) {
			place(helper, type);
			IFluidHandler handler = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(POS), null);
			helper.assertTrue(handler != null, type + " has no fluid handler");
			helper.assertTrue(handler.getTanks() > 0, type + " has no tanks");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void itemAutomationRespectsFilters(GameTestHelper helper) {
		place(helper, GendustryMachineType.MUTAGEN_PRODUCER);
		IItemHandler items = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(POS), Direction.UP);
		helper.assertTrue(items != null, "No item handler");

		ItemStack leftover = items.insertItem(0, new ItemStack(Items.DIRT, 4), false);
		helper.assertValueEqual(leftover.getCount(), 4, "dirt should be rejected");
		leftover = items.insertItem(0, new ItemStack(Items.REDSTONE, 4), false);
		helper.assertTrue(leftover.isEmpty(), "redstone should be accepted, leftover " + leftover);

		// the input slot cannot be pulled out by automation
		helper.assertTrue(items.extractItem(0, 64, false).isEmpty(), "automation pulled the input back out");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void tankFiltersAndDrain(GameTestHelper helper) {
		PoweredTankBlockEntity be = place(helper, GendustryMachineType.MUTAGEN_PRODUCER);
		IFluidHandler tanks = helper.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, helper.absolutePos(POS), null);
		helper.assertTrue(tanks != null, "No fluid handler");

		helper.assertValueEqual(tanks.fill(new FluidStack(net.minecraft.world.level.material.Fluids.WATER, 1000), IFluidHandler.FluidAction.EXECUTE), 0, "water accepted by the mutagen tank");
		// the producer's output tank refuses outside fills, so fill it the way the machine itself does
		((forestry.core.platform.fluids.StandardTank) be.getTankManager().getTank(0)).fillInternal(new FluidStack(GFluids.MUTAGEN.fluid(), 1500), IFluidHandler.FluidAction.EXECUTE);
		FluidStack drained = tanks.drain(1000, IFluidHandler.FluidAction.EXECUTE);
		helper.assertTrue(drained.getFluid() == GFluids.MUTAGEN.fluid() && drained.getAmount() == 1000, "drained " + drained);
		helper.assertValueEqual(tanks.getFluidInTank(0).getAmount(), 500, "mutagen left");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void energyCanBeInserted(GameTestHelper helper) {
		place(helper, GendustryMachineType.SAMPLER);
		IEnergyStorage energy = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, helper.absolutePos(POS), Direction.NORTH);
		helper.assertTrue(energy != null && energy.canReceive(), "Sampler cannot receive energy");
		int accepted = energy.receiveEnergy(5000, false);
		helper.assertTrue(accepted > 0, "No energy accepted");
		helper.assertValueEqual(energy.getEnergyStored(), accepted, "stored energy");
		helper.succeed();
	}

	// ---- buckets and fluid blocks

	@GameTest(template = EMPTY)
	public static void bucketsPlaceAndPickUpFluid(GameTestHelper helper) {
		var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(helper.getLevel());
		for (GFluids fluid : GFluids.values()) {
			BlockPos pos = new BlockPos(1, 2, 1);
			helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
			ItemStack bucket = new ItemStack(fluid.getBucket());

			FluidActionResult placed = FluidUtil.tryPlaceFluid(player, helper.getLevel(), InteractionHand.MAIN_HAND, helper.absolutePos(pos), bucket, new FluidStack(fluid.fluid(), 1000));
			helper.assertTrue(placed.isSuccess(), fluid + " bucket failed to place its fluid");
			helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(pos)).getType() == fluid.fluid(), fluid + " placed the wrong fluid");
			helper.assertTrue(placed.getResult().is(net.minecraft.world.item.Items.BUCKET), fluid + " bucket did not become empty");

			FluidActionResult picked = FluidUtil.tryPickUpFluid(new ItemStack(Items.BUCKET), player, helper.getLevel(), helper.absolutePos(pos), Direction.UP);
			helper.assertTrue(picked.isSuccess(), "Could not pick up " + fluid + " with an empty bucket");
			helper.assertTrue(picked.getResult().is(fluid.getBucket()), "Picked up " + picked.getResult() + " instead of the " + fluid + " bucket");
		}
		helper.succeed();
	}

	// ---- persistence

	@GameTest(template = EMPTY)
	public static void machineStateSurvivesSaveAndLoad(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		for (GendustryMachineType type : GendustryMachineType.values()) {
			TileForestry be = place(helper, type);
			ItemStack marker = new ItemStack(Items.DIAMOND, 3);
			be.getInternalInventory().setItem(0, marker.copy());
			if (be instanceof PoweredTankBlockEntity tank) {
				// first tank accepts either mutagen or DNA depending on the machine
				FluidStack fill = switch (type) {
					case PROTEIN_LIQUEFIER -> new FluidStack(GFluids.PROTEIN.fluid(), 700);
					case DNA_EXTRACTOR, REPLICATOR -> new FluidStack(GFluids.LIQUID_DNA.fluid(), 700);
					default -> new FluidStack(GFluids.MUTAGEN.fluid(), 700);
				};
				int filled = ((forestry.core.platform.fluids.StandardTank) tank.getTankManager().getTank(0)).fillInternal(fill, IFluidHandler.FluidAction.EXECUTE);
				if (filled != 700) problems.add(type + ": test could not fill tank 0 (" + filled + ")");
			}

			CompoundTag tag = be.saveWithFullMetadata(helper.getLevel().registryAccess());
			TileForestry copy = (TileForestry) be.getType().create(helper.absolutePos(POS).above(5), be.getBlockState());
			copy.loadCustomOnly(tag, helper.getLevel().registryAccess());

			ItemStack restored = copy.getInternalInventory().getItem(0);
			if (!ItemStack.matches(restored, marker)) problems.add(type + ": inventory lost, got " + restored);
			if (be instanceof PoweredTankBlockEntity tank && copy instanceof PoweredTankBlockEntity tankCopy) {
				for (int i = 0; i < tank.getTankManager().getTanks(); i++) {
					FluidStack before = tank.getTankManager().getFluidInTank(i);
					FluidStack after = tankCopy.getTankManager().getFluidInTank(i);
					if (!FluidStack.matches(before, after)) problems.add(type + ": tank " + i + " was " + before + " but loaded " + after);
				}
			}
		}
		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	// ---- genetics encoding

	/** Every default allele of every species type must survive being written to a gene sample and a template. */
	@GameTest(template = EMPTY)
	public static void geneSamplesRoundTripEveryAllele(GameTestHelper helper) {
		List<String> problems = new ArrayList<>();
		int checked = 0;
		for (ISpeciesType<?, ?> type : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			for (ISpecies<?> species : type.getAllSpecies()) {
				Map<IChromosome<?>, AllelePair<?>> chromosomes = species.getDefaultGenome().getChromosomes();
				ItemStack template = new ItemStack(GItems.GENETIC_TEMPLATE.item());
				SpeciesTypeItem.setSpeciesType(template, type);
				java.util.IdentityHashMap<IChromosome<?>, Allele<?>> active = new java.util.IdentityHashMap<>();

				for (Map.Entry<IChromosome<?>, AllelePair<?>> entry : chromosomes.entrySet()) {
					IChromosome<?> chromosome = entry.getKey();
					Allele<?> allele = entry.getValue().active();
					active.put(chromosome, allele);
					checked++;

					ItemStack sample = GeneSampleItem.createStack(type, chromosome, allele);
					IChromosome<?> readChromosome = GeneSampleItem.getChromosome(sample);
					Allele<?> readAllele = GeneSampleItem.getAllele(sample);
					if (readChromosome != chromosome) problems.add(species.id() + "/" + chromosome.id() + ": chromosome came back as " + readChromosome);
					else if (readAllele == null || !readAllele.value().equals(allele.value()) || readAllele.dominant() != allele.dominant()) {
						problems.add(species.id() + "/" + chromosome.id() + ": allele " + allele.value() + " came back as " + (readAllele == null ? null : readAllele.value()));
					}
				}

				GeneticTemplateItem.addAlleles(template, active);
				Map<IChromosome<?>, Allele<?>> readBack = GeneticTemplateItem.getAlleles(template);
				if (readBack.size() != active.size()) problems.add(species.id() + ": template stored " + readBack.size() + " of " + active.size() + " alleles");
				if (!GeneticTemplateItem.isComplete(template)) problems.add(species.id() + ": template is not complete");
				if (problems.size() > 30) break;
			}
		}
		helper.assertTrue(checked > 500, "Only checked " + checked + " alleles");
		if (!problems.isEmpty()) helper.fail(problems.size() + " problems: " + String.join("; ", problems.subList(0, Math.min(10, problems.size()))));
		else helper.succeed();
	}

	// ---- DNA extractor across every life stage that has a recipe

	@GameTest(template = EMPTY, timeoutTicks = 1500)
	public static void dnaExtractorHandlesEveryLifeStage(GameTestHelper helper) {
		DnaExtractorBlockEntity be = place(helper, GendustryMachineType.DNA_EXTRACTOR);
		keepPowered(helper, be);

		ISpeciesType<?, ?> trees = IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.TREE);
		Object[][] cases = {
				{bee("forest"), BeeLifeStage.DRONE, 100},
				{bee("forest"), BeeLifeStage.PRINCESS, 500},
				{bee("forest"), BeeLifeStage.LARVAE, 300},
				{trees.getDefaultSpecies(), TreeLifeStage.SAPLING, 100},
				{trees.getDefaultSpecies(), TreeLifeStage.POLLEN, 400},
		};

		int[] index = {0};
		int[] lastAmount = {0};
		Runnable feed = () -> {
			Object[] c = cases[index[0]];
			ItemStack stack = ((ISpecies<?>) c[0]).createStack((ILifeStage) c[1]);
			be.getInternalInventory().setItem(ProducerInventory.SLOT_INPUT, stack);
			be.getInternalInventory().setItem(ProducerInventory.SLOT_LABWARE, labware(16));
		};
		feed.run();

		helper.onEachTick(() -> {
			if (index[0] >= cases.length) return;
			int expected = (Integer) cases[index[0]][2];
			int amount = be.getTankManager().getFluidInTank(0).getAmount();
			if (amount - lastAmount[0] == expected && be.getInternalInventory().getItem(ProducerInventory.SLOT_INPUT).isEmpty()) {
				lastAmount[0] = amount;
				index[0]++;
				if (index[0] < cases.length) feed.run();
			}
		});
		helper.succeedWhen(() -> helper.assertTrue(index[0] >= cases.length, "Only completed " + index[0] + " of " + cases.length + " life stages; tank " + be.getTankManager().getFluidInTank(0).getAmount()));
	}

	// ---- advanced mutatron

	@GameTest(template = EMPTY, timeoutTicks = 800)
	public static void advancedMutatronWaitsForSelection(GameTestHelper helper) {
		AdvancedMutatronBlockEntity be = place(helper, GendustryMachineType.ADVANCED_MUTATRON);
		keepPowered(helper, be);
		ISpeciesType<?, ?> type = beeType();
		ISpecies<?> forest = bee("forest");
		ISpecies<?> marshy = bee("marshy");
		ItemStack primary = forest.createStack(type.getTypeForMutation(0));
		ItemStack secondary = marshy.createStack(type.getTypeForMutation(1));
		be.getInternalInventory().setItem(MutatronInventory.SLOT_PRIMARY, primary);
		be.getInternalInventory().setItem(MutatronInventory.SLOT_SECONDARY, secondary);
		be.getInternalInventory().setItem(MutatronInventory.SLOT_LABWARE, labware(2));
		be.getTankManager().fill(new FluidStack(GFluids.MUTAGEN.fluid(), 2000), IFluidHandler.FluidAction.EXECUTE);

		boolean[] selected = {false};
		helper.onEachTick(() -> {
			if (be.getPossibilities().isEmpty()) return;
			if (!selected[0]) {
				// before a choice is made nothing may be produced
				helper.assertTrue(be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT).isEmpty(), "Advanced mutatron mutated before a selection was made");
			}
		});

		helper.runAfterDelay(60, () -> {
			helper.assertFalse(be.getPossibilities().isEmpty(), "No mutation possibilities were found for forest + marshy");
			helper.assertTrue(be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT).isEmpty(), "Mutated without a selection");
			IMutation<?> choice = be.getPossibilities().get(0);
			selected[0] = true;
			be.setCurrentMutation(choice, be.getInternalInventory().getItem(MutatronInventory.SLOT_PRIMARY), be.getInternalInventory().getItem(MutatronInventory.SLOT_SECONDARY));
		});

		helper.succeedWhen(() -> {
			ItemStack out = be.getInternalInventory().getItem(MutatronInventory.SLOT_RESULT);
			IIndividual individual = IIndividualHandlerItem.getIndividual(out);
			helper.assertTrue(individual != null, "No result after selecting a mutation");
			helper.assertTrue(individual.getSpecies().id().equals(be.getPossibilities().get(0).getResult().id()) || individual.getSpecies().id().equals(ForestryConstants.forestry("common")), "Unexpected result " + individual.getSpecies().id());
		});
	}

	// ---- industrial apiary

	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.UP, Direction.DOWN};

	@GameTest(template = EMPTY)
	public static void industrialApiaryExtractsFromEveryOutputSlot(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = place(helper, GendustryMachineType.INDUSTRIAL_APIARY);
		IndustrialApiaryInventory inventory = (IndustrialApiaryInventory) be.getInternalInventory();
		List<String> problems = new ArrayList<>();
		for (int i = 0; i < IndustrialApiaryInventory.OUTPUT_SLOT_COUNT; i++) {
			int slot = IndustrialApiaryInventory.OUTPUT_SLOT_START + i;
			for (Direction side : SIDES) {
				if (!inventory.canTakeItemThroughFace(slot, new ItemStack(Items.HONEYCOMB), side)) {
					problems.add("output slot " + slot + " cannot be extracted from the " + side + " face");
					break;
				}
			}
		}
		for (int slot : new int[]{IndustrialApiaryInventory.QUEEN, IndustrialApiaryInventory.DRONE, IndustrialApiaryInventory.UPGRADE_SLOT_START}) {
			if (inventory.canTakeItemThroughFace(slot, new ItemStack(Items.HONEYCOMB), Direction.DOWN)) problems.add("non-output slot " + slot + " can be emptied by automation");
		}
		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void industrialApiarySlotFilters(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = place(helper, GendustryMachineType.INDUSTRIAL_APIARY);
		IndustrialApiaryInventory inv = (IndustrialApiaryInventory) be.getInternalInventory();
		ISpecies<?> forest = bee("forest");
		helper.assertTrue(inv.canSlotAccept(IndustrialApiaryInventory.QUEEN, forest.createStack(BeeLifeStage.PRINCESS)), "Princess rejected");
		helper.assertTrue(inv.canSlotAccept(IndustrialApiaryInventory.QUEEN, forest.createStack(BeeLifeStage.QUEEN)), "Queen rejected");
		helper.assertFalse(inv.canSlotAccept(IndustrialApiaryInventory.QUEEN, forest.createStack(BeeLifeStage.DRONE)), "Drone accepted in the queen slot");
		helper.assertTrue(inv.canSlotAccept(IndustrialApiaryInventory.DRONE, forest.createStack(BeeLifeStage.DRONE)), "Drone rejected");
		helper.assertFalse(inv.canSlotAccept(IndustrialApiaryInventory.DRONE, new ItemStack(Items.DIRT)), "Dirt accepted in the drone slot");

		ItemStack heater = new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.HEATER));
		helper.assertTrue(inv.canSlotAccept(IndustrialApiaryInventory.UPGRADE_SLOT_START, heater), "Upgrade rejected");
		inv.setItem(IndustrialApiaryInventory.UPGRADE_SLOT_START, heater);
		helper.assertFalse(inv.canSlotAccept(IndustrialApiaryInventory.UPGRADE_SLOT_START + 1, heater.copy()), "Same upgrade accepted in two slots");
		helper.assertFalse(inv.canSlotAccept(IndustrialApiaryInventory.UPGRADE_SLOT_START + 1, new ItemStack(Items.DIRT)), "Dirt accepted as an upgrade");
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void everyUpgradeAppliesItsEnergyCost(GameTestHelper helper) throws Exception {
		IndustrialApiaryBlockEntity be = place(helper, GendustryMachineType.INDUSTRIAL_APIARY);
		IndustrialApiaryInventory inv = (IndustrialApiaryInventory) be.getInternalInventory();
		Field energyField = IndustrialApiaryBlockEntity.class.getDeclaredField("energyConsumption");
		energyField.setAccessible(true);
		List<String> problems = new ArrayList<>();

		for (GendustryUpgradeType type : GendustryUpgradeType.values()) {
			inv.setItem(IndustrialApiaryInventory.UPGRADE_SLOT_START, new ItemStack(GItems.UPGRADE.item(type)));
			be.setChanged();
			int expected = IndustrialApiaryBlockEntity.BASE_ENERGY + type.energyCost();
			int actual = energyField.getInt(be);
			if (actual != expected) problems.add(type + " costs " + actual + ", expected " + expected);
		}
		for (EliteGendustryUpgradeType type : EliteGendustryUpgradeType.values()) {
			inv.setItem(IndustrialApiaryInventory.UPGRADE_SLOT_START, new ItemStack(GItems.ELITE_UPGRADE.item(type)));
			be.setChanged();
			int expected = IndustrialApiaryBlockEntity.BASE_ENERGY + type.energyCost();
			int actual = energyField.getInt(be);
			if (actual != expected) problems.add(type + " costs " + actual + ", expected " + expected);
		}
		inv.setItem(IndustrialApiaryInventory.UPGRADE_SLOT_START, ItemStack.EMPTY);
		be.setChanged();
		if (energyField.getInt(be) != IndustrialApiaryBlockEntity.BASE_ENERGY) problems.add("energy did not return to base after removing the upgrade");

		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	/** A mated queen in a powered apiary should run for a while without errors and actually use power. */
	@GameTest(template = EMPTY, timeoutTicks = 600)
	public static void industrialApiaryRunsWithAQueen(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = place(helper, GendustryMachineType.INDUSTRIAL_APIARY);
		// bees refuse to work without flowers in range
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 9; z++) {
				helper.setBlock(new BlockPos(x, 0, z), net.minecraft.world.level.block.Blocks.GRASS_BLOCK);
				if ((x + z) % 2 == 0 && !(x == POS.getX() && z == POS.getZ())) helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.POPPY);
			}
		}
		ISpecies<?> forest = bee("forest");
		IBee queen = (IBee) forest.createIndividual();
		queen.setMate(forest.getDefaultGenome());
		be.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, queen.createStack(BeeLifeStage.QUEEN));
		be.getEnergyManager().setEnergyStored(be.getEnergyManager().getMaxEnergyStored());

		helper.runAfterDelay(300, () -> {
			ItemStack queenSlot = be.getInternalInventory().getItem(IndustrialApiaryInventory.QUEEN);
			helper.assertFalse(queenSlot.isEmpty(), "Queen vanished within 300 ticks");
			helper.assertValueEqual(IIndividualHandlerItem.getLifeStage(queenSlot), BeeLifeStage.QUEEN, "queen life stage");
			helper.assertTrue(be.getEnergyManager().getEnergyStored() < be.getEnergyManager().getMaxEnergyStored(), "The apiary never used any power; errors: " + java.util.Arrays.toString(be.getErrorLogic().toArray()));
			helper.succeed();
		});
	}
}
