package thedarkcolour.gendustry;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.bus.api.IEventBus;

import forestry.api.client.IClientModuleHandler;
import forestry.api.modules.ForestryModule;
import forestry.api.modules.IForestryModule;
import forestry.core.platform.tile.TilePowered;

import thedarkcolour.gendustry.client.ClientHandler;
import thedarkcolour.gendustry.data.Data;
import thedarkcolour.gendustry.recipe.cache.DnaRecipeCache;
import thedarkcolour.gendustry.recipe.cache.MutagenRecipeCache;
import thedarkcolour.gendustry.recipe.cache.ProteinRecipeCache;
import thedarkcolour.gendustry.recipe.cache.RecipeCacheRegistry;
import thedarkcolour.gendustry.registry.GBlockEntities;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryBlockEntity;
import thedarkcolour.gendustry.blockentity.PoweredTankBlockEntity;

@ForestryModule
public class GendustryModule implements IForestryModule {
	public static final ResourceLocation MODULE_ID = Gendustry.loc("core");

	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(GendustryModule::registerCapabilities);
		// Recipe caching
		new RecipeCacheRegistry(registrar -> {
			registrar.accept(MutagenRecipeCache.INSTANCE);
			registrar.accept(DnaRecipeCache.INSTANCE);
			registrar.accept(ProteinRecipeCache.INSTANCE);
		});

		try {
			modBus.addListener(Data::gatherData);
		} catch (ClassCastException ignored) {
		}
	}

	private static void registerCapabilities(RegisterCapabilitiesEvent event) {
		registerPowered(event, GBlockEntities.MUTAGEN_PRODUCER.tileType());
		registerPowered(event, GBlockEntities.DNA_EXTRACTOR.tileType());
		registerPowered(event, GBlockEntities.PROTEIN_LIQUEFIER.tileType());
		registerPowered(event, GBlockEntities.SAMPLER.tileType());
		registerPowered(event, GBlockEntities.MUTATRON.tileType());
		registerPowered(event, GBlockEntities.ADVANCED_MUTATRON.tileType());
		registerPowered(event, GBlockEntities.IMPRINTER.tileType());
		registerPowered(event, GBlockEntities.GENETIC_TRANSPOSER.tileType());
		registerPowered(event, GBlockEntities.REPLICATOR.tileType());
		registerFluid(event, GBlockEntities.MUTAGEN_PRODUCER.tileType());
		registerFluid(event, GBlockEntities.DNA_EXTRACTOR.tileType());
		registerFluid(event, GBlockEntities.PROTEIN_LIQUEFIER.tileType());
		registerFluid(event, GBlockEntities.MUTATRON.tileType());
		registerFluid(event, GBlockEntities.ADVANCED_MUTATRON.tileType());
		registerFluid(event, GBlockEntities.REPLICATOR.tileType());
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GBlockEntities.INDUSTRIAL_APIARY.tileType(), (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, GBlockEntities.INDUSTRIAL_APIARY.tileType(), IndustrialApiaryBlockEntity::getEnergyHandler);
	}

	private static <T extends TilePowered> void registerPowered(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (tile, side) -> tile.getItemHandler(side));
		event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (tile, side) -> tile.getEnergyHandler(side));
	}

	private static <T extends PoweredTankBlockEntity> void registerFluid(RegisterCapabilitiesEvent event, BlockEntityType<T> type) {
		event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, (tile, side) -> tile.getTankManager());
	}

	@Override
	public ResourceLocation getId() {
		return GendustryModule.MODULE_ID;
	}

	@Override
	public void registerClientHandler(Consumer<IClientModuleHandler> registrar) {
		registrar.accept(new ClientHandler());
	}
}
