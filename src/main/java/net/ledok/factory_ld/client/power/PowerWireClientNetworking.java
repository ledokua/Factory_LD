package net.ledok.factory_ld.client.power;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.ledok.factory_ld.registry.ModNetworking;

public final class PowerWireClientNetworking {
    private PowerWireClientNetworking() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.POWER_LINKS_SNAPSHOT, (payload, context) -> {
            context.client().execute(() -> PowerWireClientState.applySnapshot(payload.levelKey(), payload.links()));
        });
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.POWER_LINK_DELTA, (payload, context) -> {
            context.client().execute(() -> PowerWireClientState.applyDelta(payload.levelKey(), payload.a(), payload.b(), payload.connected()));
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> PowerWireClientState.clear());
    }
}
