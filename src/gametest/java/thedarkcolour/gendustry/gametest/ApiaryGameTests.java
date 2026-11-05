package thedarkcolour.gendustry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.ForestryConstants;
import forestry.api.IForestryApi;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.api.apiculture.genetics.IBee;
import forestry.api.core.TemperatureType;
import forestry.api.core.genetics.ForestrySpeciesTypes;
import forestry.api.core.genetics.IGenome;
import forestry.api.core.genetics.ISpecies;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.capability.IIndividualHandlerItem;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryBlockEntity;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryInventory;
import thedarkcolour.gendustry.item.EliteGendustryUpgradeType;
import thedarkcolour.gendustry.item.GendustryUpgradeType;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GItems;

/**
 * The Industrial Apiary running real Forestry bee logic: production, queen lifecycle, automation, upgrades and mutation.
 */
@GameTestHolder(Gendustry.ID)
@PrefixGameTestTemplate(false)
public class ApiaryGameTests {
	private static final String EMPTY = "empty";
	private static final BlockPos POS = new BlockPos(4, 1, 4);

	// ---- helpers

	private static IndustrialApiaryBlockEntity placeApiary(GameTestHelper helper, boolean meadow) {
		if (meadow) {
			// bees refuse to work without flowers in range
			for (int x = 0; x < 9; x++) {
				for (int z = 0; z < 9; z++) {
					helper.setBlock(new BlockPos(x, 0, z), Blocks.GRASS_BLOCK);
					if ((x + z) % 2 == 0 && !(x == POS.getX() && z == POS.getZ())) helper.setBlock(new BlockPos(x, 1, z), Blocks.POPPY);
				}
			}
		}
		helper.setBlock(POS, GBlocks.MACHINE.get(GendustryMachineType.INDUSTRIAL_APIARY).block().defaultBlockState());
		IndustrialApiaryBlockEntity be = (IndustrialApiaryBlockEntity) helper.getBlockEntity(POS);
		helper.assertTrue(be != null, "No industrial apiary block entity");
		return be;
	}

	private static void keepPowered(GameTestHelper helper, IndustrialApiaryBlockEntity be) {
		helper.onEachTick(() -> be.getEnergyManager().setEnergyStored(be.getEnergyManager().getMaxEnergyStored()));
	}

	private static ISpeciesType<?, ?> beeType() {
		return IForestryApi.INSTANCE.getGeneticManager().getSpeciesType(ForestrySpeciesTypes.BEE);
	}

	private static ISpecies<?> bee(String name) {
		return beeType().getSpecies(ForestryConstants.forestry(name));
	}

	private static ItemStack matedQueen(ISpecies<?> species, ISpecies<?> mate, int health) {
		IBee queen = (IBee) species.createIndividual();
		queen.setMate(mate.getDefaultGenome());
		queen.setHealth(health);
		return queen.createStack(BeeLifeStage.QUEEN);
	}

	private static void setUpgrade(IndustrialApiaryBlockEntity be, int slot, ItemStack upgrade) {
		be.getInternalInventory().setItem(IndustrialApiaryInventory.UPGRADE_SLOT_START + slot, upgrade);
		be.setChanged();
	}

	private static IBeeModifier modifier(IndustrialApiaryBlockEntity be) {
		return be.getBeeModifiers().iterator().next();
	}

	private static List<ItemStack> outputs(IndustrialApiaryBlockEntity be) {
		List<ItemStack> stacks = new ArrayList<>();
		for (int i = 0; i < IndustrialApiaryInventory.OUTPUT_SLOT_COUNT; i++) {
			ItemStack stack = be.getInternalInventory().getItem(IndustrialApiaryInventory.OUTPUT_SLOT_START + i);
			if (!stack.isEmpty()) stacks.add(stack);
		}
		return stacks;
	}

	private static boolean hasStage(List<ItemStack> stacks, BeeLifeStage stage) {
		return stacks.stream().anyMatch(stack -> IIndividualHandlerItem.getLifeStage(stack) == stage);
	}

	// ---- working

	@GameTest(template = EMPTY)
	public static void apiaryNeedsFlowers(GameTestHelper helper) {
		IndustrialApiaryBlockEntity bare = placeApiary(helper, false);
		bare.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, matedQueen(bee("forest"), bee("forest"), 20));
		helper.assertFalse(bare.getBeekeepingLogic().canWork(), "Bees can work with no flowers around");
		helper.succeed();
	}

	@GameTest(template = EMPTY, timeoutTicks = 200)
	public static void apiaryWorksOverFlowers(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, true);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, matedQueen(bee("forest"), bee("forest"), 20));
		helper.runAfterDelay(40, () -> {
			helper.assertTrue(be.getBeekeepingLogic().canWork(), "Bees cannot work even with flowers; errors " + java.util.Arrays.toString(be.getErrorLogic().toArray()));
			helper.assertFalse(be.getBeekeepingLogic().getFlowerPositions().isEmpty(), "Bees found no flowers");
			helper.succeed();
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 12000)
	public static void apiaryProducesProducts(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, true);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, matedQueen(bee("forest"), bee("forest"), 50));
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.PRODUCTIVITY), 4));

		helper.succeedWhen(() -> {
			boolean product = outputs(be).stream().anyMatch(stack -> IIndividualHandlerItem.getIndividual(stack) == null);
			helper.assertTrue(product, "No honeycomb or other product yet");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 20000)
	public static void queenDeathLeavesPrincessAndDrones(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, true);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, matedQueen(bee("forest"), bee("marshy"), 1));

		helper.succeedWhen(() -> {
			List<ItemStack> outs = outputs(be);
			helper.assertTrue(hasStage(outs, BeeLifeStage.PRINCESS) || IIndividualHandlerItem.getLifeStage(be.getInternalInventory().getItem(IndustrialApiaryInventory.QUEEN)) == BeeLifeStage.PRINCESS, "No princess after the queen died");
			helper.assertTrue(hasStage(outs, BeeLifeStage.DRONE), "No drones after the queen died");
		});
	}

	@GameTest(template = EMPTY, timeoutTicks = 30000)
	public static void automationUpgradeRestartsTheColony(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, true);
		keepPowered(helper, be);
		be.getInternalInventory().setItem(IndustrialApiaryInventory.QUEEN, matedQueen(bee("forest"), bee("marshy"), 1));
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.AUTOMATION)));

		boolean[] diedOnce = {false};
		helper.succeedWhen(() -> {
			ItemStack queenSlot = be.getInternalInventory().getItem(IndustrialApiaryInventory.QUEEN);
			IIndividualHandlerItem.ifPresent(queenSlot, (individual, stage) -> {
				// the first queen has 1 health, so a queen with more health is a new generation
				if (stage == BeeLifeStage.QUEEN && individual instanceof IBee bee && bee.getHealth() > 1) diedOnce[0] = true;
			});
			helper.assertTrue(diedOnce[0], "The colony was not restarted automatically");
			helper.assertFalse(hasStage(outputs(be), BeeLifeStage.PRINCESS), "A princess was left in the outputs instead of being recycled");
		});
	}

	// ---- upgrades

	@GameTest(template = EMPTY)
	public static void upgradesChangeBeeModifiers(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, false);
		IGenome genome = bee("forest").getDefaultGenome();
		IBeeModifier mod = modifier(be);
		List<String> problems = new ArrayList<>();

		check(problems, "baseline production", mod.modifyProductionSpeed(genome, 1f), 1f);
		check(problems, "baseline aging", mod.modifyAging(genome, null, 1f), 1f);
		check(problems, "baseline pollination", mod.modifyPollination(genome, 1f), 1f);
		check(problems, "baseline mutation", mod.modifyMutationChance(genome, genome, null, 1f), 1f);
		if (mod.isSealed() || mod.isHellish() || mod.isSunlightSimulated() || mod.isAlwaysActive(genome)) problems.add("baseline modifier has flags set");

		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.PRODUCTIVITY), 2));
		check(problems, "2x productivity", mod.modifyProductionSpeed(genome, 1f), 1.5f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.LIFESPAN), 1));
		check(problems, "lifespan", mod.modifyAging(genome, null, 1f), 3f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.POLLINATION), 2));
		check(problems, "2x pollination", mod.modifyPollination(genome, 1f), 1.5f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.SCRUBBER)));
		check(problems, "scrubber pollination", mod.modifyPollination(genome, 1f), 0f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.IMMUTABLE)));
		check(problems, "immutable mutation chance", mod.modifyMutationChance(genome, genome, null, 1f), 0f);
		setUpgrade(be, 0, new ItemStack(GItems.ELITE_UPGRADE.item(EliteGendustryUpgradeType.MUTATION)));
		check(problems, "elite mutation", mod.modifyMutationChance(genome, genome, null, 1f), 1.25f);
		setUpgrade(be, 0, new ItemStack(GItems.ELITE_UPGRADE.item(EliteGendustryUpgradeType.YOUTH)));
		check(problems, "elite youth", mod.modifyMutationChance(genome, genome, null, 1f), 0.8f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.STABILIZER)));
		check(problems, "stabilizer decay", mod.modifyGeneticDecay(genome, 1f), 0f);
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.TERRITORY), 4));
		Vec3i territory = mod.modifyTerritory(genome, new Vec3i(10, 10, 10));
		if (territory.getX() != 20) problems.add("4x territory gave " + territory + ", expected 20");

		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.WEATHERPROOF)));
		if (!mod.isSealed()) problems.add("weatherproof did not seal");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.SKY)));
		if (!mod.isSunlightSimulated()) problems.add("sky did not simulate sunlight");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.LIGHTING)));
		if (!mod.isAlwaysActive(genome)) problems.add("lighting did not keep bees always active");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.NETHER)));
		if (!mod.isHellish()) problems.add("nether upgrade is not hellish");
		setUpgrade(be, 0, new ItemStack(GItems.ELITE_UPGRADE.item(EliteGendustryUpgradeType.ACTIVITY_SIMULATOR)));
		if (!(mod.isSealed() && mod.isSunlightSimulated() && mod.isAlwaysActive(genome))) problems.add("activity simulator is missing one of its three effects");

		setUpgrade(be, 0, ItemStack.EMPTY);
		check(problems, "modifier after removing upgrades", mod.modifyProductionSpeed(genome, 1f), 1f);

		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	private static void check(List<String> problems, String what, float actual, float expected) {
		if (Math.abs(actual - expected) > 0.0001f) problems.add(what + " was " + actual + ", expected " + expected);
	}

	@GameTest(template = EMPTY)
	public static void climateUpgradesShiftClimate(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, false);
		List<String> problems = new ArrayList<>();
		int baseTemp = be.temperature().ordinal();
		int baseHumidity = be.humidity().ordinal();

		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.HEATER)));
		if (be.temperature().ordinal() <= baseTemp) problems.add("heater did not raise the temperature from " + be.temperature());
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.COOLER)));
		if (be.temperature().ordinal() >= baseTemp) problems.add("cooler did not lower the temperature");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.HUMIDIFIER)));
		if (be.humidity().ordinal() <= baseHumidity) problems.add("humidifier did not raise the humidity");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.DRYER)));
		if (be.humidity().ordinal() >= baseHumidity) problems.add("dryer did not lower the humidity");
		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.NETHER)));
		if (be.temperature() != TemperatureType.HELLISH && be.temperature().ordinal() <= baseTemp) problems.add("nether upgrade left the climate at " + be.temperature());

		if (!problems.isEmpty()) helper.fail(String.join("; ", problems));
		else helper.succeed();
	}

	// ---- mutation through the real Forestry breeding logic

	/** Breeds many princesses from forest + marshy queens and counts how often they mutate into Common. */
	private static int countMutations(IndustrialApiaryBlockEntity be, int trials) {
		ISpecies<?> forest = bee("forest");
		ISpecies<?> marshy = bee("marshy");
		int mutated = 0;
		for (int i = 0; i < trials; i++) {
			IBee queen = (IBee) forest.createIndividual();
			queen.setMate(marshy.getDefaultGenome());
			IBee princess = queen.spawnPrincess(be);
			String species = princess.getSpecies().id().getPath();
			String inactive = princess.getInactiveSpecies().id().getPath();
			if (!(species.equals("forest") || species.equals("marshy")) || !(inactive.equals("forest") || inactive.equals("marshy"))) mutated++;
		}
		return mutated;
	}

	@GameTest(template = EMPTY)
	public static void apiaryCanMutateUnlessImmutable(GameTestHelper helper) {
		IndustrialApiaryBlockEntity be = placeApiary(helper, false);
		int normal = countMutations(be, 600);
		helper.assertTrue(normal > 0, "forest + marshy never mutated in 600 breedings");

		setUpgrade(be, 0, new ItemStack(GItems.UPGRADE.item(GendustryUpgradeType.IMMUTABLE)));
		helper.assertValueEqual(countMutations(be, 600), 0, "mutations with the immutable upgrade");

		setUpgrade(be, 0, new ItemStack(GItems.ELITE_UPGRADE.item(EliteGendustryUpgradeType.MUTATION)));
		setUpgrade(be, 1, new ItemStack(GItems.ELITE_UPGRADE.item(EliteGendustryUpgradeType.YOUTH)));
		// mutation and youth cancel to 1.05x, so we only check it still mutates
		helper.assertTrue(countMutations(be, 600) > 0, "no mutations with mutation + youth upgrades");
		helper.succeed();
	}
}
