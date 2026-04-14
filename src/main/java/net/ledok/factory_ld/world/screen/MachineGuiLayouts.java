package net.ledok.factory_ld.world.screen;

import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.block.entity.MachineSpecs;

public final class MachineGuiLayouts {
    private static final int PLAYER_INV_Y = 174;
    private static final int PLAYER_HOTBAR_Y = 232;
    private static final int SHARD_SLOT_Y = 174;
    private static final int[] SHARD_XS = new int[] {180, 198, 216};
    private static final int INPUT_COLUMN_X = 12;
    private static final int OUTPUT_COLUMN_X = 176;
    private static final int COLUMN_CENTER_Y = 110;
    private static final int SLOT_SIZE = 16;
    private static final int SLOT_GAP = 4;

    public static final MachineGuiLayout CONSTRUCTOR = fromSpec(MachineSpecs.CONSTRUCTOR);
    public static final MachineGuiLayout ASSEMBLER = fromSpec(MachineSpecs.ASSEMBLER);
    public static final MachineGuiLayout REFINERY = fromSpec(MachineSpecs.REFINERY);
    public static final MachineGuiLayout BLENDER = fromSpec(MachineSpecs.BLENDER);
    public static final MachineGuiLayout FOUNDRY = fromSpec(MachineSpecs.FOUNDRY);
    public static final MachineGuiLayout MANUFACTURER = fromSpec(MachineSpecs.MANUFACTURER);
    public static final MachineGuiLayout SMELTER = fromSpec(MachineSpecs.SMELTER);

    private MachineGuiLayouts() {
    }

    public static MachineGuiLayout fromSpec(MachineSpec spec) {
        int[] inputYs = computeStackY(spec.itemInputSlots() + spec.fluidInputTanks());
        int[] outputYs = computeStackY(spec.itemOutputSlots() + spec.fluidOutputTanks());

        int[] itemInputXs = fillX(spec.itemInputSlots(), INPUT_COLUMN_X);
        int[] itemInputYs = slice(inputYs, 0, spec.itemInputSlots());
        int[] fluidInputXs = fillX(spec.fluidInputTanks(), INPUT_COLUMN_X);
        int[] fluidInputYs = slice(inputYs, spec.itemInputSlots(), spec.fluidInputTanks());

        int[] itemOutputXs = fillX(spec.itemOutputSlots(), OUTPUT_COLUMN_X);
        int[] itemOutputYs = slice(outputYs, 0, spec.itemOutputSlots());
        int[] fluidOutputXs = fillX(spec.fluidOutputTanks(), OUTPUT_COLUMN_X);
        int[] fluidOutputYs = slice(outputYs, spec.itemOutputSlots(), spec.fluidOutputTanks());

        return new MachineGuiLayout(
            PLAYER_INV_Y,
            PLAYER_HOTBAR_Y,
            SHARD_SLOT_Y,
            SHARD_XS,
            itemInputXs,
            itemInputYs,
            fluidInputXs,
            fluidInputYs,
            itemOutputXs,
            itemOutputYs,
            fluidOutputXs,
            fluidOutputYs
        );
    }

    private static int[] computeStackY(int totalSlots) {
        if (totalSlots <= 0) {
            return new int[0];
        }
        int stackHeight = totalSlots * SLOT_SIZE + (totalSlots - 1) * SLOT_GAP;
        int startY = COLUMN_CENTER_Y - stackHeight / 2;
        int[] ys = new int[totalSlots];
        for (int i = 0; i < totalSlots; i++) {
            ys[i] = startY + i * (SLOT_SIZE + SLOT_GAP);
        }
        return ys;
    }

    private static int[] fillX(int count, int x) {
        int[] xs = new int[count];
        for (int i = 0; i < count; i++) {
            xs[i] = x;
        }
        return xs;
    }

    private static int[] slice(int[] source, int start, int length) {
        int[] result = new int[length];
        for (int i = 0; i < length; i++) {
            result[i] = source[start + i];
        }
        return result;
    }
}
