package thedarkcolour.gendustry.client;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

import forestry.api.client.IClientModuleHandler;

import thedarkcolour.gendustry.client.screen.AdvancedMutatronScreen;
import thedarkcolour.gendustry.client.screen.IndustrialApiaryScreen;
import thedarkcolour.gendustry.client.screen.MutatronScreen;
import thedarkcolour.gendustry.client.screen.ProducerScreen;
import thedarkcolour.gendustry.client.screen.ReplicatorScreen;
import thedarkcolour.gendustry.client.screen.ThreeInputScreen;
import thedarkcolour.gendustry.registry.GMenus;

public class ClientHandler implements IClientModuleHandler {
	@Override
	public void registerEvents(IEventBus modBus) {
		modBus.addListener(ClientHandler::registerMenuScreens);
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
