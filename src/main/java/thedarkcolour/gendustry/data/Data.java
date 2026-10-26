package thedarkcolour.gendustry.data;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;

import net.neoforged.neoforge.data.event.GatherDataEvent;
import thedarkcolour.gendustry.Gendustry;
import thedarkcolour.modkit.data.DataHelper;

public class Data {
	public static void gatherData(GatherDataEvent event) {
		DataHelper helper = new DataHelper.Builder(Gendustry.ID, event).packOutput(event.getGenerator().getPackOutput()).build();
		DataGenerator generator = event.getGenerator();
		PackOutput output = generator.getPackOutput();

		helper.createEnglish(true, English::addTranslations);
		helper.createRecipes(Recipes::addRecipes);
		helper.createBlockModels(BlockModels::addBlockModels);
		helper.createItemModels(true, true, false, null);
		helper.createTags(Registries.BLOCK, ModTags::addBlockTags);
		helper.createTags(Registries.ITEM, ModTags::addItemTags);

		generator.addProvider(event.includeServer(), new LootProvider(output, event.getLookupProvider()));
	}
}
