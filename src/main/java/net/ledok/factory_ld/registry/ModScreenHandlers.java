package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.screen.AssemblerScreenData;
import net.ledok.factory_ld.world.screen.AssemblerScreenHandler;
import net.ledok.factory_ld.world.screen.ConstructorScreenData;
import net.ledok.factory_ld.world.screen.ConstructorScreenHandler;
import net.ledok.factory_ld.world.screen.RefineryScreenData;
import net.ledok.factory_ld.world.screen.RefineryScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
    public static final MenuType<ConstructorScreenHandler> CONSTRUCTOR = Registry.register(
        BuiltInRegistries.MENU,
        FactoryLdMod.id("constructor"),
        new ExtendedScreenHandlerType<>(ConstructorScreenHandler::new, ConstructorScreenData.STREAM_CODEC)
    );
    public static final MenuType<AssemblerScreenHandler> ASSEMBLER = Registry.register(
        BuiltInRegistries.MENU,
        FactoryLdMod.id("assembler"),
        new ExtendedScreenHandlerType<>(AssemblerScreenHandler::new, AssemblerScreenData.STREAM_CODEC)
    );
    public static final MenuType<RefineryScreenHandler> REFINERY = Registry.register(
        BuiltInRegistries.MENU,
        FactoryLdMod.id("refinery"),
        new ExtendedScreenHandlerType<>(RefineryScreenHandler::new, RefineryScreenData.STREAM_CODEC)
    );

    private ModScreenHandlers() {
    }

    public static void register() {
    }
}
