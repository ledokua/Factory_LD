package net.ledok.factory_ld.world.recipe;

import java.util.List;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ConstructorRecipeSerializer implements RecipeSerializer<ConstructorRecipe> {
    private static final Codec<ConstructorRecipe.InputEntry> INPUT_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ConstructorRecipe.InputEntry::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ConstructorRecipe.InputEntry::count)
        ).apply(instance, ConstructorRecipe.InputEntry::new)
    );

    private static final MapCodec<ConstructorRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("recipe_name").forGetter(ConstructorRecipe::getRecipeName),
            Codec.STRING.optionalFieldOf("recipe_name_key", "").forGetter(ConstructorRecipe::getRecipeNameKey),
            Codec.STRING.fieldOf("category").forGetter(ConstructorRecipe::getCategory),
            Codec.STRING.fieldOf("research_group").forGetter(ConstructorRecipe::getResearchGroup),
            INPUT_ENTRY_CODEC.listOf().fieldOf("item_inputs").forGetter(ConstructorRecipe::getItemInputs),
            ItemStack.CODEC.listOf().fieldOf("item_outputs").forGetter(ConstructorRecipe::getItemOutputs),
            Codec.INT.optionalFieldOf("craft_time", 80).forGetter(ConstructorRecipe::getCraftTime)
        ).apply(instance, ConstructorRecipeSerializer::fromCodec)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, ConstructorRecipe> STREAM_CODEC =
        StreamCodec.of(ConstructorRecipeSerializer::toNetwork, ConstructorRecipeSerializer::fromNetwork);

    @Override
    public MapCodec<ConstructorRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, ConstructorRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static ConstructorRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf();
        String nameKey = buf.readUtf();
        String category = buf.readUtf();
        String researchGroup = buf.readUtf();
        int inputSize = buf.readVarInt();
        List<ConstructorRecipe.InputEntry> inputs = new java.util.ArrayList<>(inputSize);
        for (int i = 0; i < inputSize; i++) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            inputs.add(new ConstructorRecipe.InputEntry(ingredient, count));
        }
        int outputSize = buf.readVarInt();
        List<ItemStack> outputs = new java.util.ArrayList<>(outputSize);
        for (int i = 0; i < outputSize; i++) {
            outputs.add(ItemStack.STREAM_CODEC.decode(buf));
        }
        int craftTime = buf.readVarInt();
        return new ConstructorRecipe(name, nameKey, category, researchGroup, inputs, outputs, craftTime);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, ConstructorRecipe recipe) {
        buf.writeUtf(recipe.getRecipeName());
        buf.writeUtf(recipe.getRecipeNameKey());
        buf.writeUtf(recipe.getCategory());
        buf.writeUtf(recipe.getResearchGroup());
        List<ConstructorRecipe.InputEntry> inputs = recipe.getItemInputs();
        buf.writeVarInt(inputs.size());
        for (ConstructorRecipe.InputEntry entry : inputs) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.ingredient());
            buf.writeVarInt(entry.count());
        }
        List<ItemStack> outputs = recipe.getItemOutputs();
        buf.writeVarInt(outputs.size());
        for (ItemStack stack : outputs) {
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }
        buf.writeVarInt(recipe.getCraftTime());
    }

    private static ConstructorRecipe fromCodec(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        List<ConstructorRecipe.InputEntry> itemInputs,
        List<ItemStack> itemOutputs,
        int craftTime
    ) {
        List<ConstructorRecipe.InputEntry> inputs = itemInputs;
        List<ItemStack> outputs = itemOutputs;
        if (inputs.isEmpty() || outputs.isEmpty()) {
            throw new IllegalStateException("Constructor recipes must define at least 1 input and 1 output.");
        }

        return new ConstructorRecipe(recipeName, recipeNameKey, category, researchGroup, inputs, outputs, craftTime);
    }
}
