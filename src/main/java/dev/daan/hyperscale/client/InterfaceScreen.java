package dev.daan.hyperscale.client;

import java.util.ArrayList;
import java.util.List;

import dev.daan.hyperscale.hall.HallView;
import dev.daan.hyperscale.menu.InterfaceMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class InterfaceScreen extends AbstractContainerScreen<InterfaceMenu> {
    private static final int LIST_TOP = 22;
    private static final int ROW_HEIGHT = 14;
    private static final int LIST_WIDTH = 152;

    private final List<Button> imageButtons = new ArrayList<>();
    private int buttonCount = -1;

    public InterfaceScreen(InterfaceMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 210;
    }

    @Override
    protected void init() {
        super.init();
        this.rebuildButtons();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        HallView view = this.view();
        if (view.images().size() != this.buttonCount) {
            this.rebuildButtons();
            return;
        }
        for (int index = 0; index < this.imageButtons.size() && index < view.images().size(); index++) {
            this.imageButtons.get(index).setMessage(view.images().get(index).label());
        }
    }

    private void rebuildButtons() {
        for (Button button : this.imageButtons) {
            this.removeWidget(button);
        }
        this.imageButtons.clear();
        HallView view = this.view();
        this.buttonCount = view.images().size();
        int y = this.topPos + LIST_TOP;
        for (HallView.ImageRow row : view.images()) {
            int index = row.index();
            Button button = Button.builder(row.label(), ignored -> this.select(index))
                    .bounds(this.leftPos + 12, y, LIST_WIDTH, ROW_HEIGHT - 1)
                    .build();
            this.imageButtons.add(button);
            this.addRenderableWidget(button);
            y += ROW_HEIGHT;
        }
    }

    private void select(int index) {
        if (this.minecraft == null || this.minecraft.player == null) {
            return;
        }
        if (this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, index);
        }
        this.menu.clickMenuButton(this.minecraft.player, index);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;
        graphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF101820);
        graphics.fill(x + 4, y + 4, x + this.imageWidth - 4, y + this.imageHeight - 4, 0xFF173042);

        HallView view = this.view();
        if (view.cookingTime() > 0) {
            int barX = x + 12;
            int barY = y + this.imageHeight - 16;
            int width = this.imageWidth - 24;
            int filled = width * view.progress() / Math.max(1, view.cookingTime());
            graphics.fill(barX, barY, barX + width, barY + 4, 0xFF0C1C28);
            graphics.fill(barX, barY, barX + filled, barY + 4, 0xFF3DDC97);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 8, 0xE8F6FF, false);
        HallView view = this.view();
        int y = LIST_TOP + view.images().size() * ROW_HEIGHT + 6;
        if (view.images().isEmpty()) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.interface.empty"), 12, LIST_TOP + 2, 0x7FDBFF, false);
            y = LIST_TOP + 16;
        }
        for (Component line : view.detail()) {
            graphics.drawString(this.font, line, 12, y, 0x7FDBFF, false);
            y += 11;
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        HallView view = this.view();
        for (int index = 0; index < this.imageButtons.size(); index++) {
            Button button = this.imageButtons.get(index);
            boolean selected = index < view.images().size() && view.images().get(index).selected();
            button.setFocused(selected);
        }
        super.render(graphics, mouseX, mouseY, partialTick);
        this.renderTooltip(graphics, mouseX, mouseY);
    }

    private HallView view() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return new HallView(java.util.List.of(), 0, java.util.List.of(), 0, 0);
        }
        return HallView.inspect(
                this.minecraft.level,
                this.menu.pos(),
                this.menu.selectedImage(),
                this.menu.progress(),
                this.menu.cycleTicks()
        );
    }
}
