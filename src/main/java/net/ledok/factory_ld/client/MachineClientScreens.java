package net.ledok.factory_ld.client;

import java.util.List;

import net.ledok.factory_ld.client.screen.GenericMachineScreen;
import net.ledok.factory_ld.registry.MachineDescriptors;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.minecraft.client.gui.screens.MenuScreens;

public final class MachineClientScreens {
    private MachineClientScreens() {
    }

    public static void registerAll() {
        List<Runnable> registrations = MachineDescriptors.ALL.stream()
            .map(descriptor -> (Runnable)() -> MenuScreens.register(ModScreenHandlers.requireMachineMenuType(descriptor.id()), GenericMachineScreen::new))
            .toList();
        for (Runnable registration : registrations) {
            registration.run();
        }
    }
}
