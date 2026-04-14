package net.ledok.factory_ld.world.recipe;

import java.util.List;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class MachineRecipeSerializer implements RecipeSerializer<MachineRecipe> {
    private static final Codec<MachineRecipe.PrimaryOutput> PRIMARY_OUTPUT_CODEC =
        Codec.STRING.xmap(
            value -> "fluid".equalsIgnoreCase(value) ? MachineRecipe.PrimaryOutput.FLUID : MachineRecipe.PrimaryOutput.ITEM,
            value -> value == MachineRecipe.PrimaryOutput.FLUID ? "fluid" : "item"
        );

    private static final Codec<MachineRecipe.InputEntry> INPUT_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            Ingredient.CODEC_NONEMPTY.fieldOf("ingredient").forGetter(MachineRecipe.InputEntry::ingredient),
            Codec.INT.optionalFieldOf("count", 1).forGetter(MachineRecipe.InputEntry::count)
        ).apply(instance, MachineRecipe.InputEntry::new)
    );

    private static final Codec<MachineRecipe.FluidEntry> FLUID_ENTRY_CODEC = RecordCodecBuilder.create(instance ->
        instance.group(
            ResourceLocation.CODEC.fieldOf("id").forGetter(MachineRecipe.FluidEntry::fluidId),
            Codec.LONG.optionalFieldOf("amount_mb", 1000L).forGetter(MachineRecipe.FluidEntry::amountMb)
        ).apply(instance, MachineRecipe.FluidEntry::new)
    );

    private final String machineTypeId;
    private final MachineSpec spec;
    private final int defaultCraftTime;
    private final MapCodec<MachineRecipe> codec;
    private final StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec;

    public MachineRecipeSerializer(String machineTypeId, MachineSpec spec, int defaultCraftTime) {
        this.machineTypeId = machineTypeId;
        this.spec = spec;
        this.defaultCraftTime = defaultCraftTime;
        this.codec = buildCodec();
        this.streamCodec = StreamCodec.of(this::toNetwork, this::fromNetwork);
    }

    @Override
    public MapCodec<MachineRecipe> codec() {
        return codec;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, MachineRecipe> streamCodec() {
        return streamCodec;
    }

    private MapCodec<MachineRecipe> buildCodec() {
        return RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                Codec.STRING.fieldOf("recipe_name").forGetter(MachineRecipe::getRecipeName),
                Codec.STRING.optionalFieldOf("recipe_name_key", "").forGetter(MachineRecipe::getRecipeNameKey),
                Codec.STRING.fieldOf("category").forGetter(MachineRecipe::getCategory),
                Codec.STRING.fieldOf("research_group").forGetter(MachineRecipe::getResearchGroup),
                PRIMARY_OUTPUT_CODEC.optionalFieldOf("primary_output", MachineRecipe.PrimaryOutput.ITEM).forGetter(MachineRecipe::getPrimaryOutput),
                INPUT_ENTRY_CODEC.listOf().fieldOf("item_inputs").forGetter(MachineRecipe::getItemInputs),
                FLUID_ENTRY_CODEC.listOf().optionalFieldOf("fluid_inputs", List.of()).forGetter(MachineRecipe::getFluidInputs),
                ItemStack.CODEC.listOf().fieldOf("item_outputs").forGetter(MachineRecipe::getItemOutputs),
                FLUID_ENTRY_CODEC.listOf().optionalFieldOf("fluid_outputs", List.of()).forGetter(MachineRecipe::getFluidOutputs),
                Codec.INT.optionalFieldOf("craft_time", defaultCraftTime).forGetter(MachineRecipe::getCraftTime)
            ).apply(instance, this::fromCodec)
        );
    }

    private MachineRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
        String recipeName = buf.readUtf();
        String recipeNameKey = buf.readUtf();
        String category = buf.readUtf();
        String researchGroup = buf.readUtf();
        MachineRecipe.PrimaryOutput primaryOutput = buf.readEnum(MachineRecipe.PrimaryOutput.class);

        int itemInputSize = buf.readVarInt();
        List<MachineRecipe.InputEntry> itemInputs = new java.util.ArrayList<>(itemInputSize);
        for (int i = 0; i < itemInputSize; i++) {
            Ingredient ingredient = Ingredient.CONTENTS_STREAM_CODEC.decode(buf);
            int count = buf.readVarInt();
            itemInputs.add(new MachineRecipe.InputEntry(ingredient, count));
        }

        int fluidInputSize = buf.readVarInt();
        List<MachineRecipe.FluidEntry> fluidInputs = new java.util.ArrayList<>(fluidInputSize);
        for (int i = 0; i < fluidInputSize; i++) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            long amountMb = buf.readVarLong();
            fluidInputs.add(new MachineRecipe.FluidEntry(id, amountMb));
        }

        int itemOutputSize = buf.readVarInt();
        List<ItemStack> itemOutputs = new java.util.ArrayList<>(itemOutputSize);
        for (int i = 0; i < itemOutputSize; i++) {
            itemOutputs.add(ItemStack.STREAM_CODEC.decode(buf));
        }

        int fluidOutputSize = buf.readVarInt();
        List<MachineRecipe.FluidEntry> fluidOutputs = new java.util.ArrayList<>(fluidOutputSize);
        for (int i = 0; i < fluidOutputSize; i++) {
            ResourceLocation id = ResourceLocation.STREAM_CODEC.decode(buf);
            long amountMb = buf.readVarLong();
            fluidOutputs.add(new MachineRecipe.FluidEntry(id, amountMb));
        }

        int craftTime = buf.readVarInt();
        return fromCodec(recipeName, recipeNameKey, category, researchGroup, primaryOutput, itemInputs, fluidInputs, itemOutputs, fluidOutputs, craftTime);
    }

    private void toNetwork(RegistryFriendlyByteBuf buf, MachineRecipe recipe) {
        buf.writeUtf(recipe.getRecipeName());
        buf.writeUtf(recipe.getRecipeNameKey());
        buf.writeUtf(recipe.getCategory());
        buf.writeUtf(recipe.getResearchGroup());
        buf.writeEnum(recipe.getPrimaryOutput());

        List<MachineRecipe.InputEntry> itemInputs = recipe.getItemInputs();
        buf.writeVarInt(itemInputs.size());
        for (MachineRecipe.InputEntry entry : itemInputs) {
            Ingredient.CONTENTS_STREAM_CODEC.encode(buf, entry.ingredient());
            buf.writeVarInt(entry.count());
        }

        List<MachineRecipe.FluidEntry> fluidInputs = recipe.getFluidInputs();
        buf.writeVarInt(fluidInputs.size());
        for (MachineRecipe.FluidEntry entry : fluidInputs) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.fluidId());
            buf.writeVarLong(entry.amountMb());
        }

        List<ItemStack> itemOutputs = recipe.getItemOutputs();
        buf.writeVarInt(itemOutputs.size());
        for (ItemStack stack : itemOutputs) {
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }

        List<MachineRecipe.FluidEntry> fluidOutputs = recipe.getFluidOutputs();
        buf.writeVarInt(fluidOutputs.size());
        for (MachineRecipe.FluidEntry entry : fluidOutputs) {
            ResourceLocation.STREAM_CODEC.encode(buf, entry.fluidId());
            buf.writeVarLong(entry.amountMb());
        }

        buf.writeVarInt(recipe.getCraftTime());
    }

    private MachineRecipe fromCodec(
        String recipeName,
        String recipeNameKey,
        String category,
        String researchGroup,
        MachineRecipe.PrimaryOutput primaryOutput,
        List<MachineRecipe.InputEntry> itemInputs,
        List<MachineRecipe.FluidEntry> fluidInputs,
        List<ItemStack> itemOutputs,
        List<MachineRecipe.FluidEntry> fluidOutputs,
        int craftTime
    ) {
        if (itemInputs.size() != spec.itemInputSlots()) {
            throw new IllegalStateException(machineTypeId + " recipes must define exactly " + spec.itemInputSlots() + " item inputs.");
        }
        if (itemOutputs.size() != spec.itemOutputSlots()) {
            throw new IllegalStateException(machineTypeId + " recipes must define exactly " + spec.itemOutputSlots() + " item outputs.");
        }
        if (fluidInputs.size() != spec.fluidInputTanks()) {
            throw new IllegalStateException(machineTypeId + " recipes must define exactly " + spec.fluidInputTanks() + " fluid inputs.");
        }
        if (fluidOutputs.size() != spec.fluidOutputTanks()) {
            throw new IllegalStateException(machineTypeId + " recipes must define exactly " + spec.fluidOutputTanks() + " fluid outputs.");
        }
        return new MachineRecipe(
            machineTypeId,
            recipeName,
            recipeNameKey,
            category,
            researchGroup,
            primaryOutput,
            itemInputs,
            fluidInputs,
            itemOutputs,
            fluidOutputs,
            craftTime
        );
    }
}
