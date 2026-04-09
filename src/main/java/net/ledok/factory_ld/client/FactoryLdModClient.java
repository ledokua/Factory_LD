package net.ledok.factory_ld.client;

import net.fabricmc.api.ClientModInitializer;
import net.ledok.factory_ld.client.screen.AssemblerScreen;
import net.ledok.factory_ld.client.screen.ConstructorScreen;
import net.ledok.factory_ld.client.screen.RefineryScreen;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.minecraft.client.gui.screens.MenuScreens;

public class FactoryLdModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MenuScreens.register(ModScreenHandlers.CONSTRUCTOR, ConstructorScreen::new);
        MenuScreens.register(ModScreenHandlers.ASSEMBLER, AssemblerScreen::new);
        MenuScreens.register(ModScreenHandlers.REFINERY, RefineryScreen::new);
    }
}
