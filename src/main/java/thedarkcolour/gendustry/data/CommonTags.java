package thedarkcolour.gendustry.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

final class CommonTags {
	static final TagKey<Item> DUSTS_REDSTONE = item("dusts/redstone");
	static final TagKey<Item> ENDER_PEARLS = item("ender_pearls");
	static final TagKey<Item> GEMS_DIAMOND = item("gems/diamond");
	static final TagKey<Item> GEMS_EMERALD = item("gems/emerald");
	static final TagKey<Item> GEMS_LAPIS = item("gems/lapis");
	static final TagKey<Item> GEMS_QUARTZ = item("gems/quartz");
	static final TagKey<Item> GLASS = item("glass_blocks/colorless");
	static final TagKey<Item> GLASS_PANES = item("glass_panes");
	static final TagKey<Item> INGOTS_GOLD = item("ingots/gold");
	static final TagKey<Item> MUSHROOMS = item("mushrooms");
	static final TagKey<Item> NUGGETS_GOLD = item("nuggets/gold");
	static final TagKey<Item> STORAGE_BLOCKS_GOLD = item("storage_blocks/gold");
	static final TagKey<Item> STORAGE_BLOCKS_REDSTONE = item("storage_blocks/redstone");
	static final TagKey<Item> STRING = item("strings");

	private CommonTags() {
	}

	private static TagKey<Item> item(String path) {
		return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
	}
}
