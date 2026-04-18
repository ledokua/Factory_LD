package net.ledok.factory_ld.client;

import net.fabricmc.api.ClientModInitializer;
import net.ledok.factory_ld.client.power.PowerWireClientNetworking;
import net.ledok.factory_ld.client.render.PowerWireRenderers;

public class FactoryLdModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        MachineClientScreens.registerAll();
        PowerWireClientNetworking.register();
        PowerWireRenderers.register();
    }
}
