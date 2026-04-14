package net.ledok.factory_ld.registry;

import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.PowerEmitterBlock;
import net.ledok.factory_ld.world.block.PowerPoleBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {
    public static Block CONSTRUCTOR;
    public static Block ASSEMBLER;
    public static Block REFINERY;
    public static Block BLENDER;
    public static Block FOUNDRY;
    public static Block MANUFACTURER;
    public static Block SMELTER;
    public static Block POWER_EMITTER;
    public static Block POWER_POLE;
    public static Block POWER_STORAGE;

    private ModBlocks() {
    }

    private static Block register(String name, Block block) {
        ResourceLocation id = FactoryLdMod.id(name);
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties()));
        return block;
    }

    public static void register() {
        if (CONSTRUCTOR != null) {
            return;
        }
        for (MachineDescriptors.Descriptor descriptor : MachineDescriptors.ALL) {
            register(descriptor.id(), descriptor.createBlock());
        }
        POWER_EMITTER = register(
            "power_emitter",
            new PowerEmitterBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
        );
        POWER_POLE = register(
            "power_pole",
            new PowerPoleBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK))
        );
        for (PowerStorageDescriptors.Descriptor descriptor : PowerStorageDescriptors.ALL) {
            register(descriptor.id(), descriptor.createBlock());
        }
        CONSTRUCTOR = requireMachineBlock("constructor");
        ASSEMBLER = requireMachineBlock("assembler");
        REFINERY = requireMachineBlock("refinery");
        BLENDER = requireMachineBlock("blender");
        FOUNDRY = requireMachineBlock("foundry");
        MANUFACTURER = requireMachineBlock("manufacturer");
        SMELTER = requireMachineBlock("smelter");
        POWER_STORAGE = requirePowerStorageBlock("power_storage");
    }

    public static Block requireMachineBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(FactoryLdMod.id(id))
            .orElseThrow(() -> new IllegalStateException("Machine block not registered: " + id));
    }

    public static Block requirePowerStorageBlock(String id) {
        return BuiltInRegistries.BLOCK.getOptional(FactoryLdMod.id(id))
            .orElseThrow(() -> new IllegalStateException("Power storage block not registered: " + id));
    }
}
