package thedarkcolour.gendustry.gametest;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;

import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import forestry.api.apiculture.genetics.BeeLifeStage;
import forestry.core.platform.registration.FeatureFluid;

import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.recipe.cache.DnaRecipeCache;
import thedarkcolour.gendustry.recipe.cache.MutagenRecipeCache;
import thedarkcolour.gendustry.recipe.cache.ProteinRecipeCache;
import thedarkcolour.gendustry.registry.GBlocks;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GRecipeTypes;

/**
 * Load-time checks: everything registers, every recipe resolves, every machine places, saves and opens.
 */
@GameTestHolder(Gendustry.ID)
@PrefixGameTestTemplate(false)
public class SmokeGameTests {
	private static final String EMPTY = "empty";

	@GameTest(template = EMPTY)
	public static void machinesRegistered(GameTestHelper helper) {
		for (GendustryMachineType type : GendustryMachineType.values()) {
			Block block = GBlocks.MACHINE.get(type).block();
			helper.assertTrue(block != null, "No block for " + type);
			helper.assertTrue(BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Gendustry.ID), "Wrong namespace for " + type);
			Item item = block.asItem();
			helper.assertTrue(item != Items.AIR, "No block item for " + type);
			helper.assertTrue(BuiltInRegistries.ITEM.getKey(item).equals(BuiltInRegistries.BLOCK.getKey(block)), "Item id mismatch for " + type);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void fluidsRegistered(GameTestHelper helper) {
		for (GFluids gFluid : GFluids.values()) {
			Fluid source = gFluid.fluid();
			Fluid flowing = gFluid.getFlowing();
			helper.assertTrue(source != null && flowing != null, "Missing fluid for " + gFluid);
			helper.assertTrue(source.isSource(source.defaultFluidState()), gFluid + " source is not a source");
			helper.assertFalse(flowing.isSource(flowing.defaultFluidState()), gFluid + " flowing is a source");
			helper.assertTrue(source.getFluidType() instanceof FeatureFluid.ForestryFluidType, gFluid + " has the wrong fluid type");
			var type = (FeatureFluid.ForestryFluidType) source.getFluidType();
			helper.assertTrue(type.getStillTexture() != null, gFluid + " has no still texture");
			helper.assertTrue(type.getFlowingTexture() != null, gFluid + " has no flowing texture");

			BucketItem bucket = gFluid.getBucket();
			helper.assertTrue(bucket != null, "Missing bucket for " + gFluid);
			FluidStack contained = FluidUtil.getFluidContained(new ItemStack(bucket)).orElse(FluidStack.EMPTY);
			helper.assertTrue(contained.getFluid() == source && contained.getAmount() == 1000, "Bucket of " + gFluid + " contains " + contained);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY)
	public static void recipeTypesLoaded(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		helper.assertValueEqual(count(level, GRecipeTypes.MUTAGEN.type()), 4, "mutagen recipe count");
		helper.assertValueEqual(count(level, GRecipeTypes.PROTEIN.type()), 7, "protein recipe count");
		// 6 non-butterfly stages (drone, princess, queen, larvae, sapling, pollen), plus 4 when the butterfly add-on is present
		boolean butterflies = net.neoforged.fml.ModList.get().isLoaded("forestrybutterflies");
		helper.assertValueEqual(count(level, GRecipeTypes.DNA.type()), butterflies ? 10 : 6, "dna recipe count (butterflies loaded: " + butterflies + ")");
		helper.succeed();
	}

	private static int count(ServerLevel level, RecipeType<?> type) {
		return (int) level.getRecipeManager().getRecipes().stream().filter(holder -> holder.value().getType() == type).count();
	}

	@GameTest(template = EMPTY)
	public static void recipeCachesResolve(GameTestHelper helper) {
		var redstone = MutagenRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.REDSTONE));
		helper.assertTrue(redstone != null && redstone.getAmount() == 100, "Redstone mutagen recipe");
		var glowstone = MutagenRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.GLOWSTONE));
		helper.assertTrue(glowstone != null && glowstone.getAmount() == 800, "Glowstone mutagen recipe");
		helper.assertTrue(MutagenRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.DIRT)) == null, "Dirt should not make mutagen");

		var pork = ProteinRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.PORKCHOP));
		helper.assertTrue(pork != null && pork.getAmount() == 500, "Porkchop protein recipe");
		var fish = ProteinRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.SALMON));
		helper.assertTrue(fish != null && fish.getAmount() == 250, "Salmon protein recipe");
		helper.assertTrue(ProteinRecipeCache.INSTANCE.getRecipe(new ItemStack(Items.DIRT)) == null, "Dirt should not make protein");

		var drone = DnaRecipeCache.INSTANCE.getRecipe(BeeLifeStage.DRONE);
		helper.assertTrue(drone != null && drone.getAmount() == 100, "Drone DNA recipe");
		var queen = DnaRecipeCache.INSTANCE.getRecipe(BeeLifeStage.QUEEN);
		helper.assertTrue(queen != null && queen.getAmount() == 600, "Queen DNA recipe");
		helper.succeed();
	}

	/** Every crafting recipe must have real ingredients and an output, and must be craftable from its own ingredients. */
	@GameTest(template = EMPTY)
	public static void craftingRecipesAreCraftable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		List<String> problems = new ArrayList<>();
		int checked = 0;

		for (RecipeHolder<?> holder : level.getRecipeManager().getRecipes()) {
			if (!holder.id().getNamespace().equals(Gendustry.ID) || !(holder.value() instanceof CraftingRecipe recipe)) continue;
			// the genetic template recipe is special and needs inputs with data
			if (holder.id().getPath().contains("genetic_template") && !holder.id().getPath().contains("blank")) continue;
			checked++;

			NonNullList<Ingredient> ingredients = recipe.getIngredients();
			if (ingredients.isEmpty()) problems.add(holder.id() + ": no ingredients");

			List<ItemStack> stacks = new ArrayList<>();
			for (Ingredient ingredient : ingredients) {
				if (ingredient.isEmpty()) {
					stacks.add(ItemStack.EMPTY);
					continue;
				}
				ItemStack[] options = ingredient.getItems();
				if (options.length == 0) {
					problems.add(holder.id() + ": ingredient matches nothing (missing tag/item?)");
					continue;
				}
				stacks.add(options[0]);
			}

			ItemStack result = recipe.getResultItem(level.registryAccess());
			if (result.isEmpty()) problems.add(holder.id() + ": empty result");

			// shaped recipes list ingredients in grid order, so they can be matched back in a rectangle
			if (recipe instanceof net.minecraft.world.item.crafting.ShapedRecipe shaped && stacks.size() == shaped.getWidth() * shaped.getHeight()) {
				CraftingInput input = CraftingInput.of(shaped.getWidth(), shaped.getHeight(), stacks);
				if (!recipe.matches(input, level)) problems.add(holder.id() + ": does not match its own ingredients");
				else if (!ItemStack.isSameItemSameComponents(recipe.assemble(input, level.registryAccess()), result)) problems.add(holder.id() + ": assembles to a different result");
			}
		}

		helper.assertTrue(checked >= 40, "Only found " + checked + " gendustry crafting recipes");
		if (!problems.isEmpty()) {
			helper.fail(String.join("; ", problems));
			return;
		}
		helper.succeed();
	}

	/** Place every machine, read/write its NBT, and open its menu as a player. */
	@GameTest(template = EMPTY)
	public static void machinesPlaceSaveAndOpen(GameTestHelper helper) {
		// A FakePlayer avoids the mock player's connection, which rejects Forestry's login sync payload
		ServerPlayer player = FakePlayerFactory.getMinecraft(helper.getLevel());
		BlockPos pos = new BlockPos(1, 1, 1);
		List<String> problems = new ArrayList<>();

		for (GendustryMachineType type : GendustryMachineType.values()) {
			helper.setBlock(pos, GBlocks.MACHINE.get(type).block().defaultBlockState());
			BlockEntity be = helper.getBlockEntity(pos);
			if (be == null) {
				problems.add(type + ": no block entity");
				continue;
			}
			CompoundTag tag = be.saveWithFullMetadata(helper.getLevel().registryAccess());
			try {
				be.loadCustomOnly(tag, helper.getLevel().registryAccess());
			} catch (Exception e) {
				problems.add(type + ": NBT round trip failed: " + e);
			}
			if (!(be instanceof MenuProvider provider)) {
				problems.add(type + ": block entity is not a MenuProvider");
				continue;
			}
			try {
				AbstractContainerMenu menu = provider.createMenu(1, player.getInventory(), player);
				if (menu == null) problems.add(type + ": createMenu returned null");
				else if (menu.slots.isEmpty()) problems.add(type + ": menu has no slots");
			} catch (Exception e) {
				problems.add(type + ": createMenu threw " + e);
			}
			helper.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR);
		}

		if (!problems.isEmpty()) {
			helper.fail(String.join("; ", problems));
			return;
		}
		helper.succeed();
	}

	/** Every fluid places as a source block and spreads outward, compared against vanilla water and Forestry's own ethanol. */
	@GameTest(template = EMPTY, timeoutTicks = 400)
	public static void fluidsPlaceAndFlow(GameTestHelper helper) {
		List<String> labels = new ArrayList<>();
		List<Fluid> sources = new ArrayList<>();
		List<Fluid> flowings = new ArrayList<>();
		labels.add("water (control)");
		sources.add(net.minecraft.world.level.material.Fluids.WATER);
		flowings.add(net.minecraft.world.level.material.Fluids.FLOWING_WATER);
		Fluid ethanol = BuiltInRegistries.FLUID.get(forestry.api.ForestryConstants.forestry("ethanol"));
		Fluid flowingEthanol = BuiltInRegistries.FLUID.get(forestry.api.ForestryConstants.forestry("ethanol_flowing"));
		if (ethanol != net.minecraft.world.level.material.Fluids.EMPTY) {
			labels.add("forestry ethanol (control)");
			sources.add(ethanol);
			flowings.add(flowingEthanol);
		}
		for (GFluids g : GFluids.values()) {
			labels.add(g.name());
			sources.add(g.fluid());
			flowings.add(g.getFlowing());
		}

		// a stone floor and a lane per fluid so they don't interfere
		for (int x = 0; x < 9; x++) {
			for (int z = 0; z < 9; z++) {
				helper.setBlock(new BlockPos(x, 1, z), net.minecraft.world.level.block.Blocks.STONE);
			}
		}
		for (int i = 0; i < sources.size(); i++) {
			BlockPos spot = new BlockPos(1, 2, 1 + i);
			helper.setBlock(spot, sources.get(i).defaultFluidState().createLegacyBlock());
			helper.assertTrue(helper.getLevel().getFluidState(helper.absolutePos(spot)).isSource(), labels.get(i) + " did not place as a source");
		}
		helper.runAfterDelay(300, () -> {
			List<String> problems = new ArrayList<>();
			for (int i = 0; i < sources.size(); i++) {
				BlockPos beside = new BlockPos(2, 2, 1 + i);
				Fluid found = helper.getLevel().getFluidState(helper.absolutePos(beside)).getType();
				if (found != flowings.get(i) && found != sources.get(i)) problems.add(labels.get(i) + " did not flow, found " + BuiltInRegistries.FLUID.getKey(found));
			}
			if (!problems.isEmpty()) {
				helper.fail(String.join("; ", problems));
				return;
			}
			helper.succeed();
		});
	}
}
