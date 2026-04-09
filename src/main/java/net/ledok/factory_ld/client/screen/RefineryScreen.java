package net.ledok.factory_ld.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.ledok.factory_ld.world.recipe.RefineryRecipe;
import net.ledok.factory_ld.world.block.entity.RefineryBlockEntity;
import net.ledok.factory_ld.world.screen.RefineryScreenHandler;
import net.ledok.factory_ld.world.recipe.view.MachineItemStackView;
import net.ledok.factory_ld.world.recipe.view.MachineFluidView;
import net.ledok.factory_ld.world.recipe.view.MachineRecipeView;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.ledok.factory_ld.registry.ModNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import com.mojang.blaze3d.systems.RenderSystem;

public class RefineryScreen extends AbstractContainerScreen<RefineryScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("factory_ld", "textures/gui/constructor.png");
    private static final ResourceLocation OVERCLOCK_LOCKED_TEXTURE = ResourceLocation.fromNamespaceAndPath("factory_ld", "textures/gui/constructor_overclock_locked.png");
    private static final ResourceLocation PROGRESS_BG_TEXTURE = ResourceLocation.fromNamespaceAndPath("factory_ld", "textures/gui/constructor_progress_bg.png");
    private static final ResourceLocation PROGRESS_FILL_TEXTURE = ResourceLocation.fromNamespaceAndPath("factory_ld", "textures/gui/constructor_progress_fill.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private static final int HEADER_X = 8;
    private static final int HEADER_Y = 8;
    private static final int INFO_TITLE_Y = 30;
    private static final int TAB_X = 8;
    private static final int TAB_Y = 6;
    private static final int TAB_HEIGHT = 18;
    private static final int SEARCH_Y = 28;
    private static final int SEARCH_HEIGHT = 16;
    private static final int CONTENT_X_PADDING = 8;
    private static final int CONTENT_TOP = 48;
    private static final int CONTENT_BOTTOM_PADDING = 8;
    private static final int CONTENT_SPLIT_GAP = 6;
    private static final int LIST_WIDTH_PERCENT = 60;
    private static final float LIST_RECIPE_NAME_SCALE = 0.72F;
    private static final float PREVIEW_TITLE_SCALE = 0.82F;
    private static final int BYPRODUCT_ACCENT_COLOR = 0xFFE6943A;
    private static final int PROD_HEADER_Y = 26;
    private static final int PROD_HEADER_H = 18;
    private static final int PROD_SECTION_TOP = 48;
    private static final int PROD_SECTION_H = RefineryScreenHandler.PLAYER_INV_Y - PROD_SECTION_TOP - 7;
    private static final int PROD_PANEL_W = 76;
    private static final int PROD_PANEL_GAP = 6;
    private static final int PROD_LEFT_PANEL_X = 8;
    private static final int PROD_CENTER_PANEL_X = PROD_LEFT_PANEL_X + PROD_PANEL_W + PROD_PANEL_GAP;
    private static final int PROD_RIGHT_PANEL_X = PROD_CENTER_PANEL_X + PROD_PANEL_W + PROD_PANEL_GAP;
    private static final int PROD_OVERCLOCK_Y = 194;
    private static final int PROD_OVERCLOCK_H = 25;
    private static final int PROD_OVERCLOCK_LOCKED_Y = RefineryScreenHandler.SHARD_SLOT_Y;
    private static final int PROD_OVERCLOCK_LOCKED_W = PROD_PANEL_W;
    private static final int PROD_OVERCLOCK_LOCKED_H = 60;
    private static final int PROD_PROGRESS_W = 56;
    private static final int PROD_PROGRESS_H = 6;
    private static final int OVERCLOCK_LINE_HEIGHT = 9;
    private static final float PROD_FLOW_NAME_SCALE = 0.5F;
    private static final int FLUID_SLOT_RADIUS = 9;
    private static final int FLUID_SLOT_BG = 0xFF3A3A3A;
    private static final int FLUID_SLOT_INNER = 0xFF151515;
    private static final String PRIMARY_CATEGORY = "Standart Parts";
    private static final String MACHINE_TYPE_KEY = "refinery";
    private static final Map<String, CopiedSettings> COPIED_SETTINGS = new HashMap<>();
    private Button closeButton;
    private EditBox searchBox;
    private EditBox clockBox;
    private Button clockMinusButton;
    private Button clockPlusButton;
    private Button copyButton;
    private Button pasteButton;
    private Tab currentTab = Tab.SELECT_RECIPE;
    private int lastRecipeCount = -1;
    private int previewRecipeIndex = -2;
    private int hoveredRecipeIndex = -1;
    private final List<RecipeEntry> recipeEntries = new ArrayList<>();
    private final List<HoverTarget> hoverTargets = new ArrayList<>();
    private int scrollOffset = 0;
    private final Set<String> collapsedCategories = new HashSet<>();

    public RefineryScreen(RefineryScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 256;
        this.imageHeight = 256;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = HEADER_X;
        this.titleLabelY = HEADER_Y;
        this.inventoryLabelY = RefineryScreenHandler.PLAYER_INV_Y - 11;
        clearWidgets();
        previewRecipeIndex = -2;
        recipeEntries.clear();
        scrollOffset = 0;
        addTabButtons();
        addCloseButton();
        addSearchBox();
        addOverclockControls();
        currentTab = shouldOpenProductionInitially() ? Tab.PRODUCTION : Tab.SELECT_RECIPE;
        List<MachineRecipeView> recipes = menu.getAvailableRecipeViews();
        lastRecipeCount = recipes.size();
        rebuildRecipeEntries(recipes);
        syncPreviewToSelectedRecipe(recipes);
        updateTabVisibility();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int currentCount = menu.getAvailableRecipeViews().size();
        if (currentCount != lastRecipeCount) {
            lastRecipeCount = currentCount;
            List<MachineRecipeView> recipes = menu.getAvailableRecipeViews();
            rebuildRecipeEntries(recipes);
            syncPreviewToSelectedRecipe(recipes);
            scrollOffset = 0;
        }
        syncClockField();
    }

    private void addTabButtons() {
        Button selectTab = Button.builder(Component.translatable("screen.factory_ld.constructor.tab.select_recipe"), widget -> setTab(Tab.SELECT_RECIPE))
            .bounds(leftPos + TAB_X, topPos + TAB_Y, 98, TAB_HEIGHT)
            .build();
        Button productionTab = Button.builder(Component.translatable("screen.factory_ld.constructor.tab.production"), widget -> setTab(Tab.PRODUCTION))
            .bounds(leftPos + TAB_X + 100, topPos + TAB_Y, 90, TAB_HEIGHT)
            .build();
        addRenderableWidget(selectTab);
        addRenderableWidget(productionTab);
    }

    private void addSearchBox() {
        searchBox = new EditBox(font, getContentX(), topPos + SEARCH_Y, getContentWidth(), SEARCH_HEIGHT, Component.translatable("screen.factory_ld.constructor.select.search"));
        searchBox.setMaxLength(64);
        searchBox.setBordered(true);
        addRenderableWidget(searchBox);
    }

    private void addCloseButton() {
        closeButton = Button.builder(Component.literal("X"), widget -> onClose())
            .bounds(leftPos + imageWidth - 18, topPos + 6, 12, 12)
            .build();
        addRenderableWidget(closeButton);
    }

    private void setTab(Tab tab) {
        if (currentTab == tab) {
            return;
        }
        currentTab = tab;
        updateTabVisibility();
    }

    private void updateTabVisibility() {
        boolean showRecipes = currentTab == Tab.SELECT_RECIPE;
        menu.setMachineSlotsActive(!showRecipes);
        menu.setPlayerSlotsActive(!showRecipes);
        if (searchBox != null) {
            searchBox.setVisible(showRecipes);
            searchBox.setFocused(showRecipes && searchBox.isFocused());
        }
        if (clockBox != null) {
            boolean showClockControls = !showRecipes && menu.isOverclockUnlocked();
            clockBox.setVisible(showClockControls);
            clockBox.setFocused(showClockControls && clockBox.isFocused());
        }
        if (clockMinusButton != null) {
            clockMinusButton.visible = !showRecipes && menu.isOverclockUnlocked();
            clockMinusButton.active = !showRecipes && menu.isOverclockUnlocked();
        }
        if (clockPlusButton != null) {
            clockPlusButton.visible = !showRecipes && menu.isOverclockUnlocked();
            clockPlusButton.active = !showRecipes && menu.isOverclockUnlocked();
        }
        if (copyButton != null) {
            copyButton.visible = !showRecipes;
            copyButton.active = !showRecipes;
        }
        if (pasteButton != null) {
            pasteButton.visible = !showRecipes;
            pasteButton.active = !showRecipes;
        }
    }

    private void onConfirmSelection() {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = previewRecipeIndex < 0 ? 0 : previewRecipeIndex + 1;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        setTab(Tab.PRODUCTION);
    }

    private void selectRecipeAndOpenProduction(int recipeIndex) {
        previewRecipeIndex = recipeIndex;
        onConfirmSelection();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        hoverTargets.clear();
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (currentTab == Tab.PRODUCTION) {
            renderProductionInfo(guiGraphics);
        } else {
            renderSelectedRecipe(guiGraphics, mouseX, mouseY);
        }
        renderHoverTooltips(guiGraphics, mouseX, mouseY);
    }

    private void renderSelectedRecipe(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        MachineRecipeView recipe = getHoveredRecipeView(mouseX, mouseY);
        if (recipe == null) {
            recipe = getDisplayedRecipeView();
        }
        if (recipe == null) {
            int centerX = getPreviewX() + getPreviewWidth() / 2;
            drawCenteredWrapped(guiGraphics, Component.translatable("screen.factory_ld.constructor.no_recipe"), centerX, getContentTop() + 8, getPreviewWidth() - 10, 3, 0xD8D8D8);
            return;
        }
        renderRecipePreview(guiGraphics, recipe);
    }

    private void renderProductionInfo(GuiGraphics guiGraphics) {
        int headerX = leftPos + 8;
        int headerY = topPos + PROD_HEADER_Y;
        int headerW = imageWidth - 16;
        guiGraphics.fill(headerX, headerY, headerX + headerW, headerY + PROD_HEADER_H, 0xCC171717);

        var selectedView = menu.getSelectedRecipeView();
        MachineRecipeView recipe = selectedView.orElse(null);
        if (recipe == null) {
            guiGraphics.drawCenteredString(font, Component.translatable("screen.factory_ld.constructor.no_recipe"), leftPos + imageWidth / 2, headerY + 5, 0xD8D8D8);
        } else {
            boolean fluidPrimary = menu.isFluidPrimary(recipe.id());
            String amount = fluidPrimary && !recipe.fluidOutputs().isEmpty()
                ? formatFluidAmountHuman(recipe.fluidOutputs().get(0).amountMb())
                : Integer.toString(recipe.itemOutputs().isEmpty() ? 1 : recipe.itemOutputs().get(0).amountPerCraft());
            Component title = Component.translatable(
                "screen.factory_ld.constructor.select.preview.title",
                amount,
                recipeNameComponent(recipe)
            );
            guiGraphics.drawCenteredString(font, title, leftPos + imageWidth / 2, headerY + 5, 0xF0F0F0);
        }

        int sectionY = topPos + PROD_SECTION_TOP;
        guiGraphics.fill(leftPos + PROD_LEFT_PANEL_X, sectionY, leftPos + PROD_LEFT_PANEL_X + PROD_PANEL_W, sectionY + PROD_SECTION_H, 0xCCEEEEEE);
        guiGraphics.fill(leftPos + PROD_CENTER_PANEL_X, sectionY, leftPos + PROD_CENTER_PANEL_X + PROD_PANEL_W, sectionY + PROD_SECTION_H, 0xCC191919);
        guiGraphics.fill(leftPos + PROD_RIGHT_PANEL_X, sectionY, leftPos + PROD_RIGHT_PANEL_X + PROD_PANEL_W, sectionY + PROD_SECTION_H, 0xCCEEEEEE);
        int leftFlowY = topPos + menu.slots.get(0).y;
        int rightFlowY = topPos + menu.slots.get(1).y;
        if (recipe == null) {
            renderFlowColumn(guiGraphics, leftPos + PROD_LEFT_PANEL_X + 4, leftFlowY, PROD_PANEL_W - 8, List.of(), List.of(), 0.0);
            renderCenterProductionPanelEmpty(guiGraphics, sectionY + 6);
            renderFlowColumn(guiGraphics, leftPos + PROD_RIGHT_PANEL_X + 4, rightFlowY, PROD_PANEL_W - 8, List.of(), List.of(), 0.0);
        } else {
            double clockPercent = menu.getBlockEntity().getClockSpeedPercent();
            double craftsPerMinute = 1200.0 / Math.max(1, recipe.craftTimeTicks()) * (clockPercent / 100.0);
            renderFlowColumn(guiGraphics, leftPos + PROD_LEFT_PANEL_X + 4, leftFlowY, PROD_PANEL_W - 8, recipe.itemInputs(), recipe.fluidInputs(), craftsPerMinute);
            renderCenterProductionPanel(guiGraphics, recipe, sectionY + 6);
            renderFlowColumn(guiGraphics, leftPos + PROD_RIGHT_PANEL_X + 4, rightFlowY, PROD_PANEL_W - 8, recipe.itemOutputs(), recipe.fluidOutputs(), craftsPerMinute);
        }
        if (menu.isOverclockUnlocked()) {
            renderOverclockInfo(guiGraphics);
        } else {
            renderLockedOverclockPlaceholder(guiGraphics);
        }
    }

    private void renderFlowColumn(
        GuiGraphics guiGraphics,
        int x,
        int y,
        int width,
        List<MachineItemStackView> items,
        List<MachineFluidView> fluids,
        double craftsPerMinute
    ) {
        int rowY = y;
        for (MachineItemStackView item : items) {
            if (item.isEmpty()) {
                continue;
            }
            guiGraphics.renderItem(item.stack(), x, rowY);
            Component line = Component.literal(item.amountPerCraft() + " " + item.stack().getHoverName().getString());
            drawEllipsizedScaled(guiGraphics, line, x + 18, rowY + 2, width - 20, 0x2B2B2B, PROD_FLOW_NAME_SCALE);
            float perMinute = (float)(item.amountPerCraft() * craftsPerMinute);
            guiGraphics.drawString(font, Component.literal(formatPerMinute(perMinute) + "/m"), x + 18, rowY + 10, 0xCF8A34, false);
            rowY += 20;
            if (rowY > y + PROD_SECTION_H - 18) {
                return;
            }
        }
        for (MachineFluidView fluid : fluids) {
            drawFluidCircleSlot(guiGraphics, x + 8, rowY + 8);
            drawFluidGlyph(guiGraphics, fluid.fluidId(), x + 8, rowY + 8, 7);
            drawEllipsizedScaled(
                guiGraphics,
                Component.literal(formatFluidAmountHuman(fluid.amountMb()) + " " + formatFluidName(fluid.fluidId())),
                x + 18,
                rowY + 2,
                width - 20,
                0x2B2B2B,
                PROD_FLOW_NAME_SCALE
            );
            float perMinuteMb = (float)(fluid.amountMb() * craftsPerMinute);
            guiGraphics.drawString(font, Component.literal(formatFluidAmountHuman(Math.round(perMinuteMb)) + "/m"), x + 18, rowY + 10, 0xCF8A34, false);
            rowY += 20;
            if (rowY > y + PROD_SECTION_H - 18) {
                return;
            }
        }
    }

    private String formatPerMinute(float value) {
        if (value >= 100) {
            return Integer.toString(Math.round(value));
        }
        if (value >= 10) {
            return String.format(Locale.ROOT, "%.1f", value);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private void renderCenterProductionPanel(GuiGraphics guiGraphics, MachineRecipeView recipe, int y) {
        int centerX = leftPos + PROD_CENTER_PANEL_X + PROD_PANEL_W / 2;
        boolean fluidPrimary = menu.isFluidPrimary(recipe.id());
        if (fluidPrimary && !recipe.fluidOutputs().isEmpty()) {
            drawFluidGlyph(guiGraphics, recipe.fluidOutputs().get(0).fluidId(), centerX, y + 20, 14);
            if (!recipe.itemOutputs().isEmpty()) {
                ItemStack by = recipe.itemOutputs().get(0).stack().copy();
                by.setCount(1);
                drawCircle(guiGraphics, centerX + 20, y + 32, 10, BYPRODUCT_ACCENT_COLOR);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(centerX + 14, y + 26, 0);
                guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
                guiGraphics.renderItem(by, 0, 0);
                guiGraphics.pose().popPose();
            }
        } else if (!recipe.itemOutputs().isEmpty()) {
            ItemStack main = recipe.itemOutputs().get(0).stack().copy();
            main.setCount(1);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerX - 16, y + 4, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(main, 0, 0);
            guiGraphics.pose().popPose();
            if (recipe.itemOutputs().size() > 1 || !recipe.fluidOutputs().isEmpty()) {
                boolean fluidByproduct = recipe.itemOutputs().size() <= 1;
                drawCircle(guiGraphics, centerX + 20, y + 32, 10, BYPRODUCT_ACCENT_COLOR);
                if (fluidByproduct) {
                    drawFluidGlyph(guiGraphics, recipe.fluidOutputs().get(0).fluidId(), centerX + 20, y + 32, 7);
                } else {
                    ItemStack by = recipe.itemOutputs().get(1).stack().copy();
                    by.setCount(1);
                    guiGraphics.pose().pushPose();
                    guiGraphics.pose().translate(centerX + 14, y + 26, 0);
                    guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
                    guiGraphics.renderItem(by, 0, 0);
                    guiGraphics.pose().popPose();
                }
            }
        }
        renderCenterProductionDetails(guiGraphics, centerX, y, Math.max(1, recipe.craftTimeTicks()));
    }

    private void renderCenterProductionPanelEmpty(GuiGraphics guiGraphics, int y) {
        int centerX = leftPos + PROD_CENTER_PANEL_X + PROD_PANEL_W / 2;
        renderCenterProductionDetails(guiGraphics, centerX, y, -1);
    }

    private void renderCenterProductionDetails(GuiGraphics guiGraphics, int centerX, int y, int craftTimeTicks) {
        ProductionStatus status = getProductionStatus();
        int statusColor = switch (status) {
            case WORKING -> 0x86D46C;
            case NO_POWER -> 0xD16565;
            case IDLE -> 0xAAAAAA;
        };
        guiGraphics.drawCenteredString(
            font,
            Component.translatable("screen.factory_ld.constructor.production.status." + status.key),
            centerX,
            y + 42,
            statusColor
        );

        float progress = 0.0F;
        if (craftTimeTicks > 0) {
            progress = (float)Math.max(0.0, Math.min(1.0, menu.getBlockEntity().getProgress() / craftTimeTicks));
        }
        renderProgressBar(guiGraphics, centerX - PROD_PROGRESS_W / 2, y + 53, progress);

        guiGraphics.drawCenteredString(font, Component.literal(formatPower(menu.getBlockEntity().getPowerUsageMw())), centerX, y + 62, 0xCF8A34);
        if (craftTimeTicks > 0) {
            guiGraphics.drawCenteredString(font, Component.literal(String.format(Locale.ROOT, "%.2fs", craftTimeTicks / 20.0f)), centerX, y + 71, 0xCF8A34);
        } else {
            guiGraphics.drawCenteredString(font, Component.literal("--"), centerX, y + 71, 0xCF8A34);
        }
    }

    private void renderProgressBar(GuiGraphics guiGraphics, int x, int y, float progress) {
        guiGraphics.blit(
            PROGRESS_BG_TEXTURE,
            x,
            y,
            0,
            0,
            PROD_PROGRESS_W,
            PROD_PROGRESS_H,
            PROD_PROGRESS_W,
            PROD_PROGRESS_H
        );
        int fillWidth = Math.max(0, Math.min(PROD_PROGRESS_W - 2, Math.round((PROD_PROGRESS_W - 2) * progress)));
        if (fillWidth > 0) {
            guiGraphics.blit(
                PROGRESS_FILL_TEXTURE,
                x + 1,
                y + 1,
                1,
                1,
                fillWidth,
                PROD_PROGRESS_H - 2,
                PROD_PROGRESS_W,
                PROD_PROGRESS_H
            );
        }
    }

    private ProductionStatus getProductionStatus() {
        var selectedRecipe = menu.getBlockEntity().getSelectedRecipe();
        if (selectedRecipe.isEmpty()) {
            return ProductionStatus.IDLE;
        }
        RefineryRecipe recipe = selectedRecipe.get();
        ItemStack input = menu.getBlockEntity().getItems().get(RefineryBlockEntity.INPUT_SLOT);
        ItemStack output = menu.getBlockEntity().getItems().get(RefineryBlockEntity.OUTPUT_SLOT);
        int inputCount = Math.max(1, recipe.getInputCount());
        if (input.isEmpty() || input.getCount() < inputCount || !recipe.getInput().test(input)) {
            return ProductionStatus.IDLE;
        }
        ItemStack result = recipe.getOutput();
        if (result.isEmpty()) {
            return ProductionStatus.IDLE;
        }
        RefineryRecipe.FluidEntry inFluid = recipe.getFluidInputEntry(0);
        RefineryRecipe.FluidEntry outFluid = recipe.getFluidOutputEntry(0);
        if (inFluid == null || outFluid == null) {
            return ProductionStatus.IDLE;
        }
        Fluid requiredInput = BuiltInRegistries.FLUID.getOptional(inFluid.fluidId()).orElse(null);
        Fluid requiredOutput = BuiltInRegistries.FLUID.getOptional(outFluid.fluidId()).orElse(null);
        if (requiredInput == null || requiredOutput == null) {
            return ProductionStatus.IDLE;
        }
        var inputVariant = menu.getBlockEntity().getInputFluidVariant();
        long inputRequiredMb = Math.max(1L, inFluid.amountMb());
        if (inputVariant.isBlank() || !inputVariant.isOf(requiredInput) || menu.getBlockEntity().getInputFluidMb() < inputRequiredMb) {
            return ProductionStatus.IDLE;
        }
        var outputVariant = menu.getBlockEntity().getOutputFluidVariant();
        long outputResultMb = Math.max(1L, outFluid.amountMb());
        if (!outputVariant.isBlank() && !outputVariant.isOf(requiredOutput)) {
            return ProductionStatus.IDLE;
        }
        if (menu.getBlockEntity().getOutputFluidMb() + outputResultMb > 50000L) {
            return ProductionStatus.IDLE;
        }
        if (!output.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(output, result)) {
                return ProductionStatus.IDLE;
            }
            if (output.getCount() + result.getCount() > output.getMaxStackSize()) {
                return ProductionStatus.IDLE;
            }
        }
        double powerPerTick = menu.getBlockEntity().getPowerUsageMw() / 20.0;
        if (menu.getBlockEntity().getEnergyStored() < powerPerTick) {
            return ProductionStatus.NO_POWER;
        }
        return ProductionStatus.WORKING;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float delta, int mouseX, int mouseY) {
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        if (currentTab == Tab.SELECT_RECIPE) {
            renderRecipePanel(guiGraphics, mouseX, mouseY);
        }
        if (currentTab == Tab.PRODUCTION) {
            for (int i = 0; i < menu.slots.size(); i++) {
                Slot slot = menu.slots.get(i);
                if (!slot.isActive()) {
                    continue;
                }
                guiGraphics.blitSprite(SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
            }
            renderFluidSlots(guiGraphics);
        }
        renderGhostItems(guiGraphics);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Intentionally left blank: custom production/select layouts do not use vanilla title labels.
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        if (currentTab != Tab.PRODUCTION) {
            return;
        }
        RecipeHolder<RefineryRecipe> displayed = getDisplayedRecipeHolder();
        if (displayed == null) {
            return;
        }
        RefineryRecipe recipe = displayed.value();
        Slot inputSlot = menu.slots.get(0);
        Slot outputSlot = menu.slots.get(1);

        if (!inputSlot.hasItem()) {
            ItemStack[] matching = recipe.getInput().getItems();
            if (matching.length > 0) {
                ItemStack ghost = matching[0].copy();
                ghost.setCount(1);
                renderGhost(guiGraphics, ghost, leftPos + inputSlot.x, topPos + inputSlot.y);
            }
        }

        if (!outputSlot.hasItem()) {
            ItemStack ghost = recipe.getOutput().copy();
            ghost.setCount(1);
            renderGhost(guiGraphics, ghost, leftPos + outputSlot.x, topPos + outputSlot.y);
        }
        RefineryRecipe.FluidEntry inputFluid = recipe.getFluidInputEntry(0);
        if (inputFluid != null) {
            renderGhostFluidGlyph(guiGraphics, inputFluid.fluidId(), leftPos + RefineryScreenHandler.FLUID_INPUT_X + 8, topPos + RefineryScreenHandler.FLUID_INPUT_Y + 8, 7);
        }
        RefineryRecipe.FluidEntry outputFluid = recipe.getFluidOutputEntry(0);
        if (outputFluid != null) {
            renderGhostFluidGlyph(guiGraphics, outputFluid.fluidId(), leftPos + RefineryScreenHandler.FLUID_OUTPUT_X + 8, topPos + RefineryScreenHandler.FLUID_OUTPUT_Y + 8, 7);
        }
    }

    private void renderFluidSlots(GuiGraphics guiGraphics) {
        int inputX = leftPos + RefineryScreenHandler.FLUID_INPUT_X + 8;
        int inputY = topPos + RefineryScreenHandler.FLUID_INPUT_Y + 8;
        int outputX = leftPos + RefineryScreenHandler.FLUID_OUTPUT_X + 8;
        int outputY = topPos + RefineryScreenHandler.FLUID_OUTPUT_Y + 8;
        drawFluidCircleSlot(guiGraphics, inputX, inputY);
        drawFluidCircleSlot(guiGraphics, outputX, outputY);

        ResourceLocation inputFluidId = BuiltInRegistries.FLUID.getKey(menu.getBlockEntity().getInputFluidVariant().getFluid());
        if (inputFluidId != null && !menu.getBlockEntity().getInputFluidVariant().isBlank()) {
            drawFluidGlyph(guiGraphics, inputFluidId, inputX, inputY, 7);
            drawFluidAmountOverlay(guiGraphics, inputX - 8, inputY - 8, formatFluidAmountHuman(menu.getBlockEntity().getInputFluidMb()));
        }
        ResourceLocation outputFluidId = BuiltInRegistries.FLUID.getKey(menu.getBlockEntity().getOutputFluidVariant().getFluid());
        if (outputFluidId != null && !menu.getBlockEntity().getOutputFluidVariant().isBlank()) {
            drawFluidGlyph(guiGraphics, outputFluidId, outputX, outputY, 7);
            drawFluidAmountOverlay(guiGraphics, outputX - 8, outputY - 8, formatFluidAmountHuman(menu.getBlockEntity().getOutputFluidMb()));
        }
    }

    private void drawFluidCircleSlot(GuiGraphics guiGraphics, int centerX, int centerY) {
        drawCircle(guiGraphics, centerX, centerY, FLUID_SLOT_RADIUS, FLUID_SLOT_BG);
        drawCircle(guiGraphics, centerX, centerY, FLUID_SLOT_RADIUS - 1, FLUID_SLOT_INNER);
    }

    private String formatFluidName(ResourceLocation fluidId) {
        if (fluidId == null) {
            return "Fluid";
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(null);
        if (fluid == null) {
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

    private String formatFluidAmountHuman(long mb) {
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

    private RecipeHolder<RefineryRecipe> getDisplayedRecipeHolder() {
        return menu.getSelectedRecipe().map(recipe -> {
            List<RecipeHolder<RefineryRecipe>> recipes = menu.getAvailableRecipes();
            for (RecipeHolder<RefineryRecipe> holder : recipes) {
                if (holder.value() == recipe) {
                    return holder;
                }
            }
            return null;
        }).orElse(null);
    }

    private MachineRecipeView getDisplayedRecipeView() {
        if (previewRecipeIndex == -1) {
            return null;
        }
        if (previewRecipeIndex >= 0) {
            return menu.getRecipeView(previewRecipeIndex).orElse(null);
        }
        return null;
    }

    private void renderGhost(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        guiGraphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        hoverTargets.add(HoverTarget.item(x, y, 16, 16, stack.copy()));
    }

    private void renderGhostFluidGlyph(GuiGraphics guiGraphics, ResourceLocation fluidId, int centerX, int centerY, int radius) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        drawFluidGlyph(guiGraphics, fluidId, centerX, centerY, radius);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        hoverTargets.add(HoverTarget.text(centerX - radius, centerY - radius, radius * 2 + 1, radius * 2 + 1, Component.literal(formatFluidName(fluidId))));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (currentTab == Tab.SELECT_RECIPE && handleRecipeClick(mouseX, mouseY)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            return true;
        }
        if (currentTab == Tab.SELECT_RECIPE && searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        if (currentTab == Tab.PRODUCTION && menu.isOverclockUnlocked() && clockBox != null && clockBox.isFocused()) {
            if (keyCode == 257 || keyCode == 335) {
                submitClock();
                return true;
            }
            if (clockBox.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (currentTab == Tab.SELECT_RECIPE && searchBox != null && searchBox.isFocused()) {
            if (searchBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        if (currentTab == Tab.PRODUCTION && menu.isOverclockUnlocked() && clockBox != null && clockBox.isFocused()) {
            if (clockBox.charTyped(codePoint, modifiers)) {
                return true;
            }
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void renderRecipePanel(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int listX = getListX();
        int listY = getContentTop();
        int listW = getListWidth();
        int listBottom = getContentBottom();

        int previewX = getPreviewX();
        int previewW = getPreviewWidth();

        guiGraphics.fill(listX, listY, listX + listW, listBottom, 0xAA161616);
        guiGraphics.fill(previewX, listY, previewX + previewW, listBottom, 0xAA1F1F1F);

        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        int xStart = listX + 4;
        int contentTop = listY + 4;
        int contentBottom = listBottom - 4;
        int contentHeight = Math.max(0, contentBottom - contentTop);
        int cellH = 42;
        int cols = Math.max(1, (listW - 12) / 42);
        int cellW = Math.max(38, (listW - 12) / cols);
        Layout layout = computeLayout(query, cols, cellH);

        int maxScroll = Math.max(0, layout.contentHeight - contentHeight);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }
        hoveredRecipeIndex = -1;

        guiGraphics.enableScissor(listX, contentTop, listX + listW - 6, contentBottom);
        int y = contentTop - scrollOffset;
        for (String category : layout.categories) {
            boolean collapsed = collapsedCategories.contains(category);
            guiGraphics.fill(xStart, y, listX + listW - 8, y + 11, 0xFF3A3A3A);
            guiGraphics.drawString(font, Component.literal(collapsed ? "+" : "-"), xStart + 2, y + 2, 0xFFFFFF, false);
            guiGraphics.drawString(font, Component.literal(category), xStart + 12, y + 2, 0xFFFFFF, false);
            y += 13;
            if (collapsed) {
                y += 4;
                continue;
            }
            List<RecipeEntry> entries = new ArrayList<>(layout.byCategory.get(category));
            entries.sort(this::compareRecipeEntries);
            for (int i = 0; i < entries.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int cellX = xStart + col * cellW;
                int cellY = y + row * cellH;
                boolean hovered = mouseX >= cellX && mouseX <= cellX + cellW - 2 && mouseY >= cellY && mouseY <= cellY + cellH - 2;
                if (hovered) {
                    hoveredRecipeIndex = entries.get(i).index;
                }
                renderRecipeEntry(guiGraphics, entries.get(i), cellX, cellY, cellW, hovered);
            }
            int rows = (entries.size() + cols - 1) / cols;
            y += rows * cellH + 6;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int barX = listX + listW - 5;
            int barY = contentTop;
            int barH = contentHeight;
            guiGraphics.fill(barX, barY, barX + 3, barY + barH, 0xFF2F2F2F);
            int thumbH = Math.max(10, (int)((float)barH * barH / layout.contentHeight));
            int thumbY = barY + (int)((float)scrollOffset * (barH - thumbH) / maxScroll);
            guiGraphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFF838383);
        }
    }

    private void renderOverclockInfo(GuiGraphics guiGraphics) {
        double clock = menu.getBlockEntity().getClockSpeedPercent();

        int x = leftPos + PROD_RIGHT_PANEL_X;
        int y = topPos + PROD_OVERCLOCK_Y;
        int w = PROD_PANEL_W;
        guiGraphics.fill(x, y, x + w, y + PROD_OVERCLOCK_H, 0xCC1D1D1D);
        guiGraphics.drawString(font, Component.translatable("screen.factory_ld.constructor.overclock.title"), x + 4, y + 3, 0xD8D8D8, false);
        guiGraphics.drawString(
            font,
            Component.translatable("screen.factory_ld.constructor.overclock.clock", formatPercent(clock)),
            x + 4,
            y + 3 + OVERCLOCK_LINE_HEIGHT,
            0xCF8A34,
            false
        );
    }

    private void renderLockedOverclockPlaceholder(GuiGraphics guiGraphics) {
        int x = leftPos + PROD_RIGHT_PANEL_X;
        int y = topPos + PROD_OVERCLOCK_LOCKED_Y;
        guiGraphics.blit(
            OVERCLOCK_LOCKED_TEXTURE,
            x,
            y,
            0,
            0,
            PROD_OVERCLOCK_LOCKED_W,
            PROD_OVERCLOCK_LOCKED_H,
            PROD_OVERCLOCK_LOCKED_W,
            PROD_OVERCLOCK_LOCKED_H
        );
    }

    private void renderRecipeEntry(GuiGraphics guiGraphics, RecipeEntry entry, int x, int y, int cellW, boolean hovered) {
        int bgColor = entry.index == previewRecipeIndex ? 0xFF9E6A37 : hovered ? 0xFF5A5A5A : 0xFF3A3A3A;
        guiGraphics.fill(x, y, x + cellW - 2, y + 40, bgColor);
        boolean hasByproduct = !entry.byproductIcon.isEmpty() || entry.byproductFluidId != null;
        int iconY = y + 3;
        if (hasByproduct) {
            int mainCenterX = x + cellW / 2;
            int mainCenterY = iconY + 8;
            drawCircle(guiGraphics, mainCenterX, mainCenterY, 9, 0xAA262626);
            if (entry.primaryFluidId != null) {
                drawFluidGlyph(guiGraphics, entry.primaryFluidId, mainCenterX, mainCenterY, 7);
            } else {
                guiGraphics.renderItem(entry.outputIcon, mainCenterX - 8, iconY);
            }
            int badgeCenterX = mainCenterX + 7;
            int badgeCenterY = mainCenterY + 7;
            drawCircle(guiGraphics, badgeCenterX, badgeCenterY, 6, BYPRODUCT_ACCENT_COLOR);
            if (!entry.byproductIcon.isEmpty()) {
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(badgeCenterX - 4, badgeCenterY - 4, 0);
                guiGraphics.pose().scale(0.5F, 0.5F, 1.0F);
                guiGraphics.renderItem(entry.byproductIcon, 0, 0);
                guiGraphics.pose().popPose();
            } else {
                drawFluidGlyph(guiGraphics, entry.byproductFluidId, badgeCenterX, badgeCenterY, 4);
            }
        } else {
            if (entry.primaryFluidId != null) {
                drawFluidGlyph(guiGraphics, entry.primaryFluidId, x + cellW / 2, iconY + 8, 7);
            } else {
                guiGraphics.renderItem(entry.outputIcon, x + (cellW - 16) / 2 - 1, iconY);
            }
        }
        drawCenteredWrappedScaled(
            guiGraphics,
            Component.literal(entry.name),
            x + cellW / 2,
            y + 20,
            cellW - 4,
            3,
            0xFFFFFF,
            LIST_RECIPE_NAME_SCALE
        );
    }

    private boolean handleRecipeClick(double mouseX, double mouseY) {
        int listX = getListX();
        int listW = getListWidth();
        int contentTop = getContentTop() + 4;
        int contentBottom = getContentBottom() - 4;
        if (mouseX < listX || mouseX > listX + listW || mouseY < contentTop || mouseY > contentBottom) {
            return false;
        }
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        int xStart = listX + 4;
        int cellH = 42;
        int cols = Math.max(1, (listW - 12) / 42);
        int cellW = Math.max(38, (listW - 12) / cols);
        Layout layout = computeLayout(query, cols, cellH);

        int y = contentTop - scrollOffset;

        for (String category : layout.categories) {
            boolean collapsed = collapsedCategories.contains(category);
            if (mouseX >= xStart && mouseX <= xStart + 10 && mouseY >= y && mouseY <= y + 11) {
                if (collapsed) {
                    collapsedCategories.remove(category);
                } else {
                    collapsedCategories.add(category);
                }
                return true;
            }
            y += 13;
            if (collapsed) {
                y += 4;
                continue;
            }
            List<RecipeEntry> entries = new ArrayList<>(layout.byCategory.get(category));
            entries.sort(this::compareRecipeEntries);
            for (int i = 0; i < entries.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int cellX = xStart + col * cellW;
                int cellY = y + row * cellH;
                if (mouseX >= cellX && mouseX <= cellX + cellW - 2 && mouseY >= cellY && mouseY <= cellY + cellH - 2) {
                    selectRecipeAndOpenProduction(entries.get(i).index);
                    return true;
                }
            }
            int rows = (entries.size() + cols - 1) / cols;
            y += rows * cellH + 6;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab != Tab.SELECT_RECIPE) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int listX = getListX();
        int listW = getListWidth();
        int contentTop = getContentTop() + 4;
        int contentBottom = getContentBottom() - 4;
        if (mouseX < listX || mouseX > listX + listW || mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        int cellH = 42;
        int cols = Math.max(1, (listW - 12) / 42);
        Layout layout = computeLayout(query, cols, cellH);
        int contentHeight = contentBottom - contentTop;
        int maxScroll = Math.max(0, layout.contentHeight - contentHeight);
        if (maxScroll == 0) {
            return true;
        }
        int scrollStep = 16;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * scrollStep)));
        return true;
    }

    private void rebuildRecipeEntries(List<MachineRecipeView> recipes) {
        recipeEntries.clear();
        String emptyName = Component.translatable("screen.factory_ld.constructor.select.empty_recipe").getString();
        recipeEntries.add(new RecipeEntry(-1, emptyName, PRIMARY_CATEGORY, new ItemStack(Items.BARRIER), ItemStack.EMPTY, null, null, emptyName.toLowerCase(Locale.ROOT)));
        for (int i = 0; i < recipes.size(); i++) {
            MachineRecipeView recipe = recipes.get(i);
            boolean fluidPrimary = menu.isFluidPrimary(recipe.id());
            ItemStack output = fluidPrimary
                ? ItemStack.EMPTY
                : (recipe.itemOutputs().isEmpty() ? ItemStack.EMPTY : recipe.itemOutputs().get(0).stack());
            ResourceLocation primaryFluidId = fluidPrimary && !recipe.fluidOutputs().isEmpty()
                ? recipe.fluidOutputs().get(0).fluidId()
                : null;
            ItemStack byproduct = fluidPrimary
                ? (recipe.itemOutputs().isEmpty() ? ItemStack.EMPTY : recipe.itemOutputs().get(0).stack())
                : (recipe.itemOutputs().size() > 1 ? recipe.itemOutputs().get(1).stack() : ItemStack.EMPTY);
            ResourceLocation byproductFluidId = fluidPrimary
                ? (recipe.fluidOutputs().size() > 1 ? recipe.fluidOutputs().get(1).fluidId() : null)
                : (byproduct.isEmpty() && !recipe.fluidOutputs().isEmpty() ? recipe.fluidOutputs().get(0).fluidId() : null);
            String name = getRecipeNameText(recipe);
            String category = recipe.category();
            String outputName = fluidPrimary
                ? formatFluidName(primaryFluidId)
                : output.getHoverName().getString();
            String byproductName = !byproduct.isEmpty()
                ? byproduct.getHoverName().getString()
                : (byproductFluidId == null ? "" : formatFluidName(byproductFluidId));
            String searchKey = (name + " " + outputName + " " + byproductName).toLowerCase(Locale.ROOT);
            recipeEntries.add(new RecipeEntry(i, name, category, output, byproduct, primaryFluidId, byproductFluidId, searchKey));
        }
    }

    private void syncPreviewToSelectedRecipe(List<MachineRecipeView> recipes) {
        ResourceLocation selectedId = menu.getSelectedRecipeId();
        if (selectedId == null) {
            previewRecipeIndex = -1;
            return;
        }
        for (int i = 0; i < recipes.size(); i++) {
            if (selectedId.equals(recipes.get(i).id())) {
                previewRecipeIndex = i;
                return;
            }
        }
        previewRecipeIndex = -1;
    }

    private Layout computeLayout(String query, int cols, int cellH) {
        List<RecipeEntry> filtered = recipeEntries.stream()
            .filter(entry -> query.isEmpty() || entry.searchKey.contains(query))
            .toList();

        Map<String, List<RecipeEntry>> byCategory = filtered.stream()
            .collect(Collectors.groupingBy(entry -> entry.category));

        List<String> categories = byCategory.keySet().stream()
            .sorted((a, b) -> {
                boolean aPrimary = PRIMARY_CATEGORY.equalsIgnoreCase(a);
                boolean bPrimary = PRIMARY_CATEGORY.equalsIgnoreCase(b);
                if (aPrimary && !bPrimary) {
                    return -1;
                }
                if (!aPrimary && bPrimary) {
                    return 1;
                }
                return a.compareToIgnoreCase(b);
            })
            .toList();

        int contentHeight = 0;
        for (String category : categories) {
            contentHeight += 13;
            if (collapsedCategories.contains(category)) {
                contentHeight += 4;
                continue;
            }
            int size = byCategory.get(category).size();
            int rows = (size + cols - 1) / cols;
            contentHeight += rows * cellH + 6;
        }
        return new Layout(byCategory, categories, contentHeight);
    }

    private int getContentX() {
        return leftPos + CONTENT_X_PADDING;
    }

    private int getContentWidth() {
        return imageWidth - CONTENT_X_PADDING * 2;
    }

    private int getContentTop() {
        return topPos + CONTENT_TOP;
    }

    private int getContentBottom() {
        return topPos + imageHeight - CONTENT_BOTTOM_PADDING;
    }

    private int getListX() {
        return getContentX();
    }

    private int getListWidth() {
        int available = getContentWidth() - CONTENT_SPLIT_GAP;
        return Math.max(64, available * LIST_WIDTH_PERCENT / 100);
    }

    private int getPreviewX() {
        return getListX() + getListWidth() + CONTENT_SPLIT_GAP;
    }

    private int getPreviewWidth() {
        return getContentX() + getContentWidth() - getPreviewX();
    }

    private MachineRecipeView getRecipeByIndex(int index) {
        if (index < 0) {
            return null;
        }
        return menu.getRecipeView(index).orElse(null);
    }

    private MachineRecipeView getHoveredRecipeView(int mouseX, int mouseY) {
        if (currentTab != Tab.SELECT_RECIPE || hoveredRecipeIndex < 0) {
            return null;
        }
        if (mouseX < getListX() || mouseX > getListX() + getListWidth() || mouseY < getContentTop() || mouseY > getContentBottom()) {
            return null;
        }
        return getRecipeByIndex(hoveredRecipeIndex);
    }

    private void renderRecipePreview(GuiGraphics guiGraphics, MachineRecipeView recipe) {
        int panelX = getPreviewX();
        int panelW = getPreviewWidth();
        int centerX = panelX + panelW / 2;
        int y = getContentTop() + 6;
        boolean fluidPrimary = menu.isFluidPrimary(recipe.id());
        MachineItemStackView primaryItem = fluidPrimary
            ? new MachineItemStackView(ItemStack.EMPTY, 0)
            : (recipe.itemOutputs().isEmpty() ? new MachineItemStackView(ItemStack.EMPTY, 1) : recipe.itemOutputs().get(0));
        MachineFluidView primaryFluid = fluidPrimary && !recipe.fluidOutputs().isEmpty() ? recipe.fluidOutputs().get(0) : null;
        MachineItemStackView byproductOutputView = fluidPrimary
            ? (recipe.itemOutputs().isEmpty() ? new MachineItemStackView(ItemStack.EMPTY, 0) : recipe.itemOutputs().get(0))
            : (recipe.itemOutputs().size() > 1 ? recipe.itemOutputs().get(1) : new MachineItemStackView(ItemStack.EMPTY, 0));
        MachineFluidView byproductFluid = fluidPrimary
            ? (recipe.fluidOutputs().size() > 1 ? recipe.fluidOutputs().get(1) : null)
            : (recipe.itemOutputs().size() > 1 || recipe.fluidOutputs().isEmpty() ? null : recipe.fluidOutputs().get(0));
        ItemStack mainOutput = primaryItem.stack();
        ItemStack byproductOutput = byproductOutputView.stack();
        boolean hasByproduct = !byproductOutput.isEmpty() || byproductFluid != null;

        Component title;
        if (hasByproduct) {
            Component byproductName = !byproductOutput.isEmpty()
                ? byproductOutput.getHoverName()
                : Component.literal(formatFluidName(byproductFluid.fluidId()));
            int byproductAmount = !byproductOutput.isEmpty()
                ? byproductOutputView.amountPerCraft()
                : (int)Math.max(1L, byproductFluid.amountMb());
            title = Component.translatable(
                "screen.factory_ld.constructor.select.preview.title_with_byproduct",
                fluidPrimary ? formatFluidAmountHuman(primaryFluid.amountMb()) : primaryItem.amountPerCraft(),
                recipeNameComponent(recipe),
                byproductAmount,
                byproductName
            );
        } else {
            title = Component.translatable(
                "screen.factory_ld.constructor.select.preview.title",
                fluidPrimary ? formatFluidAmountHuman(primaryFluid.amountMb()) : primaryItem.amountPerCraft(),
                recipeNameComponent(recipe)
            );
        }
        y = drawCenteredWrappedScaled(guiGraphics, title, centerX, y, panelW - 10, 3, 0xF0F0F0, PREVIEW_TITLE_SCALE) + 2;

        int iconX = centerX - 16;
        int iconY = y;
        if (hasByproduct) {
            drawCircle(guiGraphics, centerX, iconY + 16, 18, 0xAA262626);
        }
        if (fluidPrimary && primaryFluid != null) {
            drawFluidGlyph(guiGraphics, primaryFluid.fluidId(), centerX, iconY + 16, 14);
        } else {
            ItemStack output = mainOutput.copy();
            output.setCount(1);
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(iconX, iconY, 0);
            guiGraphics.pose().scale(2.0F, 2.0F, 1.0F);
            guiGraphics.renderItem(output, 0, 0);
            guiGraphics.pose().popPose();
        }
        if (hasByproduct) {
            int badgeCenterX = centerX + 24;
            int badgeCenterY = iconY + 28;
            drawCircle(guiGraphics, badgeCenterX, badgeCenterY, 11, BYPRODUCT_ACCENT_COLOR);
            if (!byproductOutput.isEmpty()) {
                ItemStack by = byproductOutput.copy();
                by.setCount(1);
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate(badgeCenterX - 6, badgeCenterY - 6, 0);
                guiGraphics.pose().scale(0.75F, 0.75F, 1.0F);
                guiGraphics.renderItem(by, 0, 0);
                guiGraphics.pose().popPose();
            } else {
                drawFluidGlyph(guiGraphics, byproductFluid.fluidId(), badgeCenterX, badgeCenterY, 7);
            }
        }
        y += 40;

        guiGraphics.drawCenteredString(font, Component.translatable("screen.factory_ld.constructor.select.preview.info"), centerX, y, 0xD0D0D0);
        y += 12;
        y = drawCenteredWrapped(
            guiGraphics,
            outputDescriptionComponent(mainOutput),
            centerX,
            y,
            panelW - 10,
            3,
            0xD0D0D0
        );

        if (hasByproduct) {
            y += 6;
            guiGraphics.drawCenteredString(font, Component.translatable("screen.factory_ld.constructor.select.preview.byproduct"), centerX, y, 0xD0D0D0);
            y += 12;
            y = drawCenteredWrapped(
                guiGraphics,
                !byproductOutput.isEmpty()
                    ? outputDescriptionComponent(byproductOutput)
                    : fluidDescriptionComponent(byproductFluid.fluidId()),
                centerX,
                y,
                panelW - 10,
                3,
                0xD0D0D0
            );
        }

        y += 6;
        guiGraphics.drawCenteredString(font, Component.translatable("screen.factory_ld.constructor.select.preview.stats"), centerX, y, 0xD0D0D0);
        y += 12;
        String timeText = String.format(Locale.ROOT, "%.2fs", Math.max(1, recipe.craftTimeTicks()) / 20.0f);
        guiGraphics.drawCenteredString(font, Component.literal(timeText), centerX, y, 0xF0F0F0);

        y += 16;
        guiGraphics.drawCenteredString(font, Component.translatable("screen.factory_ld.constructor.select.preview.cost"), centerX, y, 0xD0D0D0);
        y += 12;
        List<ItemStack> costStacks = new ArrayList<>();
        for (MachineItemStackView input : recipe.itemInputs()) {
            if (input.isEmpty()) {
                continue;
            }
            ItemStack stack = input.stack().copy();
            stack.setCount(Math.max(1, input.amountPerCraft()));
            costStacks.add(stack);
        }
        if (!costStacks.isEmpty()) {
            int totalEntries = costStacks.size() + recipe.fluidInputs().size();
            int totalW = totalEntries * 16 + (totalEntries - 1) * 2;
            int costX = centerX - totalW / 2;
            for (ItemStack costStack : costStacks) {
                guiGraphics.renderItem(costStack, costX, y);
                guiGraphics.renderItemDecorations(font, costStack, costX, y);
                hoverTargets.add(HoverTarget.item(costX, y, 16, 16, costStack.copy()));
                costX += 18;
            }
            for (MachineFluidView fluidInput : recipe.fluidInputs()) {
                renderFluidCostEntry(guiGraphics, fluidInput.fluidId(), fluidInput.amountMb(), costX, y);
                costX += 18;
            }
        }
    }

    private void addOverclockControls() {
        int x = leftPos + PROD_RIGHT_PANEL_X + 4;
        int y = topPos + PROD_OVERCLOCK_Y + PROD_OVERCLOCK_H + 2;
        clockBox = new EditBox(font, x, y, 44, 14, Component.literal("Clock"));
        clockBox.setMaxLength(8);
        addRenderableWidget(clockBox);

        clockMinusButton = Button.builder(Component.literal("-"), button -> adjustClock(-1.0))
            .bounds(x + 46, y, 12, 14)
            .build();
        clockPlusButton = Button.builder(Component.literal("+"), button -> adjustClock(1.0))
            .bounds(x + 60, y, 12, 14)
            .build();
        copyButton = Button.builder(Component.translatable("screen.factory_ld.constructor.copy"), button -> copyCurrentSettings())
            .bounds(x, y + 16, 34, 14)
            .build();
        pasteButton = Button.builder(Component.translatable("screen.factory_ld.constructor.paste"), button -> pasteCopiedSettings())
            .bounds(x + 36, y + 16, 34, 14)
            .build();
        addRenderableWidget(clockMinusButton);
        addRenderableWidget(clockPlusButton);
        addRenderableWidget(copyButton);
        addRenderableWidget(pasteButton);
    }

    private void copyCurrentSettings() {
        ResourceLocation selectedRecipeId = menu.getSelectedRecipeId();
        double clock = menu.getBlockEntity().getClockSpeedPercent();
        COPIED_SETTINGS.put(MACHINE_TYPE_KEY, new CopiedSettings(selectedRecipeId, clock));
    }

    private void pasteCopiedSettings() {
        CopiedSettings copied = COPIED_SETTINGS.get(MACHINE_TYPE_KEY);
        if (copied == null) {
            return;
        }
        previewRecipeIndex = -2;
        ClientPlayNetworking.send(new ModNetworking.PasteMachineSettingsPayload(
            menu.getBlockEntity().getBlockPos(),
            copied.recipeId(),
            (int)Math.round(copied.clockPercent() * 10000.0)
        ));
        setTab(Tab.PRODUCTION);
    }

    private boolean shouldOpenProductionInitially() {
        if (menu.getSelectedRecipeId() != null) {
            return true;
        }
        for (int slot = 0; slot < RefineryBlockEntity.SLOT_COUNT; slot++) {
            if (!menu.getBlockEntity().getItems().get(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private Component recipeNameComponent(MachineRecipeView recipe) {
        String key = recipe.nameKey();
        if (!key.isEmpty()) {
            return Component.translatable(key);
        }
        return Component.literal(recipe.name());
    }

    private String getRecipeNameText(MachineRecipeView recipe) {
        String key = recipe.nameKey();
        if (!key.isEmpty()) {
            return Component.translatable(key).getString();
        }
        return recipe.name();
    }

    private Component outputDescriptionComponent(ItemStack output) {
        if (!output.isEmpty()) {
            String key = output.getDescriptionId() + ".description";
            if (I18n.exists(key)) {
                return Component.translatable(key);
            }
        }
        return Component.translatable("screen.factory_ld.constructor.select.preview.no_description");
    }

    private Component fluidDescriptionComponent(ResourceLocation fluidId) {
        if (fluidId == null) {
            return Component.translatable("screen.factory_ld.constructor.select.preview.no_description");
        }
        String key = "fluid." + fluidId.getNamespace() + "." + fluidId.getPath() + ".description";
        if (I18n.exists(key)) {
            return Component.translatable(key);
        }
        return Component.translatable("screen.factory_ld.constructor.select.preview.no_description");
    }

    private int drawCenteredWrapped(
        GuiGraphics guiGraphics,
        Component component,
        int centerX,
        int startY,
        int maxWidth,
        int maxLines,
        int color
    ) {
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(component, Math.max(16, maxWidth));
        int y = startY;
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            net.minecraft.util.FormattedCharSequence line = lines.get(i);
            int lineWidth = font.width(line);
            guiGraphics.drawString(font, line, centerX - lineWidth / 2, y, color, false);
            y += 9;
        }
        return y;
    }

    private int drawCenteredWrappedScaled(
        GuiGraphics guiGraphics,
        Component component,
        int centerX,
        int startY,
        int maxWidth,
        int maxLines,
        int color,
        float scale
    ) {
        float safeScale = Math.max(0.4F, scale);
        int wrapWidth = Math.max(16, (int)(maxWidth / safeScale));
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(component, wrapWidth);
        int y = startY;
        int lineStep = Math.max(6, Math.round(9 * safeScale));
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(safeScale, safeScale, 1.0F);
        for (int i = 0; i < Math.min(maxLines, lines.size()); i++) {
            net.minecraft.util.FormattedCharSequence line = lines.get(i);
            int lineWidth = font.width(line);
            int drawX = Math.round(centerX / safeScale - lineWidth / 2.0F);
            int drawY = Math.round(y / safeScale);
            guiGraphics.drawString(font, line, drawX, drawY, color, false);
            y += lineStep;
        }
        guiGraphics.pose().popPose();
        return y;
    }

    private void drawEllipsized(GuiGraphics guiGraphics, Component component, int x, int y, int maxWidth, int color) {
        String text = component.getString();
        if (font.width(text) <= maxWidth) {
            guiGraphics.drawString(font, component, x, y, color, false);
            return;
        }
        String ellipsis = "...";
        String cut = font.plainSubstrByWidth(text, Math.max(1, maxWidth - font.width(ellipsis)));
        guiGraphics.drawString(font, Component.literal(cut + ellipsis), x, y, color, false);
    }

    private void drawEllipsizedScaled(
        GuiGraphics guiGraphics,
        Component component,
        int x,
        int y,
        int maxWidth,
        int color,
        float scale
    ) {
        float safeScale = Math.max(0.4F, scale);
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(safeScale, safeScale, 1.0F);
        int scaledX = Math.round(x / safeScale);
        int scaledY = Math.round(y / safeScale);
        int scaledWidth = Math.max(1, Math.round(maxWidth / safeScale));
        String text = component.getString();
        if (font.width(text) <= scaledWidth) {
            guiGraphics.drawString(font, component, scaledX, scaledY, color, false);
        } else {
            String ellipsis = "...";
            String cut = font.plainSubstrByWidth(text, Math.max(1, scaledWidth - font.width(ellipsis)));
            guiGraphics.drawString(font, Component.literal(cut + ellipsis), scaledX, scaledY, color, false);
        }
        guiGraphics.pose().popPose();
    }

    private void drawCircle(GuiGraphics guiGraphics, int centerX, int centerY, int radius, int color) {
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int)Math.floor(Math.sqrt(radius * radius - dy * dy));
            guiGraphics.fill(centerX - span, centerY + dy, centerX + span + 1, centerY + dy + 1, color);
        }
    }

    private void drawFluidGlyph(GuiGraphics guiGraphics, ResourceLocation fluidId, int centerX, int centerY, int radius) {
        TextureAtlasSprite sprite = getFluidSprite(fluidId);
        if (sprite == null) {
            drawCircle(guiGraphics, centerX, centerY, radius, fluidColor(fluidId));
            String letter = "F";
            if (fluidId != null && !fluidId.getPath().isBlank()) {
                letter = fluidId.getPath().substring(0, 1).toUpperCase(Locale.ROOT);
            }
            int textWidth = font.width(letter);
            guiGraphics.drawString(font, letter, centerX - textWidth / 2, centerY - 4, 0xFFFFFFFF, false);
            return;
        }
        int diameter = radius * 2 + 1;
        int left = centerX - radius;
        int right = centerX + radius;
        int top = centerY - radius;
        int tint = fluidId != null && fluidId.toString().toLowerCase(Locale.ROOT).contains("water") ? 0xFF3F76E4 : 0xFFFFFFFF;
        float r = ((tint >> 16) & 0xFF) / 255.0F;
        float g = ((tint >> 8) & 0xFF) / 255.0F;
        float b = (tint & 0xFF) / 255.0F;
        guiGraphics.setColor(r, g, b, 1.0F);
        guiGraphics.blit(left, top, 0, diameter, diameter, sprite);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);

        // Mask corners after drawing full sprite to avoid UV resampling darkening.
        for (int dy = -radius; dy <= radius; dy++) {
            int span = (int)Math.floor(Math.sqrt(radius * radius - dy * dy));
            int rowY = centerY + dy;
            int leftMaskEnd = centerX - span;
            int rightMaskStart = centerX + span + 1;
            if (leftMaskEnd > left) {
                guiGraphics.fill(left, rowY, leftMaskEnd, rowY + 1, FLUID_SLOT_INNER);
            }
            if (rightMaskStart <= right) {
                guiGraphics.fill(rightMaskStart, rowY, right + 1, rowY + 1, FLUID_SLOT_INNER);
            }
        }
    }

    private void drawFluidAmountOverlay(GuiGraphics guiGraphics, int slotX, int slotY, String label) {
        guiGraphics.pose().pushPose();
        float scale = 0.5F;
        guiGraphics.pose().scale(scale, scale, 1.0F);
        int x = Math.round((slotX + 16 - font.width(label) * scale) / scale);
        int y = Math.round((slotY + 12) / scale);
        guiGraphics.drawString(font, label, x, y, 0xFFFFFFFF, true);
        guiGraphics.pose().popPose();
    }

    private int fluidColor(ResourceLocation fluidId) {
        if (fluidId == null) {
            return 0xFF6B6B6B;
        }
        String key = fluidId.toString().toLowerCase(Locale.ROOT);
        if (key.contains("water")) {
            return 0xFF2B7CC9;
        }
        if (key.contains("lava")) {
            return 0xFFCF6A2A;
        }
        if (key.contains("oil")) {
            return 0xFF2A2A2A;
        }
        if (key.contains("fuel")) {
            return 0xFFD88C2D;
        }
        return 0xFF5D8AA8;
    }

    private TextureAtlasSprite getFluidSprite(ResourceLocation fluidId) {
        if (minecraft == null || fluidId == null) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getOptional(fluidId).orElse(Fluids.EMPTY);
        if (fluid == Fluids.EMPTY) {
            return null;
        }
        return minecraft.getBlockRenderer()
            .getBlockModelShaper()
            .getParticleIcon(fluid.defaultFluidState().createLegacyBlock());
    }

    private void renderFluidCostEntry(GuiGraphics guiGraphics, ResourceLocation fluidId, long amountMb, int x, int y) {
        guiGraphics.blitSprite(SLOT_SPRITE, x - 1, y - 1, 18, 18);
        drawFluidGlyph(guiGraphics, fluidId, x + 8, y + 8, 7);
        drawFluidAmountOverlay(guiGraphics, x, y, formatFluidAmountHuman(amountMb));
        hoverTargets.add(HoverTarget.text(x, y, 16, 16, Component.literal(formatFluidAmountHuman(amountMb) + " " + formatFluidName(fluidId))));
    }

    private void renderHoverTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (this.hoveredSlot != null && this.hoveredSlot.hasItem()) {
            renderTooltip(guiGraphics, mouseX, mouseY);
            return;
        }
        for (int i = hoverTargets.size() - 1; i >= 0; i--) {
            HoverTarget target = hoverTargets.get(i);
            if (!target.contains(mouseX, mouseY)) {
                continue;
            }
            if (target.stack != null && !target.stack.isEmpty()) {
                guiGraphics.renderTooltip(font, target.stack, mouseX, mouseY);
            } else if (target.text != null) {
                guiGraphics.renderTooltip(font, target.text, mouseX, mouseY);
            }
            return;
        }
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private int compareRecipeEntries(RecipeEntry a, RecipeEntry b) {
        if (a.index == -1 && b.index != -1) {
            return -1;
        }
        if (a.index != -1 && b.index == -1) {
            return 1;
        }
        return a.name.compareToIgnoreCase(b.name);
    }

    private void syncClockField() {
        if (clockBox == null || clockBox.isFocused()) {
            return;
        }
        if (currentTab != Tab.PRODUCTION || !menu.isOverclockUnlocked()) {
            return;
        }
        double current = menu.getBlockEntity().getClockSpeedPercent();
        String desired = formatPercentValue(current);
        if (!clockBox.getValue().equals(desired)) {
            clockBox.setValue(desired);
        }
    }

    private void adjustClock(double deltaPercent) {
        if (!menu.isOverclockUnlocked()) {
            return;
        }
        double current = menu.getBlockEntity().getClockSpeedPercent();
        submitClockValue(current + deltaPercent);
    }

    private void submitClock() {
        if (!menu.isOverclockUnlocked()) {
            return;
        }
        String value = clockBox == null ? "" : clockBox.getValue().trim();
        if (value.isEmpty()) {
            return;
        }
        try {
            double percent = Double.parseDouble(value);
            submitClockValue(percent);
        } catch (NumberFormatException ignored) {
        }
    }

    private void submitClockValue(double percent) {
        if (minecraft == null) {
            return;
        }
        ClientPlayNetworking.send(new ModNetworking.SetClockSpeedPayload(
            menu.getBlockEntity().getBlockPos(),
            (int)Math.round(percent * 10000.0)
        ));
    }

    private String formatPercent(double percent) {
        return formatPercentValue(percent) + "%";
    }

    private String formatPercentValue(double percent) {
        double rounded = Math.round(percent * 10000.0) / 10000.0;
        if (rounded % 1.0 == 0) {
            return Integer.toString((int)rounded);
        }
        if (Math.abs(rounded * 10 - Math.round(rounded * 10)) < 0.0001) {
            return String.format(Locale.ROOT, "%.1f", rounded);
        }
        return String.format(Locale.ROOT, "%.4f", rounded);
    }

    private String formatPower(double powerMw) {
        return String.format(Locale.ROOT, "%.2f MW", powerMw);
    }

    private record RecipeEntry(
        int index,
        String name,
        String category,
        ItemStack outputIcon,
        ItemStack byproductIcon,
        ResourceLocation primaryFluidId,
        ResourceLocation byproductFluidId,
        String searchKey
    ) {
    }

    private record Layout(Map<String, List<RecipeEntry>> byCategory, List<String> categories, int contentHeight) {
    }

    private record CopiedSettings(ResourceLocation recipeId, double clockPercent) {
    }

    private enum Tab {
        SELECT_RECIPE,
        PRODUCTION
    }

    private enum ProductionStatus {
        IDLE("idle"),
        WORKING("working"),
        NO_POWER("no_power");

        private final String key;

        ProductionStatus(String key) {
            this.key = key;
        }
    }

    private static final class HoverTarget {
        private final int x;
        private final int y;
        private final int w;
        private final int h;
        private final ItemStack stack;
        private final Component text;

        private HoverTarget(int x, int y, int w, int h, ItemStack stack, Component text) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.stack = stack;
            this.text = text;
        }

        private static HoverTarget item(int x, int y, int w, int h, ItemStack stack) {
            return new HoverTarget(x, y, w, h, stack, null);
        }

        private static HoverTarget text(int x, int y, int w, int h, Component text) {
            return new HoverTarget(x, y, w, h, ItemStack.EMPTY, text);
        }

        private boolean contains(int mx, int my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
}
