package net.ledok.factory_ld.client.screen;

import net.ledok.factory_ld.world.screen.GenericMachineScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class GenericMachineScreen extends ConfiguredMachineScreen<GenericMachineScreenHandler> {
    public GenericMachineScreen(GenericMachineScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
    }
}
