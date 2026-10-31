package thedarkcolour.gendustry.client;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import forestry.api.client.IClientModuleHandler;
import forestry.core.platform.registration.FeatureFluid.ForestryFluidType;

import thedarkcolour.gendustry.client.screen.AdvancedMutatronScreen;
import thedarkcolour.gendustry.client.screen.IndustrialApiaryScreen;
import thedarkcolour.gendustry.client.screen.MutatronScreen;
import thedarkcolour.gendustry.client.screen.ProducerScreen;
import thedarkcolour.gendustry.client.screen.ReplicatorScreen;
import thedarkcolour.gendustry.client.screen.ThreeInputScreen;
import thedarkcolour.gendustry.registry.GFluids;
import thedarkcolour.gendustry.registry.GMenus;

public class ClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ClientHandler::registerMenuScreens);
		modBus.addListener(ClientHandler::registerClientExtensions);
	}

	// Forestry only registers client extensions for its own fluids, so without this the textures are null
	private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
		for (GFluids fluid : GFluids.values()) {
			if (fluid.fluid().getFluidType() instanceof ForestryFluidType type) {
				event.registerFluidType(new IClientFluidTypeExtensions() {
					@Override
					public ResourceLocation getStillTexture() {
						return type.getStillTexture();
					}

					@Override
					public ResourceLocation getFlowingTexture() {
						return type.getFlowingTexture();
					}

					@Override
					public int getTintColor() {
						return type.getColor();
					}
				}, type);
			}
		}
	}

	private static void registerMenuScreens(RegisterMenuScreensEvent event) {
		event.register(GMenus.PROCESSOR.menuType(), ProducerScreen::new);
		event.register(GMenus.SAMPLER.menuType(), ThreeInputScreen::new);
		event.register(GMenus.IMPRINTER.menuType(), ThreeInputScreen::new);
		event.register(GMenus.GENETIC_TRANSPOSER.menuType(), ThreeInputScreen::new);
		event.register(GMenus.MUTATRON.menuType(), MutatronScreen::new);
		event.register(GMenus.ADVANCED_MUTATRON.menuType(), AdvancedMutatronScreen::new);
		event.register(GMenus.REPLICATOR.menuType(), ReplicatorScreen::new);
		event.register(GMenus.INDUSTRIAL_APIARY.menuType(), IndustrialApiaryScreen::new);
	}
}
