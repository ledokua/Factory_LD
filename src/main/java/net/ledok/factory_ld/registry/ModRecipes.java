package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.recipe.AssemblerRecipe;
import net.ledok.factory_ld.world.recipe.AssemblerRecipeSerializer;
import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.recipe.ConstructorRecipeSerializer;
import net.ledok.factory_ld.world.recipe.RefineryRecipe;
import net.ledok.factory_ld.world.recipe.RefineryRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

public final class ModRecipes {
    public static final RecipeType<ConstructorRecipe> CONSTRUCTOR_TYPE = registerType("constructor");
    public static final RecipeType<AssemblerRecipe> ASSEMBLER_TYPE = registerType("assembler");
    public static final RecipeType<RefineryRecipe> REFINERY_TYPE = registerType("refinery");

    public static final RecipeSerializer<ConstructorRecipe> CONSTRUCTOR_SERIALIZER =
        registerSerializer("constructor", new ConstructorRecipeSerializer());
    public static final RecipeSerializer<AssemblerRecipe> ASSEMBLER_SERIALIZER =
        registerSerializer("assembler", new AssemblerRecipeSerializer());
    public static final RecipeSerializer<RefineryRecipe> REFINERY_SERIALIZER =
        registerSerializer("refinery", new RefineryRecipeSerializer());

    private ModRecipes() {
    }

    public static void register() {
    }

    private static <T extends Recipe<?>> RecipeType<T> registerType(String name) {
        return Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            FactoryLdMod.id(name),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return FactoryLdMod.MOD_ID + ":" + name;
                }
            }
        );
    }

    private static <T extends RecipeSerializer<?>> T registerSerializer(String name, T serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, FactoryLdMod.id(name), serializer);
    }
}
