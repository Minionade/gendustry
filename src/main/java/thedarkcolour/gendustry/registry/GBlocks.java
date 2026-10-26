package thedarkcolour.gendustry.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;

import forestry.core.platform.registration.FeatureBlockGroup;
import forestry.core.platform.registration.FeatureProvider;
import forestry.core.platform.registration.IFeatureRegistry;
import forestry.core.platform.registration.ModFeatureRegistry;

import thedarkcolour.gendustry.GendustryModule;
import thedarkcolour.gendustry.block.GendustryMachineBlock;
import thedarkcolour.gendustry.block.GendustryMachineType;

@FeatureProvider
public class GBlocks {
	private static final IFeatureRegistry REGISTRY = ModFeatureRegistry.get(GendustryModule.MODULE_ID);

	public static final FeatureBlockGroup<GendustryMachineBlock, GendustryMachineType> MACHINE = REGISTRY
			.blockGroup(GendustryMachineBlock::new, GendustryMachineType.values())
			.item(b -> new BlockItem(b, new Item.Properties()))
			.create();
}
