package net.ledok.factory_ld;

import net.fabricmc.api.ModInitializer;
import net.ledok.factory_ld.registry.ModBlockEntities;
import net.ledok.factory_ld.registry.ModBlocks;
import net.ledok.factory_ld.registry.ModCommands;
import net.ledok.factory_ld.registry.ModCreativeTabs;
import net.ledok.factory_ld.registry.ModFluidStorages;
import net.ledok.factory_ld.registry.ModItems;
import net.ledok.factory_ld.registry.ModNetworking;
import net.ledok.factory_ld.registry.ModRecipes;
import net.ledok.factory_ld.registry.ModScreenHandlers;
import net.minecraft.resources.ResourceLocation;

public class FactoryLdMod implements ModInitializer {
    public static final String MOD_ID = "factory_ld";

    @Override
    public void onInitialize() {
        ModBlocks.register();
        ModBlockEntities.register();
        ModScreenHandlers.register();
        ModRecipes.register();
        ModFluidStorages.register();
        ModItems.register();
        ModCreativeTabs.register();
        ModCommands.register();
        ModNetworking.register();
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
