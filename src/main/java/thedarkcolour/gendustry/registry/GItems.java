package thedarkcolour.gendustry.registry;

import net.minecraft.world.item.Item;

import forestry.core.platform.item.ItemForestry;
import forestry.core.platform.registration.FeatureGroup;
import forestry.core.platform.registration.FeatureItem;
import forestry.core.platform.registration.FeatureItemGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import thedarkcolour.gendustry.GendustryModule;
import thedarkcolour.gendustry.item.EliteGendustryUpgradeType;
import thedarkcolour.gendustry.item.GendustryResourceType;
import thedarkcolour.gendustry.item.GendustryUpgradeItem;
import thedarkcolour.gendustry.item.GendustryUpgradeType;
import thedarkcolour.gendustry.item.GeneSampleItem;
import thedarkcolour.gendustry.item.GeneticTemplateItem;
import thedarkcolour.gendustry.item.PollenKitItem;

@FeatureProvider
public class GItems {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(GendustryModule.MODULE_ID);

	public static final FeatureItemGroup<ItemForestry, GendustryResourceType> RESOURCE = REGISTRY.itemGroup(subtype -> new ItemForestry(), GendustryResourceType.values()).create();
	public static final FeatureItemGroup<GendustryUpgradeItem, GendustryUpgradeType> UPGRADE = REGISTRY.itemGroup(GendustryUpgradeItem::new, GendustryUpgradeType.values()).identifier("upgrade", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureItemGroup<GendustryUpgradeItem, EliteGendustryUpgradeType> ELITE_UPGRADE = REGISTRY.itemGroup(GendustryUpgradeItem::new, EliteGendustryUpgradeType.values()).identifier("elite_upgrade", FeatureGroup.IdentifierType.SUFFIX).create();
	public static final FeatureItem<Item> POLLEN_KIT = REGISTRY.item(PollenKitItem::new, "pollen_kit");
	public static final FeatureItem<Item> GENE_SAMPLE = REGISTRY.item(GeneSampleItem::new, "gene_sample");
	public static final FeatureItem<Item> GENETIC_TEMPLATE = REGISTRY.item(GeneticTemplateItem::new, "genetic_template");
	//public static final FeatureItem<DebugWand> DEBUG_WAND = REGISTRY.item(DebugWand::new, "debug_wand");
	//public static final FeatureItem<Item> INDUSTRIAL_SCOOP = REGISTRY.item(() -> new IndustrialScoopItem(), "industrial_scoop");
	//public static final FeatureItem<Item> INDUSTRIAL_GRAFTER = REGISTRY.item(() -> new IndustrialGrafterItem(), "industrial_grafter");
}
