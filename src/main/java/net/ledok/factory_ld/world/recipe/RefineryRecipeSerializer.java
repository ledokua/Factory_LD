package net.ledok.factory_ld.world.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class RefineryRecipeSerializer implements RecipeSerializer<RefineryRecipe> {
    private static final Codec<RefineryRecipe.PrimaryOutput> PRIMARY_OUTPUT_CODEC =
        Codec.STRING.xmap(
            value -> "fluid".equalsIgnoreCase(value) ? RefineryRecipe.PrimaryOutput.FLUID : RefineryRecipe.PrimaryOutput.ITEM,
            value -> value == RefineryRecipe.PrimaryOutput.FLUID ? "fluid" : "item"
        );

    private static final Codec<ConstructorRecipe.InputEntry> INPUT_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(ConstructorRecipe.InputEntry::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(ConstructorRecipe.InputEntry::count)
        ).apply(instance, ConstructorRecipe.InputEntry::new)
    );

    private static final Codec<RefineryRecipe.FluidEntry> FLUID_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(RefineryRecipe.FluidEntry::fluidId),
            Codec.LONG.optionalFieldOf("amount_mb", 1000L).forGetter(RefineryRecipe.FluidEntry::amountMb)
        ).apply(instance, RefineryRecipe.FluidEntry::new)
    );

    private static final MapCodec<RefineryRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
        instance.group(
            Codec.STRING.fieldOf("recipe_name").forGetter(ConstructorRecipe::getRecipeName),
            Codec.STRING.optionalFieldOf("recipe_name_key", "").forGetter(ConstructorRecipe::getRecipeNameKey),
            Codec.STRING.fieldOf("category").forGetter(ConstructorRecipe::getCategory),
            Codec.STRING.fieldOf("research_group").forGetter(ConstructorRecipe::getResearchGroup),
            PRIMARY_OUTPUT_CODEC.optionalFieldOf("primary_output", RefineryRecipe.PrimaryOutput.ITEM).forGetter(RefineryRecipe::getPrimaryOutput),
            INPUT_ENTRY_CODEC.listOf().fieldOf("item_inputs").forGetter(ConstructorRecipe::getItemInputs),
            FLUID_ENTRY_CODEC.listOf().fieldOf("fluid_inputs").forGetter(RefineryRecipe::getFluidInputs),
            ItemStack.CODEC.listOf().fieldOf("item_outputs").forGetter(ConstructorRecipe::getItemOutputs),
            FLUID_ENTRY_CODEC.listOf().fieldOf("fluid_outputs").forGetter(RefineryRecipe::getFluidOutputs),
            Codec.INT.optionalFieldOf("craft_time", 160).forGetter(ConstructorRecipe::getCraftTime)
        ).apply(instance, RefineryRecipeSerializer::fromCodec)
    );

    private static final StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> STREAM_CODEC =
        StreamCodec.of(RefineryRecipeSerializer::toNetwork, RefineryRecipeSerializer::fromNetwork);

    @Override
    public MapCodec<RefineryRecipe> codec() {
        return CODEC;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, RefineryRecipe> streamCodec() {
        return STREAM_CODEC;
    }

    private static RefineryRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        String name = buf.readUtf();
        String nameKey = buf.readUtf();
        String category = buf.readUtf();
        String researchGroup = buf.readUtf();
        RefineryRecipe.PrimaryOutput primaryOutput = buf.readEnum(RefineryRecipe.PrimaryOutput.class);

        int itemInputSize = buf.readVarInt();
        List<ConstructorRecipe.InputEntry> itemInputs = new java.util.ArrayList<>(itemInputSize);
        for (int i = 0; i < itemInputSize; i++) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            itemInputs.add(new ConstructorRecipe.InputEntry(ingredient, count));
        }

        int fluidInputSize = buf.readVarInt();
        List<RefineryRecipe.FluidEntry> fluidInputs = new java.util.ArrayList<>(fluidInputSize);
        for (int i = 0; i < fluidInputSize; i++) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            long amountMb = buf.readVarLong();
            fluidInputs.add(new RefineryRecipe.FluidEntry(id, amountMb));
        }

        int itemOutputSize = buf.readVarInt();
        List<ItemStack> itemOutputs = new java.util.ArrayList<>(itemOutputSize);
        for (int i = 0; i < itemOutputSize; i++) {
            itemOutputs.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        int fluidOutputSize = buf.readVarInt();
        List<RefineryRecipe.FluidEntry> fluidOutputs = new java.util.ArrayList<>(fluidOutputSize);
        for (int i = 0; i < fluidOutputSize; i++) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            long amountMb = buf.readVarLong();
            fluidOutputs.add(new RefineryRecipe.FluidEntry(id, amountMb));
        }

        int craftTime = buf.readVarInt();
        return new RefineryRecipe(name, nameKey, category, researchGroup, primaryOutput, itemInputs, fluidInputs, itemOutputs, fluidOutputs, craftTime);
    }

    private static void toNetwork(RegistryFriendlyByteBuf buf, RefineryRecipe recipe) {
        buf.writeUtf(recipe.getRecipeName());
        buf.writeUtf(recipe.getRecipeNameKey());
        buf.writeUtf(recipe.getCategory());
        buf.writeUtf(recipe.getResearchGroup());
        buf.writeEnum(recipe.getPrimaryOutput());

        List<ConstructorRecipe.InputEntry> itemInputs = recipe.getItemInputs();
        buf.writeVarInt(itemInputs.size());
        for (ConstructorRecipe.InputEntry entry : itemInputs) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.ingredient());
            buf.writeVarInt(entry.count());
        }

        List<RefineryRecipe.FluidEntry> fluidInputs = recipe.getFluidInputs();
        buf.writeVarInt(fluidInputs.size());
        for (RefineryRecipe.FluidEntry entry : fluidInputs) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.fluidId());
            buf.writeVarLong(entry.amountMb());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        buf.writeVarInt(itemOutputs.size());
        for (ItemStack stack : itemOutputs) {
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }

        List<RefineryRecipe.FluidEntry> fluidOutputs = recipe.getFluidOutputs();
        buf.writeVarInt(fluidOutputs.size());
        for (RefineryRecipe.FluidEntry entry : fluidOutputs) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.fluidId());
            buf.writeVarLong(entry.amountMb());
        }

        buf.writeVarInt(recipe.getCraftTime());
    }

    private static RefineryRecipe fromCodec(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        RefineryRecipe.PrimaryOutput primaryOutput,
        List<ConstructorRecipe.InputEntry> itemInputs,
        List<RefineryRecipe.FluidEntry> fluidInputs,
        List<ItemStack> itemOutputs,
        List<RefineryRecipe.FluidEntry> fluidOutputs,
        int craftTime
    ) {
        if (itemInputs.size() != 1) {
            throw new IllegalStateException("Refinery recipes must define exactly 1 item input.");
        }
        if (fluidInputs.size() != 1) {
            throw new IllegalStateException("Refinery recipes must define exactly 1 fluid input.");
        }
        if (itemOutputs.size() != 1) {
            throw new IllegalStateException("Refinery recipes must define exactly 1 item output.");
        }
        if (fluidOutputs.size() != 1) {
            throw new IllegalStateException("Refinery recipes must define exactly 1 fluid output.");
        }
        return new RefineryRecipe(recipeName, recipeNameKey, category, researchGroup, primaryOutput, itemInputs, fluidInputs, itemOutputs, fluidOutputs, craftTime);
    }
}
