package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.AssemblerBlock;
import net.ledok.factory_ld.world.block.ConstructorBlock;
import net.ledok.factory_ld.world.block.PowerEmitterBlock;
import net.ledok.factory_ld.world.block.RefineryBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static final Block CONSTRUCTOR = register(
        "constructor",
        new ConstructorBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
    );
    public static final Block ASSEMBLER = register(
        "assembler",
        new AssemblerBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
    );
    public static final Block REFINERY = register(
        "refinery",
        new RefineryBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
    );
    public static final Block POWER_EMITTER = register(
        "power_emitter",
        new PowerEmitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
    );

    private ModBlocks() {
    }

    private static Block register(String name, Block block) {
        ResourceLocation id = FactoryLdMod.id(name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
        return block;
    }

    public static void register() {
    }
}
