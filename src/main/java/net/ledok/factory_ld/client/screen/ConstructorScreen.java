package net.ledok.factory_ld.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import net.ledok.factory_ld.world.recipe.ConstructorRecipe;
import net.ledok.factory_ld.world.screen.ConstructorScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import com.mojang.blaze3d.systems.RenderSystem;

public class ConstructorScreen extends AbstractContainerScreen<ConstructorScreenHandler> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("factory_ld", "textures/gui/constructor.png");
    private static final ResourceLocation SLOT_SPRITE = ResourceLocation.withDefaultNamespace("container/slot");
    private Button confirmButton;
    private Button emptyButton;
    private EditBox searchBox;
    private Tab currentTab = Tab.SELECT_RECIPE;
    private int lastRecipeCount = -1;
    private int previewRecipeIndex = -2;
    private final List<RecipeEntry> recipeEntries = new ArrayList<>();
    private int scrollOffset = 0;
    private final Set<String> collapsedCategories = new HashSet<>();

    public ConstructorScreen(ConstructorScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 180;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 8;
        this.inventoryLabelY = 90;
        clearWidgets();
        previewRecipeIndex = -2;
        recipeEntries.clear();
        scrollOffset = 0;
        addTabButtons();
        addConfirmButton();
        addEmptyButton();
        addSearchBox();
        List<RecipeHolder<ConstructorRecipe>> recipes = menu.getAvailableRecipes();
        lastRecipeCount = recipes.size();
        rebuildRecipeEntries(recipes);
        updateTabVisibility();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        int currentCount = menu.getAvailableRecipes().size();
        if (currentCount != lastRecipeCount) {
            init();
        }
    }

    private void addTabButtons() {
        Button selectTab = Button.builder(Component.literal("Select Recipe"), widget -> setTab(Tab.SELECT_RECIPE))
            .bounds(leftPos + 8, topPos - 22, 100, 18)
            .build();
        Button productionTab = Button.builder(Component.literal("Production"), widget -> setTab(Tab.PRODUCTION))
            .bounds(leftPos + 112, topPos - 22, 90, 18)
            .build();
        addRenderableWidget(selectTab);
        addRenderableWidget(productionTab);
    }

    private void addConfirmButton() {
        confirmButton = Button.builder(Component.literal("Select Recipe"), widget -> onConfirmSelection())
            .bounds(leftPos + 150, topPos + 8, 80, 18)
            .build();
        addRenderableWidget(confirmButton);
    }

    private void addEmptyButton() {
        emptyButton = Button.builder(Component.literal("Empty"), widget -> onEmptyPreview())
            .bounds(getPanelX() + 4, getPanelY() + 26, getPanelWidth() - 8, 18)
            .build();
        addRenderableWidget(emptyButton);
    }

    private void addSearchBox() {
        searchBox = new EditBox(font, getPanelX() + 4, getPanelY() + 6, getPanelWidth() - 8, 16, Component.literal("Search"));
        searchBox.setMaxLength(64);
        searchBox.setBordered(true);
        addRenderableWidget(searchBox);
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
        if (confirmButton != null) {
            confirmButton.visible = showRecipes;
            confirmButton.active = showRecipes;
        }
        if (emptyButton != null) {
            emptyButton.visible = showRecipes;
            emptyButton.active = showRecipes;
        }
        if (searchBox != null) {
            searchBox.setVisible(showRecipes);
            searchBox.setFocused(showRecipes && searchBox.isFocused());
        }
    }

    private void onRecipePreview(int index) {
        previewRecipeIndex = index;
    }

    private void onEmptyPreview() {
        previewRecipeIndex = -1;
    }

    private void onConfirmSelection() {
        if (minecraft == null || minecraft.gameMode == null) {
            return;
        }
        int buttonId = previewRecipeIndex < 0 ? 0 : previewRecipeIndex + 1;
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
        setTab(Tab.PRODUCTION);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        super.render(guiGraphics, mouseX, mouseY, delta);
        if (currentTab == Tab.PRODUCTION) {
            renderProductionInfo(guiGraphics);
        } else {
            renderSelectedRecipe(guiGraphics);
        }
    }

    private void renderSelectedRecipe(GuiGraphics guiGraphics) {
        RecipeHolder<ConstructorRecipe> recipe = getDisplayedRecipe();
        ResourceLocation selectedId = recipe == null ? null : recipe.id();
        if (selectedId == null) {
            guiGraphics.drawString(font, Component.translatable("screen.factory_ld.constructor.no_recipe"), leftPos + 8, topPos + 22, 0x404040, false);
            return;
        }
        String label = recipe.value().getRecipeName();
        guiGraphics.drawString(font, Component.literal(label), leftPos + 8, topPos + 22, 0x404040, false);
        renderCountsAndTime(guiGraphics, recipe.value());
    }

    private void renderProductionInfo(GuiGraphics guiGraphics) {
        ResourceLocation selectedId = menu.getSelectedRecipeId();
        if (selectedId == null) {
            guiGraphics.drawString(font, Component.translatable("screen.factory_ld.constructor.no_recipe"), leftPos + 8, topPos + 22, 0x404040, false);
            return;
        }
        menu.getSelectedRecipe().ifPresent(recipe -> {
            guiGraphics.drawString(font, Component.literal(recipe.getRecipeName()), leftPos + 8, topPos + 22, 0x404040, false);
            renderCountsAndTime(guiGraphics, recipe);
        });
        guiGraphics.drawString(font, Component.literal("Input"), leftPos + 26, topPos + 58, 0x404040, false);
        guiGraphics.drawString(font, Component.literal("Output"), leftPos + 84, topPos + 58, 0x404040, false);
    }

    private void renderCountsAndTime(GuiGraphics guiGraphics, ConstructorRecipe recipe) {
        int inputCount = Math.max(1, recipe.getInputCount());
        int outputCount = recipe.getOutput().getCount();
        int craftTime = Math.max(1, recipe.getCraftTime());
        float craftsPerMinute = 1200.0f / craftTime;
        float inputPerMinute = craftsPerMinute * inputCount;
        float outputPerMinute = craftsPerMinute * outputCount;
        String inputText = "x" + inputCount + " (" + formatPerMinute(inputPerMinute) + "/m)";
        String outputText = "x" + outputCount + " (" + formatPerMinute(outputPerMinute) + "/m)";
        guiGraphics.drawString(font, Component.literal(inputText), leftPos + 30, topPos + 74, 0x404040, false);
        guiGraphics.drawString(font, Component.literal(outputText), leftPos + 90, topPos + 74, 0x404040, false);

        String timeText = (craftTime / 20.0f) + "s";
        guiGraphics.drawString(font, Component.literal(timeText), leftPos + 60, topPos + 42, 0x404040, false);
    }

    private String formatPerMinute(float value) {
        if (value >= 100) {
            return Integer.toString(Math.round(value));
        }
        if (value >= 10) {
            return String.format(java.util.Locale.ROOT, "%.1f", value);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float delta, int mouseX, int mouseY) {
        if (currentTab == Tab.SELECT_RECIPE) {
            renderRecipePanel(guiGraphics);
        }
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        for (Slot slot : menu.slots) {
            guiGraphics.blitSprite(SLOT_SPRITE, leftPos + slot.x - 1, topPos + slot.y - 1, 18, 18);
        }
        renderGhostItems(guiGraphics);
    }

    private void renderGhostItems(GuiGraphics guiGraphics) {
        RecipeHolder<ConstructorRecipe> displayed = getDisplayedRecipe();
        if (displayed == null) {
            return;
        }
        ConstructorRecipe recipe = displayed.value();
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
    }

    private RecipeHolder<ConstructorRecipe> getDisplayedRecipe() {
        if (currentTab == Tab.SELECT_RECIPE) {
            if (previewRecipeIndex == -1) {
                return null;
            }
            if (previewRecipeIndex >= 0) {
                List<RecipeHolder<ConstructorRecipe>> recipes = menu.getAvailableRecipes();
                if (previewRecipeIndex < recipes.size()) {
                    return recipes.get(previewRecipeIndex);
                }
            }
            return null;
        }
        return menu.getSelectedRecipe().map(recipe -> {
            List<RecipeHolder<ConstructorRecipe>> recipes = menu.getAvailableRecipes();
            for (RecipeHolder<ConstructorRecipe> holder : recipes) {
                if (holder.value() == recipe) {
                    return holder;
                }
            }
            return null;
        }).orElse(null);
    }

    private void renderGhost(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        guiGraphics.renderItem(stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
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
        if (currentTab == Tab.SELECT_RECIPE && searchBox != null && searchBox.isFocused()) {
            if (searchBox.keyPressed(keyCode, scanCode, modifiers)) {
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
        return super.charTyped(codePoint, modifiers);
    }

    private void renderRecipePanel(GuiGraphics guiGraphics) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelWidth();
        int panelH = imageHeight;

        guiGraphics.fill(panelX, panelY, panelX + panelW, panelY + panelH, 0xAA2A2A2A);
        guiGraphics.drawString(font, Component.literal("Recipes"), panelX + 6, panelY + 48, 0xFFFFFF, false);

        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        Layout layout = computeLayout(query);

        int contentTop = panelY + 60;
        int contentBottom = panelY + panelH - 6;
        int contentHeight = contentBottom - contentTop;
        int maxScroll = Math.max(0, layout.contentHeight - contentHeight);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        guiGraphics.enableScissor(panelX, contentTop, panelX + panelW, contentBottom);
        int y = contentTop - scrollOffset;
        int xStart = panelX + 6;
        int cellW = 40;
        int cellH = 40;
        int cols = 3;

        for (String category : layout.categories) {
            boolean collapsed = collapsedCategories.contains(category);
            String arrow = collapsed ? ">" : "v";
            guiGraphics.drawString(font, Component.literal(arrow), xStart, y, 0xFFFFFF, false);
            guiGraphics.drawString(font, Component.literal(category), xStart + 10, y, 0xFFFFFF, false);
            y += 12;
            if (collapsed) {
                y += 6;
                continue;
            }
            List<RecipeEntry> entries = new ArrayList<>(layout.byCategory.get(category));
            entries.sort(Comparator.comparing(entry -> entry.name));

            for (int i = 0; i < entries.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int cellX = xStart + col * cellW;
                int cellY = y + row * cellH;
                renderRecipeEntry(guiGraphics, entries.get(i), cellX, cellY, cellW);
            }
            int rows = (entries.size() + cols - 1) / cols;
            y += rows * cellH + 8;
        }
        guiGraphics.disableScissor();

        if (maxScroll > 0) {
            int barX = panelX + panelW - 6;
            int barY = contentTop;
            int barH = contentHeight;
            guiGraphics.fill(barX, barY, barX + 4, barY + barH, 0xFF2F2F2F);
            int thumbH = Math.max(10, (int)((float)barH * barH / layout.contentHeight));
            int thumbY = barY + (int)((float)scrollOffset * (barH - thumbH) / maxScroll);
            guiGraphics.fill(barX, thumbY, barX + 4, thumbY + thumbH, 0xFF6B6B6B);
        }
    }

    private void renderRecipeEntry(GuiGraphics guiGraphics, RecipeEntry entry, int x, int y, int cellW) {
        int bgColor = entry.index == previewRecipeIndex ? 0xFF555555 : 0xFF3A3A3A;
        guiGraphics.fill(x, y, x + cellW - 2, y + 36, bgColor);
        guiGraphics.renderItem(entry.outputIcon, x + 12, y);
        List<net.minecraft.util.FormattedCharSequence> lines = font.split(Component.literal(entry.name), cellW);
        int lineY = y + 18;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            int lineWidth = font.width(lines.get(i));
            int textX = x + Math.max(0, (cellW - lineWidth) / 2);
            guiGraphics.drawString(font, lines.get(i), textX, lineY, 0xFFFFFF, false);
            lineY += 9;
        }
    }

    private boolean handleRecipeClick(double mouseX, double mouseY) {
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelWidth();
        int contentTop = panelY + 60;
        int contentBottom = panelY + imageHeight - 6;
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < contentTop || mouseY > contentBottom) {
            return false;
        }
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        Layout layout = computeLayout(query);

        int y = contentTop - scrollOffset;
        int xStart = panelX + 6;
        int cellW = 40;
        int cellH = 40;
        int cols = 3;

        for (String category : layout.categories) {
            boolean collapsed = collapsedCategories.contains(category);
            if (mouseX >= xStart && mouseX <= xStart + 8 && mouseY >= y && mouseY <= y + 10) {
                if (collapsed) {
                    collapsedCategories.remove(category);
                } else {
                    collapsedCategories.add(category);
                }
                return true;
            }
            y += 12;
            if (collapsed) {
                y += 6;
                continue;
            }
            List<RecipeEntry> entries = new ArrayList<>(layout.byCategory.get(category));
            entries.sort(Comparator.comparing(entry -> entry.name));
            for (int i = 0; i < entries.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int cellX = xStart + col * cellW;
                int cellY = y + row * cellH;
                if (mouseX >= cellX && mouseX <= cellX + cellW && mouseY >= cellY && mouseY <= cellY + cellH) {
                    previewRecipeIndex = entries.get(i).index;
                    return true;
                }
            }
            int rows = (entries.size() + cols - 1) / cols;
            y += rows * cellH + 8;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (currentTab != Tab.SELECT_RECIPE) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelW = getPanelWidth();
        int contentTop = panelY + 60;
        int contentBottom = panelY + imageHeight - 6;
        if (mouseX < panelX || mouseX > panelX + panelW || mouseY < contentTop || mouseY > contentBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }
        String query = searchBox == null ? "" : searchBox.getValue().toLowerCase(Locale.ROOT).trim();
        Layout layout = computeLayout(query);
        int contentHeight = contentBottom - contentTop;
        int maxScroll = Math.max(0, layout.contentHeight - contentHeight);
        if (maxScroll == 0) {
            return true;
        }
        int scrollStep = 16;
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset - (int)(verticalAmount * scrollStep)));
        return true;
    }

    private void rebuildRecipeEntries(List<RecipeHolder<ConstructorRecipe>> recipes) {
        recipeEntries.clear();
        for (int i = 0; i < recipes.size(); i++) {
            RecipeHolder<ConstructorRecipe> holder = recipes.get(i);
            ConstructorRecipe recipe = holder.value();
            ItemStack output = recipe.getOutput();
            String name = recipe.getRecipeName();
            String category = recipe.getCategory();
            String outputName = output.getHoverName().getString();
            String searchKey = (name + " " + outputName).toLowerCase(Locale.ROOT);
            recipeEntries.add(new RecipeEntry(i, name, category, output, searchKey));
        }
    }

    private Layout computeLayout(String query) {
        List<RecipeEntry> filtered = recipeEntries.stream()
            .filter(entry -> query.isEmpty() || entry.searchKey.contains(query))
            .toList();

        Map<String, List<RecipeEntry>> byCategory = filtered.stream()
            .collect(Collectors.groupingBy(entry -> entry.category));

        List<String> categories = byCategory.keySet().stream()
            .sorted()
            .toList();

        int cellH = 40;
        int cols = 3;
        int contentHeight = 0;
        for (String category : categories) {
            contentHeight += 12;
            if (collapsedCategories.contains(category)) {
                contentHeight += 6;
                continue;
            }
            int size = byCategory.get(category).size();
            int rows = (size + cols - 1) / cols;
            contentHeight += rows * cellH + 8;
        }
        return new Layout(byCategory, categories, contentHeight);
    }

    private int getPanelWidth() {
        return 120;
    }

    private int getPanelX() {
        return Math.max(0, leftPos - getPanelWidth() - 6);
    }

    private int getPanelY() {
        return topPos;
    }

    private record RecipeEntry(int index, String name, String category, ItemStack outputIcon, String searchKey) {
    }

    private record Layout(Map<String, List<RecipeEntry>> byCategory, List<String> categories, int contentHeight) {
    }

    private enum Tab {
        SELECT_RECIPE,
        PRODUCTION
    }
}
