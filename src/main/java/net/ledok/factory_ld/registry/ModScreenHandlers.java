package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.screen.GenericMachineScreenHandler;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

public final class ModScreenHandlers {
    public static MenuType<GenericMachineScreenHandler> CONSTRUCTOR;
    public static MenuType<GenericMachineScreenHandler> ASSEMBLER;
    public static MenuType<GenericMachineScreenHandler> REFINERY;
    public static MenuType<GenericMachineScreenHandler> BLENDER;
    public static MenuType<GenericMachineScreenHandler> FOUNDRY;
    public static MenuType<GenericMachineScreenHandler> MANUFACTURER;
    public static MenuType<GenericMachineScreenHandler> SMELTER;

    private ModScreenHandlers() {
    }

    public static void register() {
        if (CONSTRUCTOR != null) {
            return;
        }
        for (MachineDescriptors.Descriptor descriptor : MachineDescriptors.ALL) {
            Registry.register(
                BuiltInRegistries.MENU,
                FactoryLdMod.id(descriptor.id()),
                descriptor.createMenuType()
            );
        }
        CONSTRUCTOR = requireMachineMenuType("constructor");
        ASSEMBLER = requireMachineMenuType("assembler");
        REFINERY = requireMachineMenuType("refinery");
        BLENDER = requireMachineMenuType("blender");
        FOUNDRY = requireMachineMenuType("foundry");
        MANUFACTURER = requireMachineMenuType("manufacturer");
        SMELTER = requireMachineMenuType("smelter");
    }

    @SuppressWarnings("unchecked")
    public static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> requireMachineMenuType(String id) {
        return (MenuType<T>) BuiltInRegistries.MENU.getOptional(FactoryLdMod.id(id))
            .orElseThrow(() -> new IllegalStateException("Machine menu type not registered: " + id));
    }
}
