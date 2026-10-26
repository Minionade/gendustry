package thedarkcolour.gendustry.registry;

import net.minecraft.world.item.CreativeModeTab;

import forestry.api.IForestryApi;
import forestry.api.core.genetics.ISpeciesType;
import forestry.api.core.genetics.alleles.Allele;
import forestry.api.core.genetics.alleles.IChromosome;
import forestry.api.core.genetics.alleles.IKaryotype;
import forestry.core.platform.tab.ForestryCreativeTabs;
import forestry.core.platform.registration.FeatureCreativeTab;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import thedarkcolour.gendustry.GendustryModule;
import thedarkcolour.gendustry.block.GendustryMachineType;
import thedarkcolour.gendustry.item.GeneSampleItem;

@FeatureProvider
public class GCreativeTabs {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(GendustryModule.MODULE_ID);

	public static final FeatureCreativeTab GENDUSTRY = REGISTRY.creativeTab("gendustry", tab -> {
		tab.icon(() -> GBlocks.MACHINE.stack(GendustryMachineType.INDUSTRIAL_APIARY));
		tab.displayItems(GCreativeTabs::addGendustryItems);
		tab.withTabsBefore(ForestryCreativeTabs.MAIL.getKey());
		tab.withTabsAfter(GCreativeTabs.GENE_SAMPLES.getKey());
	});

	public static final FeatureCreativeTab GENE_SAMPLES = REGISTRY.creativeTab("gene_samples", tab -> {
		tab.icon(GItems.GENE_SAMPLE::stack);
		tab.displayItems(GCreativeTabs::addGeneSamples);
		tab.withTabsBefore(GCreativeTabs.GENDUSTRY.getKey());
	});

	private static void addGendustryItems(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		GBlocks.MACHINE.getBlocks().forEach(items::accept);
		items.accept(GItems.POLLEN_KIT);
		items.accept(GFluids.MUTAGEN.getBucket());
		items.accept(GFluids.LIQUID_DNA.getBucket());
		items.accept(GFluids.PROTEIN.getBucket());
		GItems.RESOURCE.getItems().forEach(items::accept);
		GItems.UPGRADE.getItems().forEach(items::accept);
		GItems.ELITE_UPGRADE.getItems().forEach(items::accept);
	}

	private static void addGeneSamples(CreativeModeTab.ItemDisplayParameters params, CreativeModeTab.Output items) {
		for (ISpeciesType<?, ?> speciesType : IForestryApi.INSTANCE.getGeneticManager().getSpeciesTypes()) {
			IKaryotype karyotype = speciesType.getKaryotype();

			for (IChromosome<?> chromosome : karyotype.getChromosomes()) {
				items.accept(GeneSampleItem.createStack(speciesType, chromosome, karyotype.getDefaultAllele(chromosome)));
			}
		}
	}
}
