package net.ledok.factory_ld.client;

import net.fabricmc.api.ClientModInitializer;

public class FactoryLdModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MachineClientScreens.registerAll();
    }
}
