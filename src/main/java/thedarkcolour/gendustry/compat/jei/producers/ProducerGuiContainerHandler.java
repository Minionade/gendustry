package thedarkcolour.gendustry.compat.jei.producers;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import net.minecraft.world.level.block.entity.BlockEntity;

import mezz.jei.api.gui.handlers.IGuiClickableArea;
import mezz.jei.api.gui.handlers.IGuiContainerHandler;
import mezz.jei.api.recipe.RecipeType;
import thedarkcolour.gendustry.blockentity.DnaExtractorBlockEntity;
import thedarkcolour.gendustry.blockentity.MutagenProducerBlockEntity;
import thedarkcolour.gendustry.blockentity.ProteinLiquefierBlockEntity;
import thedarkcolour.gendustry.client.screen.ProducerScreen;
import thedarkcolour.gendustry.compat.jei.GendustryRecipeType;

public class ProducerGuiContainerHandler implements IGuiContainerHandler<ProducerScreen> {
	private final Set<RecipeType<?>> registeredTypes;

	public ProducerGuiContainerHandler(Set<RecipeType<?>> registeredTypes) {
		this.registeredTypes = Set.copyOf(registeredTypes);
	}

	@Override
	public Collection<IGuiClickableArea> getGuiClickableAreas(ProducerScreen containerScreen, double guiMouseX, double guiMouseY) {
		BlockEntity blockEntity = containerScreen.getMenu().getTile();
		if (blockEntity instanceof MutagenProducerBlockEntity) {
			return getClickArea(GendustryRecipeType.MUTAGEN_PRODUCER);
		} else if (blockEntity instanceof DnaExtractorBlockEntity) {
			return getClickArea(GendustryRecipeType.DNA_EXTRACTOR);
		} else if (blockEntity instanceof ProteinLiquefierBlockEntity) {
			return getClickArea(GendustryRecipeType.PROTEIN_LIQUEFIER);
		}
		return Collections.emptyList();
	}

	private Collection<IGuiClickableArea> getClickArea(RecipeType<?> recipeType) {
		if (!this.registeredTypes.contains(recipeType)) {
			return Collections.emptyList();
		}
		return Collections.singleton(IGuiClickableArea.createBasic(48, 40, 55, 18, recipeType));
	}
}
