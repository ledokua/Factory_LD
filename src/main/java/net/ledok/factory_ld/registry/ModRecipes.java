package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.block.entity.MachineSpecs;
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.ledok.factory_ld.world.recipe.MachineRecipeSerializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModRecipes {
    private static final int DEFAULT_MACHINE_CRAFT_TIME = 100;
    private static final Map<String, RecipeType<MachineRecipe>> MACHINE_TYPES = new LinkedHashMap<>();
    private static final Map<String, RecipeSerializer<MachineRecipe>> MACHINE_SERIALIZERS = new LinkedHashMap<>();

    static {
        for (MachineSpec spec : MachineSpecs.ALL) {
            MACHINE_TYPES.put(spec.id(), registerType(spec.recipeTypeId()));
            MACHINE_SERIALIZERS.put(
                spec.id(),
                registerSerializer(spec.recipeTypeId(), new MachineRecipeSerializer(spec.id(), spec, DEFAULT_MACHINE_CRAFT_TIME))
            );
        }
    }

    public static final RecipeType<MachineRecipe> CONSTRUCTOR_TYPE = requireMachineRecipeType("constructor");
    public static final RecipeType<MachineRecipe> ASSEMBLER_TYPE = requireMachineRecipeType("assembler");
    public static final RecipeType<MachineRecipe> REFINERY_TYPE = requireMachineRecipeType("refinery");
    public static final RecipeType<MachineRecipe> BLENDER_TYPE = requireMachineRecipeType("blender");
    public static final RecipeType<MachineRecipe> FOUNDRY_TYPE = requireMachineRecipeType("foundry");
    public static final RecipeType<MachineRecipe> MANUFACTURER_TYPE = requireMachineRecipeType("manufacturer");
    public static final RecipeType<MachineRecipe> SMELTER_TYPE = requireMachineRecipeType("smelter");

    public static final RecipeSerializer<MachineRecipe> CONSTRUCTOR_SERIALIZER = requireMachineRecipeSerializer("constructor");
    public static final RecipeSerializer<MachineRecipe> ASSEMBLER_SERIALIZER = requireMachineRecipeSerializer("assembler");
    public static final RecipeSerializer<MachineRecipe> REFINERY_SERIALIZER = requireMachineRecipeSerializer("refinery");
    public static final RecipeSerializer<MachineRecipe> BLENDER_SERIALIZER = requireMachineRecipeSerializer("blender");
    public static final RecipeSerializer<MachineRecipe> FOUNDRY_SERIALIZER = requireMachineRecipeSerializer("foundry");
    public static final RecipeSerializer<MachineRecipe> MANUFACTURER_SERIALIZER = requireMachineRecipeSerializer("manufacturer");
    public static final RecipeSerializer<MachineRecipe> SMELTER_SERIALIZER = requireMachineRecipeSerializer("smelter");

    private ModRecipes() {
    }

    public static void register() {
    }

    public static RecipeType<MachineRecipe> requireMachineRecipeType(String id) {
        RecipeType<MachineRecipe> type = MACHINE_TYPES.get(id);
        if (type == null) {
            throw new IllegalStateException("Unknown machine recipe type: " + id);
        }
        return type;
    }

    public static RecipeSerializer<MachineRecipe> requireMachineRecipeSerializer(String id) {
        RecipeSerializer<MachineRecipe> serializer = MACHINE_SERIALIZERS.get(id);
        if (serializer == null) {
            throw new IllegalStateException("Unknown machine recipe serializer: " + id);
        }
        return serializer;
    }

    private static <T extends Recipe<?>> RecipeType<T> registerType(String id) {
        return Registry.register(
            BuiltInRegistries.RECIPE_TYPE,
            ResourceLocation.fromNamespaceAndPath(FactoryLdMod.MOD_ID, id),
            new RecipeType<>() {
                @Override
                public String toString() {
                    return FactoryLdMod.MOD_ID + ":" + id;
                }
            }
        );
    }

    private static <T extends RecipeSerializer<?>> T registerSerializer(String name, T serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, FactoryLdMod.id(name), serializer);
    }
}
