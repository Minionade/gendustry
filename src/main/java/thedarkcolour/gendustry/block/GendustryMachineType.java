package thedarkcolour.gendustry.block;

import java.util.Locale;

import forestry.core.platform.block.IBlockType;
import forestry.core.platform.block.IMachineProperties;
import forestry.core.platform.block.MachineProperties;
import forestry.core.platform.tile.IForestryTicker;
import forestry.core.platform.tile.TileForestry;
import forestry.core.platform.registration.FeatureTileType;

import thedarkcolour.gendustry.blockentity.AdvancedMutatronBlockEntity;
import thedarkcolour.gendustry.blockentity.DnaExtractorBlockEntity;
import thedarkcolour.gendustry.blockentity.GeneticTransposerBlockEntity;
import thedarkcolour.gendustry.blockentity.ImprinterBlockEntity;
import thedarkcolour.gendustry.blockentity.IndustrialApiaryBlockEntity;
import thedarkcolour.gendustry.blockentity.MutagenProducerBlockEntity;
import thedarkcolour.gendustry.blockentity.MutatronBlockEntity;
import thedarkcolour.gendustry.blockentity.ProteinLiquefierBlockEntity;
import thedarkcolour.gendustry.blockentity.ReplicatorBlockEntity;
import thedarkcolour.gendustry.blockentity.SamplerBlockEntity;
import thedarkcolour.gendustry.registry.GBlockEntities;

public enum GendustryMachineType implements IBlockType {
	INDUSTRIAL_APIARY(GBlockEntities.INDUSTRIAL_APIARY, IndustrialApiaryBlockEntity::serverTick, IndustrialApiaryBlockEntity::clientTick),
	MUTAGEN_PRODUCER(GBlockEntities.MUTAGEN_PRODUCER, MutagenProducerBlockEntity::serverTick),
	DNA_EXTRACTOR(GBlockEntities.DNA_EXTRACTOR, DnaExtractorBlockEntity::serverTick),
	PROTEIN_LIQUEFIER(GBlockEntities.PROTEIN_LIQUEFIER, ProteinLiquefierBlockEntity::serverTick),
	SAMPLER(GBlockEntities.SAMPLER, SamplerBlockEntity::serverTick),
	MUTATRON(GBlockEntities.MUTATRON, MutatronBlockEntity::serverTick),
	ADVANCED_MUTATRON(GBlockEntities.ADVANCED_MUTATRON, AdvancedMutatronBlockEntity::serverTick),
	IMPRINTER(GBlockEntities.IMPRINTER, ImprinterBlockEntity::serverTick),
	GENETIC_TRANSPOSER(GBlockEntities.GENETIC_TRANSPOSER, GeneticTransposerBlockEntity::serverTick),
	REPLICATOR(GBlockEntities.REPLICATOR, ReplicatorBlockEntity::serverTick);

	private final IMachineProperties<?> properties;

	<T extends TileForestry> GendustryMachineType(FeatureTileType<T> teClass, IForestryTicker<T> serverTicker) {
		String name = name().toLowerCase(Locale.ENGLISH);

		this.properties = new MachineProperties.Builder<>(teClass, name)
				.setServerTicker(serverTicker)
				.create();
	}

	<T extends TileForestry> GendustryMachineType(FeatureTileType<T> teClass, IForestryTicker<T> serverTicker, IForestryTicker<T> clientTicker) {
		String name = name().toLowerCase(Locale.ENGLISH);

		this.properties = new MachineProperties.Builder<>(teClass, name)
				.setServerTicker(serverTicker)
				.setClientTicker(clientTicker)
				.create();
	}

	@Override
	public IMachineProperties<?> getMachineProperties() {
		return this.properties;
	}

	@Override
	public String getSerializedName() {
		return this.properties.getSerializedName();
	}
}
