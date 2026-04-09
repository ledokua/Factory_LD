package net.ledok.factory_ld.registry;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.ledok.factory_ld.FactoryLdMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModCreativeTabs {
    public static final CreativeModeTab MAIN = Registry.register(
        BuiltInRegistries.CREATIVE_MODE_TAB,
        FactoryLdMod.id("main"),
        FabricItemGroup.builder()
            .title(Component.translatable("itemGroup.factory_ld.main"))
            .icon(() -> new ItemStack(ModBlocks.CONSTRUCTOR))
            .displayItems((parameters, output) -> {
                output.accept(ModBlocks.CONSTRUCTOR);
                output.accept(ModBlocks.ASSEMBLER);
                output.accept(ModBlocks.REFINERY);
                output.accept(ModBlocks.POWER_EMITTER);
            })
            .build()
    );

    private ModCreativeTabs() {
    }

    public static void register() {
    }
}
