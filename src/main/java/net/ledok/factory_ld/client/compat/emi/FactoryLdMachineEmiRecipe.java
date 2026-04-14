package net.ledok.factory_ld.client.compat.emi;

import dev.emi.emi.api.recipe.BasicEmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.TextWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.ledok.factory_ld.FactoryLdMod;
import net.ledok.factory_ld.world.block.entity.MachineSpec;
import net.ledok.factory_ld.world.recipe.MachineRecipe;
import net.ledok.factory_ld.world.screen.MachineGuiLayout;
import net.ledok.factory_ld.world.screen.MachineGuiLayouts;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import java.util.List;
import java.util.Locale;

public final class FactoryLdMachineEmiRecipe extends BasicEmiRecipe {
    private static final ResourceLocation SCREEN_TEXTURE = FactoryLdMod.id("textures/gui/constructor.png");
    private static final ResourceLocation PROGRESS_BG_TEXTURE = FactoryLdMod.id("textures/gui/constructor_progress_bg.png");
    private static final ResourceLocation PROGRESS_FILL_TEXTURE = FactoryLdMod.id("textures/gui/constructor_progress_fill.png");
    private static final int WIDTH = 256;
    private static final int HEIGHT = 142;
    private static final int HEADER_X = 8;
    private static final int HEADER_Y = 26;
    private static final int HEADER_W = 240;
    private static final int HEADER_H = 18;
    private static final int PROD_SECTION_Y = 48;
    private static final int PROD_PANEL_W = 76;
    private static final int PROD_PANEL_GAP = 6;
    private static final int PROD_LEFT_PANEL_X = 8;
    private static final int PROD_CENTER_PANEL_X = PROD_LEFT_PANEL_X + PROD_PANEL_W + PROD_PANEL_GAP;
    private static final int PROD_RIGHT_PANEL_X = PROD_CENTER_PANEL_X + PROD_PANEL_W + PROD_PANEL_GAP;
    private static final int EMI_CONTENT_Y_OFFSET = -19;
    private static final int PROD_PROGRESS_W = 56;
    private static final int PROD_PROGRESS_H = 6;

    private final MachineRecipe recipe;
    private final MachineGuiLayout layout;
    private final MachineSpec spec;

    public FactoryLdMachineEmiRecipe(
        EmiRecipeCategory category,
        ResourceLocation id,
        MachineRecipe recipe,
        EmiStack workstation,
        MachineSpec spec
    ) {
        super(category, id, WIDTH, HEIGHT);
        this.recipe = recipe;
        this.inputs = buildInputs(recipe);
        this.outputs = buildOutputs(recipe);
        this.catalysts = List.of(workstation);
        this.layout = MachineGuiLayouts.fromSpec(spec);
        this.spec = spec;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addTexture(SCREEN_TEXTURE, 0, 0, 0, 0, WIDTH, HEIGHT);
        widgets.addDrawable(
            0,
            0,
            WIDTH,
            HEIGHT,
            (draw, mouseX, mouseY, delta) -> {
                int prodSectionHeight = HEIGHT - PROD_SECTION_Y - 8;
                draw.fill(HEADER_X, HEADER_Y, HEADER_X + HEADER_W, HEADER_Y + HEADER_H, 0xCC171717);
                draw.fill(PROD_LEFT_PANEL_X, PROD_SECTION_Y, PROD_LEFT_PANEL_X + PROD_PANEL_W, PROD_SECTION_Y + prodSectionHeight, 0xCCEEEEEE);
                draw.fill(PROD_CENTER_PANEL_X, PROD_SECTION_Y, PROD_CENTER_PANEL_X + PROD_PANEL_W, PROD_SECTION_Y + prodSectionHeight, 0xCC191919);
                draw.fill(PROD_RIGHT_PANEL_X, PROD_SECTION_Y, PROD_RIGHT_PANEL_X + PROD_PANEL_W, PROD_SECTION_Y + prodSectionHeight, 0xCCEEEEEE);
                draw.drawCenteredString(Minecraft.getInstance().font, buildRecipeTitle(), WIDTH / 2, HEADER_Y + 5, 0xF0F0F0);
            }
        );

        widgets.addTexture(
            PROGRESS_BG_TEXTURE,
            PROD_CENTER_PANEL_X + 10,
            yAligned(PROD_SECTION_Y + 59),
            PROD_PROGRESS_W,
            PROD_PROGRESS_H,
            0,
            0,
            PROD_PROGRESS_W,
            PROD_PROGRESS_H,
            PROD_PROGRESS_W,
            PROD_PROGRESS_H
        );
        widgets.addAnimatedTexture(
            PROGRESS_FILL_TEXTURE,
            PROD_CENTER_PANEL_X + 11,
            yAligned(PROD_SECTION_Y + 60),
            PROD_PROGRESS_W - 2,
            PROD_PROGRESS_H - 2,
            1,
            1,
            PROD_PROGRESS_W - 2,
            PROD_PROGRESS_H - 2,
            PROD_PROGRESS_W,
            PROD_PROGRESS_H,
            Math.max(1, recipe.getCraftTime()) * 50,
            true,
            false,
            false
        );
        widgets.addText(
            Component.translatable("screen.factory_ld.constructor.production.status.working"),
            PROD_CENTER_PANEL_X + PROD_PANEL_W / 2,
            yAligned(PROD_SECTION_Y + 48),
            0x86D46C,
            false
        ).horizontalAlign(TextWidget.Alignment.CENTER);
        widgets.addText(
            Component.literal(formatPower(spec.basePowerMw())),
            PROD_CENTER_PANEL_X + PROD_PANEL_W / 2,
            yAligned(PROD_SECTION_Y + 68),
            0xCF8A34,
            false
        ).horizontalAlign(TextWidget.Alignment.CENTER);
        widgets.addText(
            Component.literal(formatCraftTimeSeconds(recipe.getCraftTime())),
            PROD_CENTER_PANEL_X + PROD_PANEL_W / 2,
            yAligned(PROD_SECTION_Y + 79),
            0xCF8A34,
            false
        ).horizontalAlign(TextWidget.Alignment.CENTER);
        widgets.addText(
            Component.literal("At 100% speed"),
            PROD_CENTER_PANEL_X + PROD_PANEL_W / 2,
            yAligned(PROD_SECTION_Y + 88),
            0xAAAAAA,
            false
        ).horizontalAlign(TextWidget.Alignment.CENTER);

        double craftsPerMinute = 1200.0 / Math.max(1, recipe.getCraftTime());

        for (int i = 0; i < recipe.getItemInputs().size(); i++) {
            MachineRecipe.InputEntry entry = recipe.getItemInputs().get(i);
            int slotX = layout.itemInputX(i);
            int slotY = yAligned(layout.itemInputY(i));
            widgets.addSlot(dev.emi.emi.api.stack.EmiIngredient.of(entry.ingredient(), Math.max(1, entry.count())), slotX, slotY)
                .recipeContext(this);
            addSlotFlowText(widgets, slotX + 20, slotY, entry.count() + " " + ingredientName(entry), entry.count() * craftsPerMinute);
        }
        for (int i = 0; i < recipe.getFluidInputs().size(); i++) {
            MachineRecipe.FluidEntry entry = recipe.getFluidInputs().get(i);
            int slotX = layout.fluidInputX(i);
            int slotY = yAligned(layout.fluidInputY(i));
            widgets.addSlot(fluidStack(entry), slotX, slotY).recipeContext(this);
            addSlotFlowText(
                widgets,
                slotX + 20,
                slotY,
                formatFluidAmountHuman(entry.amountMb()) + " " + formatFluidName(entry.fluidId()),
                entry.amountMb() * craftsPerMinute,
                true
            );
        }

        for (int i = 0; i < recipe.getItemOutputs().size(); i++) {
            var stack = recipe.getItemOutputs().get(i);
            if (stack.isEmpty()) {
                continue;
            }
            int slotX = layout.itemOutputX(i);
            int slotY = yAligned(layout.itemOutputY(i));
            widgets.addSlot(EmiStack.of(stack.copy()), slotX, slotY).recipeContext(this);
            addSlotFlowText(
                widgets,
                slotX + 20,
                slotY,
                stack.getCount() + " " + stack.getHoverName().getString(),
                stack.getCount() * craftsPerMinute
            );
        }
        for (int i = 0; i < recipe.getFluidOutputs().size(); i++) {
            MachineRecipe.FluidEntry entry = recipe.getFluidOutputs().get(i);
            int slotX = layout.fluidOutputX(i);
            int slotY = yAligned(layout.fluidOutputY(i));
            widgets.addSlot(fluidStack(entry), slotX, slotY).recipeContext(this);
            addSlotFlowText(
                widgets,
                slotX + 20,
                slotY,
                formatFluidAmountHuman(entry.amountMb()) + " " + formatFluidName(entry.fluidId()),
                entry.amountMb() * craftsPerMinute,
                true
            );
        }
    }

    private static List<dev.emi.emi.api.stack.EmiIngredient> buildInputs(MachineRecipe recipe) {
        List<dev.emi.emi.api.stack.EmiIngredient> result = new java.util.ArrayList<>();
        for (MachineRecipe.InputEntry entry : recipe.getItemInputs()) {
            result.add(dev.emi.emi.api.stack.EmiIngredient.of(entry.ingredient(), Math.max(1, entry.count())));
        }
        for (MachineRecipe.FluidEntry entry : recipe.getFluidInputs()) {
            result.add(fluidStack(entry));
        }
        return result;
    }

    private static List<EmiStack> buildOutputs(MachineRecipe recipe) {
        List<EmiStack> result = new java.util.ArrayList<>();
        for (var itemOutput : recipe.getItemOutputs()) {
            if (itemOutput.isEmpty()) {
                continue;
            }
            result.add(EmiStack.of(itemOutput.copy()));
        }
        for (MachineRecipe.FluidEntry fluidOutput : recipe.getFluidOutputs()) {
            result.add(fluidStack(fluidOutput));
        }
        return result;
    }

    private static EmiStack fluidStack(MachineRecipe.FluidEntry entry) {
        if (entry == null || entry.fluidId() == null) {
            return EmiStack.EMPTY;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(entry.fluidId()).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return EmiStack.EMPTY;
        }
        return EmiStack.of(fluid, mbToDroplets(entry.amountMb()));
    }

    private static long mbToDroplets(long mb) {
        return Math.max(1L, mb) * FluidConstants.BUCKET / 1000L;
    }

    private static String formatCraftTimeSeconds(int craftTimeTicks) {
        return String.format(Locale.ROOT, "%.2fs", Math.max(1, craftTimeTicks) / 20.0F);
    }

    private static String formatPower(double powerMw) {
        return String.format(Locale.ROOT, "%.2f MW", powerMw);
    }

    private void addSlotFlowText(WidgetHolder widgets, int x, int y, String materialLine, double perMinute) {
        addSlotFlowText(widgets, x, y, materialLine, perMinute, false);
    }

    private void addSlotFlowText(WidgetHolder widgets, int x, int y, String materialLine, double perMinute, boolean fluidRate) {
        widgets.addText(Component.literal(ellipsize(materialLine, 52)), x, y + 2, 0x2B2B2B, false);
        String rate = fluidRate ? formatFluidAmountHuman(Math.round(perMinute)) + "/m" : formatPerMinute(perMinute) + "/m";
        widgets.addText(Component.literal(rate), x, y + 10, 0xCF8A34, false);
    }

    private String ingredientName(MachineRecipe.InputEntry entry) {
        ItemStack[] options = entry.ingredient().getItems();
        if (options.length == 0 || options[0].isEmpty()) {
            return "Item";
        }
        return options[0].getHoverName().getString();
    }

    private static String formatPerMinute(double value) {
        if (value >= 100.0) {
            return Integer.toString((int)Math.round(value));
        }
        if (value >= 10.0) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private int yAligned(int y) {
        return y + EMI_CONTENT_Y_OFFSET;
    }

    private Component buildRecipeTitle() {
        Component recipeName = recipe.getRecipeNameKey().isEmpty()
            ? Component.literal(recipe.getRecipeName())
            : Component.translatable(recipe.getRecipeNameKey());
        String amount = recipe.isFluidPrimaryOutput() && !recipe.getFluidOutputs().isEmpty()
            ? formatFluidAmountHuman(recipe.getFluidOutputs().getFirst().amountMb())
            : Integer.toString(recipe.getItemOutputs().isEmpty() ? 1 : recipe.getItemOutputs().getFirst().getCount());
        return Component.translatable("screen.factory_ld.constructor.select.preview.title", amount, recipeName);
    }

    private static String formatFluidAmountHuman(long mb) {
        long safeMb = Math.max(0L, mb);
        if (safeMb < 1000L) {
            return safeMb + "mb";
        }
        if (safeMb % 1000L == 0L) {
            return (safeMb / 1000L) + "b";
        }
        String value = String.format(Locale.ROOT, "%.1f", safeMb / 1000.0);
        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }
        return value + "b";
    }

    private static String formatFluidName(ResourceLocation fluidId) {
        if (fluidId == null) {
            return "Fluid";
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return "Fluid";
        }
        ItemStack bucket = new ItemStack(fluid.getBucket());
        if (bucket.isEmpty()) {
            return "Fluid";
        }
        String name = bucket.getHoverName().getString();
        if (name.endsWith(" Bucket")) {
            return name.substring(0, name.length() - " Bucket".length());
        }
        return name;
    }

    private static String ellipsize(String value, int maxWidth) {
        var font = Minecraft.getInstance().font;
        if (font.width(value) <= maxWidth) {
            return value;
        }
        String ellipsis = "...";
        String cut = font.plainSubstrByWidth(value, Math.max(1, maxWidth - font.width(ellipsis)));
        return cut + ellipsis;
    }
}
