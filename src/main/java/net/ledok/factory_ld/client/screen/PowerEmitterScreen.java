package net.ledok.factory_ld.client.screen;

import net.ledok.factory_ld.world.screen.PowerEmitterScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PowerEmitterScreen extends AbstractContainerScreen<PowerEmitterScreenHandler> {
    private static final int GRAPH_X = 12;
    private static final int GRAPH_Y = 18;
    private static final int GRAPH_W = 216;
    private static final int GRAPH_H = 90;
    private static final int HISTORY_LIMIT = GRAPH_W;

    private final List<Double> capacityHistory = new ArrayList<>();
    private final List<Double> productionHistory = new ArrayList<>();
    private final List<Double> consumptionHistory = new ArrayList<>();
    private final List<Double> maxConsumptionHistory = new ArrayList<>();

    public PowerEmitterScreen(PowerEmitterScreenHandler handler, Inventory inventory, Component title) {
        super(handler, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 212;
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelY = -1000;
        addRenderableWidget(Button.builder(Component.translatable("screen.factory_ld.power.reset"), button -> {
            if (minecraft != null && minecraft.gameMode != null) {
                minecraft.gameMode.handleInventoryButtonClick(menu.containerId, 0);
            }
        }).bounds(leftPos + imageWidth - 70, topPos + imageHeight - 22, 62, 14).build());
    }

    @Override
    public void containerTick() {
        super.containerTick();
        sample(capacityHistory, menu.capacityMw());
        sample(productionHistory, menu.productionMw());
        sample(consumptionHistory, menu.consumptionMw());
        sample(maxConsumptionHistory, menu.maxConsumptionMw());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float delta, int mouseX, int mouseY) {
        guiGraphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFF2E2E2E);
        guiGraphics.fill(leftPos + 4, topPos + 4, leftPos + imageWidth - 4, topPos + imageHeight - 4, 0xFF3A3A3A);
        guiGraphics.fill(leftPos + GRAPH_X, topPos + GRAPH_Y, leftPos + GRAPH_X + GRAPH_W, topPos + GRAPH_Y + GRAPH_H, 0xFF1C1C1C);
        guiGraphics.hLine(leftPos + GRAPH_X, leftPos + GRAPH_X + GRAPH_W - 1, topPos + GRAPH_Y + GRAPH_H - 1, 0xFF555555);

        double maxValue = Math.max(1.0, maxOf(menu.capacityMw(), menu.maxConsumptionMw(), maxHistoryValue()));
        drawHistory(guiGraphics, maxConsumptionHistory, 0xFF6FAFD1, maxValue);
        drawHistory(guiGraphics, capacityHistory, 0xFFC7C7C7, maxValue);
        drawHistory(guiGraphics, productionHistory, 0xFF8F8F8F, maxValue);
        drawHistory(guiGraphics, consumptionHistory, 0xFFE29A3A, maxValue);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(font, title, titleLabelX, titleLabelY, 0xF0F0F0, false);

        int y = GRAPH_Y + GRAPH_H + 8;
        guiGraphics.drawString(font, Component.translatable("screen.factory_ld.power.capacity", formatMw(menu.capacityMw())), 12, y, 0xFFC7C7C7, false);
        y += 10;
        guiGraphics.drawString(font, Component.translatable("screen.factory_ld.power.production", formatMw(menu.productionMw())), 12, y, 0xFF8F8F8F, false);
        y += 10;
        guiGraphics.drawString(font, Component.translatable("screen.factory_ld.power.consumption", formatMw(menu.consumptionMw())), 12, y, 0xFFE29A3A, false);
        y += 10;
        guiGraphics.drawString(font, Component.translatable("screen.factory_ld.power.max_consumption", formatMw(menu.maxConsumptionMw())), 12, y, 0xFF6FAFD1, false);

        String statusKey = menu.tripped() ? "screen.factory_ld.power.status.tripped" : "screen.factory_ld.power.status.stable";
        guiGraphics.drawString(font, Component.translatable(statusKey), 12, y + 14, menu.tripped() ? 0xFFD16565 : 0xFF86D46C, false);
        guiGraphics.drawString(
            font,
            Component.translatable("screen.factory_ld.power.nodes", menu.generatorCount(), menu.consumerCount()),
            130,
            y + 14,
            0xFFD8D8D8,
            false
        );
        guiGraphics.drawString(
            font,
            Component.translatable(
                "screen.factory_ld.power.storage",
                formatMwh(menu.storageStoredMwh()),
                formatMwh(menu.storageCapacityMwh()),
                formatPercent(menu.storageStoredMwh(), menu.storageCapacityMwh())
            ),
            12,
            y + 28,
            0xFF9ED2E7,
            false
        );
        guiGraphics.drawString(
            font,
            Component.translatable("screen.factory_ld.power.storage_io", formatMw(menu.storageChargeMw()), formatMw(menu.storageDischargeMw())),
            12,
            y + 38,
            0xFF9ED2E7,
            false
        );
    }

    private void drawHistory(GuiGraphics guiGraphics, List<Double> history, int color, double maxValue) {
        if (history.size() < 2) {
            return;
        }
        int start = Math.max(0, history.size() - GRAPH_W);
        for (int i = start + 1; i < history.size(); i++) {
            int x1 = leftPos + GRAPH_X + (i - 1 - start);
            int x2 = leftPos + GRAPH_X + (i - start);
            int y1 = toGraphY(history.get(i - 1), maxValue);
            int y2 = toGraphY(history.get(i), maxValue);
            guiGraphics.renderOutline(Math.min(x1, x2), Math.min(y1, y2), Math.abs(x2 - x1) + 1, Math.abs(y2 - y1) + 1, color);
        }
    }

    private int toGraphY(double value, double maxValue) {
        double ratio = Math.max(0.0, Math.min(1.0, value / maxValue));
        return topPos + GRAPH_Y + GRAPH_H - 1 - (int) Math.round((GRAPH_H - 1) * ratio);
    }

    private static void sample(List<Double> history, double value) {
        history.add(Math.max(0.0, value));
        if (history.size() > HISTORY_LIMIT) {
            history.remove(0);
        }
    }

    private double maxHistoryValue() {
        double max = 0.0;
        for (double value : capacityHistory) {
            if (value > max) {
                max = value;
            }
        }
        for (double value : productionHistory) {
            if (value > max) {
                max = value;
            }
        }
        for (double value : consumptionHistory) {
            if (value > max) {
                max = value;
            }
        }
        for (double value : maxConsumptionHistory) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private static String formatMw(double value) {
        return String.format(Locale.ROOT, "%.1f MW", Math.max(0.0, value));
    }

    private static String formatMwh(double value) {
        return String.format(Locale.ROOT, "%.2f MWh", Math.max(0.0, value));
    }

    private static String formatPercent(double value, double max) {
        if (max <= 1.0E-9) {
            return "0.0%";
        }
        double percent = Math.max(0.0, Math.min(100.0, (value / max) * 100.0));
        return String.format(Locale.ROOT, "%.1f%%", percent);
    }

    private static double maxOf(double a, double b, double c) {
        return Math.max(a, Math.max(b, c));
    }
}
