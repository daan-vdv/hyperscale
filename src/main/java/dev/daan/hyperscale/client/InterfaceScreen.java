package dev.daan.hyperscale.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import dev.daan.hyperscale.hall.DeploymentStatus;
import dev.daan.hyperscale.hall.HallCapacity;
import dev.daan.hyperscale.hall.HallView;
import dev.daan.hyperscale.hall.ProcessImage;
import dev.daan.hyperscale.hall.StackCount;
import dev.daan.hyperscale.menu.InterfaceMenu;
import dev.daan.hyperscale.network.InterfaceActionPayload;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

public class InterfaceScreen extends AbstractContainerScreen<InterfaceMenu> {
    private static final int GUI_W = 336;
    private static final int GUI_H = 220;

    private static final int REG_X = 8;
    private static final int REG_W = 142;
    private static final int REG_Y = 56;
    private static final int REG_ROW_H = 28;
    private static final int REG_VISIBLE = 5;

    private static final int CON_X = 156;
    private static final int CON_W = 172;
    private static final int CON_Y = 56;
    private static final int CON_ROW_H = 35;
    private static final int CON_VISIBLE = 4;

    private static final int STO_COLS = 6;
    private static final int STO_ROWS = 4;
    private static final int STO_CELL_W = 28;
    private static final int STO_CELL_H = 35;
    private static final int STO_PER_PAGE = STO_COLS * STO_ROWS;

    private final List<Button> dynamicButtons = new ArrayList<>();
    private HallView cachedView = HallView.EMPTY;
    private boolean storageTab;
    private int regPage;
    private int conPage;
    private int stoPage;
    private int lastRegCount = -1;
    private int lastConCount = -1;
    private int lastStoCount = -1;
    private String lastConSignature = "";

    public InterfaceScreen(InterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = GUI_W;
        this.imageHeight = GUI_H;
    }

    @Override
    protected void init() {
        super.init();
        this.refreshView();
        this.rebuildButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        this.refreshView();
        String sig = this.containerSignature();
        if (this.cachedView.registry().size() != this.lastRegCount
                || this.cachedView.containers().size() != this.lastConCount
                || this.cachedView.storedItems().size() != this.lastStoCount
                || !sig.equals(this.lastConSignature)) {
            this.rebuildButtons();
        }
    }

    private void refreshView() {
        if (this.minecraft == null || this.minecraft.level == null) {
            this.cachedView = HallView.EMPTY;
            return;
        }
        this.cachedView = HallView.inspect(this.minecraft.level, this.menu.pos());
    }

    private String containerSignature() {
        StringBuilder sb = new StringBuilder();
        for (HallView.ContainerRow row : this.cachedView.containers()) {
            sb.append(row.key()).append(':').append(row.paused() ? '1' : '0').append(':').append(row.profile().id()).append(';');
        }
        return sb.toString();
    }

    private void clearDynamicButtons() {
        for (Button button : this.dynamicButtons) {
            this.removeWidget(button);
        }
        this.dynamicButtons.clear();
    }

    private void addDynamicButton(Button button) {
        this.dynamicButtons.add(button);
        this.addRenderableWidget(button);
    }

    private void rebuildButtons() {
        this.clearDynamicButtons();

        List<HallView.RegistryRow> regRows = this.cachedView.registry();
        List<HallView.ContainerRow> conRows = this.cachedView.containers();
        List<StackCount> stoItems = this.cachedView.storedItems();
        this.lastRegCount = regRows.size();
        this.lastConCount = conRows.size();
        this.lastStoCount = stoItems.size();
        this.lastConSignature = this.containerSignature();

        int maxRegPage = Math.max(0, (regRows.size() - 1) / REG_VISIBLE);
        int maxConPage = Math.max(0, (conRows.size() - 1) / CON_VISIBLE);
        int maxStoPage = Math.max(0, (stoItems.size() - 1) / STO_PER_PAGE);
        this.regPage = Math.max(0, Math.min(this.regPage, maxRegPage));
        this.conPage = Math.max(0, Math.min(this.conPage, maxConPage));
        this.stoPage = Math.max(0, Math.min(this.stoPage, maxStoPage));

        // Right-pane tab buttons: Containers | Storage
        Button conTabBtn = Button.builder(Component.translatable("gui.hyperscale.orchestrator.tab_containers"), b -> {
            if (this.storageTab) {
                this.storageTab = false;
                this.rebuildButtons();
            }
        }).bounds(this.leftPos + CON_X, this.topPos + 43, 68, 12).build();
        conTabBtn.active = this.storageTab;
        this.addDynamicButton(conTabBtn);

        Button stoTabBtn = Button.builder(Component.translatable("gui.hyperscale.orchestrator.tab_storage"), b -> {
            if (!this.storageTab) {
                this.storageTab = true;
                this.rebuildButtons();
            }
        }).bounds(this.leftPos + CON_X + 70, this.topPos + 43, 56, 12).build();
        stoTabBtn.active = !this.storageTab;
        this.addDynamicButton(stoTabBtn);

        // Registry pagination buttons
        if (maxRegPage > 0) {
            Button up = Button.builder(Component.literal("^"), b -> {
                if (this.regPage > 0) {
                    this.regPage--;
                    this.rebuildButtons();
                }
            }).bounds(this.leftPos + REG_X + REG_W - 28, this.topPos + 43, 13, 12).build();
            up.active = this.regPage > 0;
            this.addDynamicButton(up);

            Button down = Button.builder(Component.literal("v"), b -> {
                if (this.regPage < maxRegPage) {
                    this.regPage++;
                    this.rebuildButtons();
                }
            }).bounds(this.leftPos + REG_X + REG_W - 13, this.topPos + 43, 13, 12).build();
            down.active = this.regPage < maxRegPage;
            this.addDynamicButton(down);
        }

        // Right-pane pagination buttons
        int maxRightPage = this.storageTab ? maxStoPage : maxConPage;
        int rightPage = this.storageTab ? this.stoPage : this.conPage;
        if (maxRightPage > 0) {
            Button up = Button.builder(Component.literal("^"), b -> {
                if (this.storageTab && this.stoPage > 0) {
                    this.stoPage--;
                    this.rebuildButtons();
                } else if (!this.storageTab && this.conPage > 0) {
                    this.conPage--;
                    this.rebuildButtons();
                }
            }).bounds(this.leftPos + CON_X + CON_W - 28, this.topPos + 43, 13, 12).build();
            up.active = rightPage > 0;
            this.addDynamicButton(up);

            Button down = Button.builder(Component.literal("v"), b -> {
                if (this.storageTab && this.stoPage < maxStoPage) {
                    this.stoPage++;
                    this.rebuildButtons();
                } else if (!this.storageTab && this.conPage < maxConPage) {
                    this.conPage++;
                    this.rebuildButtons();
                }
            }).bounds(this.leftPos + CON_X + CON_W - 13, this.topPos + 43, 13, 12).build();
            down.active = rightPage < maxRightPage;
            this.addDynamicButton(down);
        }

        // Registry Deploy (+) buttons
        int regStart = this.regPage * REG_VISIBLE;
        int regEnd = Math.min(regRows.size(), regStart + REG_VISIBLE);
        for (int i = regStart; i < regEnd; i++) {
            HallView.RegistryRow row = regRows.get(i);
            int y = this.topPos + REG_Y + (i - regStart) * (REG_ROW_H + 2);
            String key = row.key();
            Button deployBtn = Button.builder(Component.literal("+"), b -> this.sendAction(InterfaceActionPayload.ACTION_DEPLOY, key, scaleStep()))
                    .bounds(this.leftPos + REG_X + REG_W - 18, y + 6, 15, 16)
                    .build();
            this.addDynamicButton(deployBtn);
        }

        // Container controls (profile, -, +, pause/play, x) when on Containers tab
        if (!this.storageTab) {
            int conStart = this.conPage * CON_VISIBLE;
            int conEnd = Math.min(conRows.size(), conStart + CON_VISIBLE);
            for (int i = conStart; i < conEnd; i++) {
                HallView.ContainerRow row = conRows.get(i);
                int y = this.topPos + CON_Y + (i - conStart) * (CON_ROW_H + 2);
                int btnY = y + 3;
                int rightX = this.leftPos + CON_X + CON_W - 3;
                String key = row.key();

                this.addDynamicButton(Button.builder(Component.literal("x"), b -> this.sendAction(InterfaceActionPayload.ACTION_REMOVE, key, 1))
                        .bounds(rightX - 12, btnY, 12, 12).build());
                this.addDynamicButton(Button.builder(Component.literal(row.paused() ? ">" : "||"), b -> this.sendAction(InterfaceActionPayload.ACTION_TOGGLE_PAUSE, key, 1))
                        .bounds(rightX - 25, btnY, 12, 12).build());
                this.addDynamicButton(Button.builder(Component.literal("+"), b -> this.sendAction(InterfaceActionPayload.ACTION_SCALE_UP, key, scaleStep()))
                        .bounds(rightX - 38, btnY, 12, 12).build());
                this.addDynamicButton(Button.builder(Component.literal("-"), b -> this.sendAction(InterfaceActionPayload.ACTION_SCALE_DOWN, key, scaleStep()))
                        .bounds(rightX - 51, btnY, 12, 12).build());
                this.addDynamicButton(Button.builder(Component.literal(row.profile().shortLabel()), b -> this.sendAction(InterfaceActionPayload.ACTION_CYCLE_PROFILE, key, 1))
                        .bounds(rightX - 26, btnY + 13, 26, 11).build());
            }
        }
    }

    private static int scaleStep() {
        if (Screen.hasControlDown()) {
            return 10;
        }
        if (Screen.hasShiftDown()) {
            return 5;
        }
        return 1;
    }

    private void sendAction(int actionId, String key, int amount) {
        PacketDistributor.sendToServer(new InterfaceActionPayload(this.menu.pos(), actionId, key, amount));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.storageTab && (button == 0 || button == 1)) {
            StackCount clicked = this.hoveredStorageEntry((int) mouseX, (int) mouseY);
            if (clicked != null) {
                int amount = button == 1 ? 1 : 64;
                this.sendAction(InterfaceActionPayload.ACTION_EXTRACT_STORAGE, clicked.item().toString(), amount);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private StackCount hoveredStorageEntry(int mouseX, int mouseY) {
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;
        if (relX < CON_X || relX >= CON_X + STO_COLS * STO_CELL_W || relY < CON_Y || relY >= CON_Y + STO_ROWS * STO_CELL_H) {
            return null;
        }
        int col = (relX - CON_X) / STO_CELL_W;
        int row = (relY - CON_Y) / STO_CELL_H;
        int idx = this.stoPage * STO_PER_PAGE + row * STO_COLS + col;
        List<StackCount> items = this.cachedView.storedItems();
        return (idx >= 0 && idx < items.size()) ? items.get(idx) : null;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // Outer frame & main slate background
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF0A1117);
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + this.imageHeight - 2, 0xFF121E2A);
        // Header band
        graphics.fill(x + 2, y + 2, x + this.imageWidth - 2, y + 41, 0xFF0E1822);
        graphics.fill(x + 2, y + 41, x + this.imageWidth - 2, y + 42, 0xFF234157);

        // 5 Telemetry bars: RAM | CPU | GPU | DSK | PWR
        HallView view = this.cachedView;
        HallCapacity cap = view.capacity();
        this.renderBar(graphics, x + 6, y + 20, 62, view.usedRamGb(), Math.max(1, cap.totalRamGb()), 0xFF38BDF8);
        this.renderBar(graphics, x + 71, y + 20, 58, view.usedVcpu(), Math.max(1, cap.totalVcpu()), 0xFF60A5FA);
        this.renderBar(graphics, x + 132, y + 20, 56, cap.totalTflops() > 0 ? 1 : 0, 1, cap.totalTflops() > 0 ? 0xFFC084FC : 0xFF334155);
        this.renderBar(graphics, x + 191, y + 20, 68, view.usedStorageItems(), Math.max(1, view.totalStorageCapacity()), 0xFF2DD4BF);
        this.renderBar(graphics, x + 262, y + 20, 68, view.energyStored(), Math.max(1, view.maxEnergyStored()), view.totalFePerTick() > 0 ? 0xFF34D399 : 0xFFFBBF24);

        // Left & right pane backgrounds
        graphics.fill(x + REG_X - 2, y + 43, x + REG_X + REG_W + 2, y + 204, 0xFF0D1720);
        graphics.fill(x + CON_X - 2, y + 43, x + CON_X + CON_W + 2, y + 204, 0xFF0D1720);

        // Registry cards
        List<HallView.RegistryRow> regRows = view.registry();
        int regStart = this.regPage * REG_VISIBLE;
        int regEnd = Math.min(regRows.size(), regStart + REG_VISIBLE);
        for (int i = regStart; i < regEnd; i++) {
            int ry = y + REG_Y + (i - regStart) * (REG_ROW_H + 2);
            boolean hovered = mouseX >= x + REG_X && mouseX < x + REG_X + REG_W && mouseY >= ry && mouseY < ry + REG_ROW_H;
            graphics.fill(x + REG_X, ry, x + REG_X + REG_W, ry + REG_ROW_H, hovered ? 0xFF1F364A : 0xFF162736);
            graphics.fill(x + REG_X, ry, x + REG_X + 2, ry + REG_ROW_H, regRows.get(i).deployedReplicas() > 0 ? 0xFF34D399 : 0xFF2B4C65);
        }

        if (!this.storageTab) {
            // Container cards
            List<HallView.ContainerRow> conRows = view.containers();
            int conStart = this.conPage * CON_VISIBLE;
            int conEnd = Math.min(conRows.size(), conStart + CON_VISIBLE);
            for (int i = conStart; i < conEnd; i++) {
                HallView.ContainerRow row = conRows.get(i);
                int cy = y + CON_Y + (i - conStart) * (CON_ROW_H + 2);
                boolean hovered = mouseX >= x + CON_X && mouseX < x + CON_X + CON_W && mouseY >= cy && mouseY < cy + CON_ROW_H;
                graphics.fill(x + CON_X, cy, x + CON_X + CON_W, cy + CON_ROW_H, hovered ? 0xFF1F364A : 0xFF162736);
                graphics.fill(x + CON_X, cy, x + CON_X + 3, cy + CON_ROW_H, row.status().color());
                int barX = x + CON_X + 5;
                int barY = cy + CON_ROW_H - 5;
                int barW = CON_W - 10;
                double ratio = Math.max(0.0D, Math.min(1.0D, row.progress() / Math.max(1, row.cycleTicks())));
                int filled = (int) Math.round(barW * ratio);
                graphics.fill(barX, barY, barX + barW, barY + 3, 0xFF0A141D);
                graphics.fill(barX, barY, barX + filled, barY + 3, row.status().color());
            }
        } else {
            // Storage item grid cells
            List<StackCount> items = view.storedItems();
            int start = this.stoPage * STO_PER_PAGE;
            int end = Math.min(items.size(), start + STO_PER_PAGE);
            for (int i = start; i < end; i++) {
                int local = i - start;
                int col = local % STO_COLS;
                int row = local / STO_COLS;
                int cx = x + CON_X + col * STO_CELL_W;
                int cy = y + CON_Y + row * STO_CELL_H;
                boolean hovered = mouseX >= cx && mouseX < cx + STO_CELL_W - 2 && mouseY >= cy && mouseY < cy + STO_CELL_H - 2;
                graphics.fill(cx, cy, cx + STO_CELL_W - 2, cy + STO_CELL_H - 2, hovered ? 0xFF233E54 : 0xFF162736);
            }
        }
    }

    private void renderBar(GuiGraphics graphics, int x, int y, int width, int value, int max, int color) {
        graphics.fill(x, y, x + width, y + 18, 0xFF142432);
        int barY = y + 13;
        graphics.fill(x + 2, barY, x + width - 2, barY + 3, 0xFF09121A);
        double ratio = Math.max(0.0D, Math.min(1.0D, (double) value / Math.max(1, max)));
        int fillW = (int) Math.round((width - 4) * ratio);
        int fillColor = value > max ? 0xFFF87171 : color;
        graphics.fill(x + 2, barY, x + 2 + fillW, barY + 3, fillColor);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        HallView view = this.cachedView;
        HallCapacity cap = view.capacity();

        // Top title & rack/temp summary
        graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.title"), 8, 7, 0xE8F6FF, false);
        int tempInt = (int) Math.round(view.hallTempC());
        int tempColor = view.thermalShutdown() || tempInt >= 90 ? 0xF87171
                : tempInt >= 75 ? 0xFBBF24
                : (view.liquidCooled() && tempInt <= 50 ? 0x38BDF8 : 0x34D399);
        String summary = cap.readyRacks() > 0
                ? String.format(Locale.ROOT, "%dR · %.2fx · %d°C · %dW/%dW", cap.readyRacks(), cap.speedMultiplier(), tempInt, view.heatWatts(), view.coolingWatts())
                : Component.translatable("gui.hyperscale.orchestrator.offline").getString();
        int sumW = this.font.width(summary);
        graphics.drawString(this.font, summary, this.imageWidth - sumW - 8, 7, cap.readyRacks() > 0 ? tempColor : 0xF87171, false);

        // Telemetry labels inside the 5 header bars
        graphics.drawString(this.font, "R " + view.usedRamGb() + "/" + cap.totalRamGb() + "G", 9, 22, 0xBAE6FD, false);
        graphics.drawString(this.font, "C " + view.usedVcpu() + "/" + cap.totalVcpu(), 74, 22, 0xBFDBFE, false);
        String gpuText = cap.totalTflops() > 0 ? "G " + cap.totalTflops() + "TF" : "G None";
        graphics.drawString(this.font, gpuText, 135, 22, cap.totalTflops() > 0 ? 0xE9D5FF : 0x64748B, false);
        String dskText = "D " + formatCompact(view.usedStorageItems()) + "/" + formatCompact(view.totalStorageCapacity());
        graphics.drawString(this.font, dskText, 194, 22, view.totalStorageCapacity() > 0 ? 0x99F6E4 : 0x64748B, false);
        String pwrText = view.totalFePerTick() > 0
                ? "-" + formatCompact(view.totalFePerTick()) + "/t"
                : formatCompact(view.energyStored()) + "FE";
        graphics.drawString(this.font, pwrText, 265, 22, 0xFDE68A, false);

        // Left section header
        graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.images", view.registry().size()), REG_X, 46, 0x7DD3FC, false);

        if (!view.hasSwitch()) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.interface.no_switch"), CON_X + 4, CON_Y + 6, 0xF87171, false);
            return;
        }

        // Registry rows
        List<HallView.RegistryRow> regRows = view.registry();
        if (regRows.isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.no_images"), REG_X + 4, REG_Y + 6, 0x64748B, false);
        } else {
            int regStart = this.regPage * REG_VISIBLE;
            int regEnd = Math.min(regRows.size(), regStart + REG_VISIBLE);
            for (int i = regStart; i < regEnd; i++) {
                HallView.RegistryRow row = regRows.get(i);
                int ry = REG_Y + (i - regStart) * (REG_ROW_H + 2);
                graphics.renderItem(row.icon(), REG_X + 4, ry + 6);
                String trimmedName = this.font.plainSubstrByWidth(row.name().getString(), 96);
                graphics.drawString(this.font, trimmedName, REG_X + 23, ry + 4, 0xE2E8F0, false);
                String meta = row.ramPerReplica() + "G · " + row.vcpuPerReplica() + "c · "
                        + String.format(Locale.ROOT, "%.1fs", row.image().cycleTicks() / 20.0D);
                graphics.drawString(this.font, meta, REG_X + 23, ry + 15, 0x94A3B8, false);
            }
        }

        if (!this.storageTab) {
            // Container rows
            List<HallView.ContainerRow> conRows = view.containers();
            if (conRows.isEmpty()) {
                graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.no_containers"), CON_X + 4, CON_Y + 6, 0x64748B, false);
            } else {
                int conStart = this.conPage * CON_VISIBLE;
                int conEnd = Math.min(conRows.size(), conStart + CON_VISIBLE);
                for (int i = conStart; i < conEnd; i++) {
                    HallView.ContainerRow row = conRows.get(i);
                    int cy = CON_Y + (i - conStart) * (CON_ROW_H + 2);
                    graphics.renderItem(row.icon(), CON_X + 6, cy + 7);
                    String name = this.font.plainSubstrByWidth(row.name().getString(), 68);
                    graphics.drawString(this.font, name, CON_X + 25, cy + 4, 0xF1F5F9, false);
                    String rep = "x" + row.replicas();
                    graphics.drawString(this.font, rep, CON_X + 95, cy + 4, 0x38BDF8, false);

                    String statusLine = statusShort(row.status()) + " · " + row.ramUsed() + "G/" + row.vcpuUsed() + "c";
                    graphics.drawString(this.font, this.font.plainSubstrByWidth(statusLine, 142), CON_X + 25, cy + 16, row.status().color(), false);
                }
            }
        } else {
            // Storage grid items
            List<StackCount> items = view.storedItems();
            if (items.isEmpty()) {
                Component msg = view.totalStorageCapacity() > 0
                        ? Component.translatable("gui.hyperscale.orchestrator.storage_empty")
                        : Component.translatable("gui.hyperscale.orchestrator.storage_no_drives");
                graphics.drawString(this.font, msg, CON_X + 4, CON_Y + 6, 0x64748B, false);
            } else {
                int start = this.stoPage * STO_PER_PAGE;
                int end = Math.min(items.size(), start + STO_PER_PAGE);
                for (int i = start; i < end; i++) {
                    StackCount entry = items.get(i);
                    Item item = BuiltInRegistries.ITEM.get(entry.item());
                    if (item == Items.AIR) {
                        continue;
                    }
                    int local = i - start;
                    int col = local % STO_COLS;
                    int row = local / STO_COLS;
                    int cx = CON_X + col * STO_CELL_W;
                    int cy = CON_Y + row * STO_CELL_H;
                    graphics.renderItem(new ItemStack(item), cx + 5, cy + 3);
                    String cnt = formatCompact(entry.count());
                    int cw = this.font.width(cnt);
                    graphics.drawString(this.font, cnt, cx + (STO_CELL_W - 2 - cw) / 2, cy + 21, 0xE2E8F0, false);
                }
            }
        }

        // Footer warning, S.M.A.R.T. alert, or controls hint
        if (!view.missingParts().isEmpty()) {
            Component warn = Component.translatable("gui.hyperscale.interface.missing", String.join(", ", view.missingParts()));
            graphics.drawString(this.font, this.font.plainSubstrByWidth(warn.getString(), GUI_W - 16), 8, 208, 0xFBBF24, false);
        } else if (view.failedParts() > 0 || view.degradedParts() > 0) {
            Component alert = Component.translatable("gui.hyperscale.orchestrator.smart_alert", view.failedParts(), view.degradedParts());
            graphics.drawString(this.font, this.font.plainSubstrByWidth(alert.getString(), GUI_W - 16), 8, 208, view.failedParts() > 0 ? 0xF87171 : 0xFBBF24, false);
        } else if (this.storageTab) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.storage_hint"), 8, 208, 0x64748B, false);
        } else {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.orchestrator.hint"), 8, 208, 0x64748B, false);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderCardTooltips(graphics, mouseX, mouseY);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private void renderCardTooltips(GuiGraphics graphics, int mouseX, int mouseY) {
        HallView view = this.cachedView;
        int relX = mouseX - this.leftPos;
        int relY = mouseY - this.topPos;

        // Hover over top telemetry strip (Thermal, Cooling Watts, Noise, and PUE)
        if (view.hasSwitch() && relX >= 140 && relX < GUI_W - 6 && relY >= 4 && relY < 40) {
            List<Component> lines = new ArrayList<>();
            int tempInt = (int) Math.round(view.hallTempC());
            ChatFormatting tempFmt = view.thermalShutdown() || tempInt >= 90 ? ChatFormatting.RED
                    : tempInt >= 75 ? ChatFormatting.GOLD
                    : (view.liquidCooled() && tempInt <= 50 ? ChatFormatting.AQUA : ChatFormatting.GREEN);
            lines.add(Component.literal("Datacenter Telemetry").withStyle(ChatFormatting.WHITE));
            lines.add(Component.literal("Temp: " + tempInt + "°C"
                    + (view.thermalShutdown() ? " (SHUTDOWN)" : (view.liquidCooled() && tempInt <= 50 ? " (Cold Turbo +15%)" : "")))
                    .withStyle(tempFmt));
            lines.add(Component.literal("Heat: " + view.heatWatts() + "W / " + view.coolingWatts() + "W Cooling · " + view.noiseDb() + " dB")
                    .withStyle(ChatFormatting.GRAY));
            if (view.computeFePerTick() > 0) {
                boolean optimal = view.pue() <= 1.20D && tempInt < 75;
                ChatFormatting pueFmt = optimal ? ChatFormatting.GREEN : (view.pue() <= 1.50D ? ChatFormatting.YELLOW : ChatFormatting.RED);
                lines.add(Component.literal(String.format(Locale.ROOT, "PUE: %.2f%s", view.pue(), optimal ? " (Optimal -10% FE!)" : ""))
                        .withStyle(pueFmt));
                lines.add(Component.literal("IT Compute: " + view.computeFePerTick() + " FE/t · Cooling: " + view.coolingFePerTick() + " FE/t")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                lines.add(Component.literal("PUE: 1.00 (Idle) · Storage I/O: " + view.storageIoPerTick() + " items/t")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            return;
        }

        // Hover over Registry card
        if (relX >= REG_X && relX < REG_X + REG_W - 20) {
            List<HallView.RegistryRow> regRows = view.registry();
            int regStart = this.regPage * REG_VISIBLE;
            int regEnd = Math.min(regRows.size(), regStart + REG_VISIBLE);
            for (int i = regStart; i < regEnd; i++) {
                int ry = REG_Y + (i - regStart) * (REG_ROW_H + 2);
                if (relY >= ry && relY < ry + REG_ROW_H) {
                    HallView.RegistryRow row = regRows.get(i);
                    List<Component> lines = buildImageTooltip(row.name(), row.image(), row.ramPerReplica(), row.vcpuPerReplica(), 1, null, 0);
                    graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                    return;
                }
            }
        }

        if (!this.storageTab) {
            // Hover over Container card
            if (relX >= CON_X && relX < CON_X + CON_W - 60) {
                List<HallView.ContainerRow> conRows = view.containers();
                int conStart = this.conPage * CON_VISIBLE;
                int conEnd = Math.min(conRows.size(), conStart + CON_VISIBLE);
                for (int i = conStart; i < conEnd; i++) {
                    int cy = CON_Y + (i - conStart) * (CON_ROW_H + 2);
                    if (relY >= cy && relY < cy + CON_ROW_H) {
                        HallView.ContainerRow row = conRows.get(i);
                        if (row.image() != null) {
                            List<Component> lines = buildImageTooltip(
                                    row.name(),
                                    row.image(),
                                    row.ramUsed(),
                                    row.vcpuUsed(),
                                    row.replicas(),
                                    row.status(),
                                    row.fePerTick()
                            );
                            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                        }
                        return;
                    }
                }
            }
        } else {
            // Hover over Storage grid item
            StackCount hovered = this.hoveredStorageEntry(mouseX, mouseY);
            if (hovered != null) {
                Item item = BuiltInRegistries.ITEM.get(hovered.item());
                if (item != Items.AIR) {
                    List<Component> lines = List.of(
                            Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.WHITE),
                            Component.literal("Stored: " + hovered.count()).withStyle(ChatFormatting.AQUA),
                            Component.translatable("gui.hyperscale.orchestrator.storage_click").withStyle(ChatFormatting.DARK_GRAY)
                    );
                    graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                }
            }
        }
    }

    private static List<Component> buildImageTooltip(
            Component title,
            ProcessImage image,
            int ramGb,
            int vcpu,
            int replicas,
            DeploymentStatus status,
            int fePerTick
    ) {
        List<Component> lines = new ArrayList<>();
        lines.add(title.copy().withStyle(ChatFormatting.WHITE));
        if (status != null) {
            lines.add(Component.literal("Replicas: x" + replicas + " · " + statusShort(status)).withStyle(ChatFormatting.AQUA));
            lines.add(Component.literal("Reserved: " + ramGb + " GB RAM · " + vcpu + " vCPU").withStyle(ChatFormatting.GRAY));
            if (fePerTick > 0) {
                lines.add(Component.literal("Live Draw: " + fePerTick + " FE/t").withStyle(ChatFormatting.GOLD));
            }
        } else {
            lines.add(Component.literal("Cost / replica: " + ramGb + " GB RAM · " + vcpu + " vCPU").withStyle(ChatFormatting.AQUA));
        }
        lines.add(Component.literal("Cycle: " + String.format(Locale.ROOT, "%.1fs", image.cycleTicks() / 20.0D)
                + (image.energyPerCycle() > 0 ? " · " + image.energyPerCycle() + " FE/cycle" : "")).withStyle(ChatFormatting.GRAY));

        lines.add(Component.translatable("gui.hyperscale.interface.inputs").withStyle(ChatFormatting.YELLOW));
        for (StackCount in : image.inputs()) {
            Item item = BuiltInRegistries.ITEM.get(in.item());
            lines.add(Component.literal("  " + in.count() + "x ").append(Component.translatable(item.getDescriptionId())).withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.translatable("gui.hyperscale.interface.outputs").withStyle(ChatFormatting.GREEN));
        for (StackCount out : image.outputs()) {
            Item item = BuiltInRegistries.ITEM.get(out.item());
            lines.add(Component.literal("  " + out.count() + "x ").append(Component.translatable(item.getDescriptionId())).withStyle(ChatFormatting.GRAY));
        }
        return lines;
    }

    private static String statusShort(DeploymentStatus status) {
        return switch (status) {
            case RUNNING -> "Running";
            case PAUSED -> "Paused";
            case WAITING_INPUT -> "Wait Input";
            case OUTPUT_FULL -> "Disk Full";
            case NO_POWER -> "No Power";
            case OVERHEATED -> "OVERHEATED";
            case INSUFFICIENT_RESOURCES -> "Need RAM/CPU";
            case MISSING_IMAGE -> "No Image";
        };
    }

    private static String formatCompact(int value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0D);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.1fk", value / 1_000.0D);
        }
        return Integer.toString(value);
    }
}
