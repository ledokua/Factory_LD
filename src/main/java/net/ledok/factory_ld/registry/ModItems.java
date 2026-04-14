package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.item.PowerLineToolItem;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static Item POWER_LINE_TOOL;

    private ModItems() {
    }

    public static void register() {
        if (POWER_LINE_TOOL != null) {
            return;
        }
        POWER_LINE_TOOL = Registry.register(
            BuiltInRegistries.ITEM,
            FactoryLdMod.id("power_line_tool"),
            new PowerLineToolItem(new Item.Properties().stacksTo(1))
        );
    }
}
