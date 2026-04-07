package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.recipe.ConstructorRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
    public static final RecipeType<ConstructorRecipe> CONSTRUCTOR_TYPE = Registry.register(
        BuiltInRegistries.RECIPE_TYPE,
        FactoryLdMod.id("constructor"),
        new RecipeType<>() {
            @Override
            public String toString() {
                return FactoryLdMod.MOD_ID + ":constructor";
            }
        }
    );

    public static final RecipeSerializer<ConstructorRecipe> CONSTRUCTOR_SERIALIZER = Registry.register(
        BuiltInRegistries.RECIPE_SERIALIZER,
        FactoryLdMod.id("constructor"),
        new ConstructorRecipeSerializer()
    );

    private ModRecipes() {
    }

    public static void register() {
    }
}
