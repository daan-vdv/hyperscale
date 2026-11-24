package dev.daan.hyperscale.client;

import dev.daan.hyperscale.menu.WaferPressMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class WaferPressScreen extends AbstractContainerScreen<WaferPressMenu> {
    public WaferPressScreen(WaferPressMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        graphics.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, 0xFF101820);
        graphics.fill(this.leftPos + 4, this.topPos + 4, this.leftPos + this.imageWidth - 4, this.topPos + this.imageHeight - 4, 0xFF173042);

        for (Slot slot : this.menu.slots) {
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
            graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
        }

        // Energy bar (left)
        int ex = this.leftPos + 14;
        int ey = this.topPos + 20;
        int eh = 46;
        graphics.fill(ex - 1, ey - 1, ex + 9, ey + eh + 1, 0xFF556B7A);
        graphics.fill(ex, ey, ex + 8, ey + eh, 0xFF101820);
        int filledEnergy = (int) Math.round((double) this.menu.energyStored() / this.menu.maxEnergy() * eh);
        if (filledEnergy > 0) {
            graphics.fill(ex, ey + eh - filledEnergy, ex + 8, ey + eh, 0xFFF59E0B);
        }

        // Progress bar (center arrow)
        int px = this.leftPos + 90;
        int py = this.topPos + 39;
        int pw = 28;
        graphics.fill(px - 1, py - 1, px + pw + 1, py + 9, 0xFF556B7A);
        graphics.fill(px, py, px + pw, py + 8, 0xFF101820);
        int filledProg = (int) Math.round((double) this.menu.progress() / this.menu.maxProgress() * pw);
        if (filledProg > 0) {
            graphics.fill(px, py, px + filledProg, py + 8, 0xFF38BDF8);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xE8F6FF, false);
        graphics.drawString(this.font, Component.literal("Base"), 41, 24, 0x7FDBFF, false);
        graphics.drawString(this.font, Component.literal("Dopant"), 63, 24, 0x94A3B8, false);
        if (this.menu.energyStored() <= 0) {
            graphics.drawString(this.font, Component.literal("No FE Power"), 90, 56, 0xF87171, false);
        }
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0xC8D6E0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int ex = this.leftPos + 14;
        int ey = this.topPos + 20;
        if (mouseX >= ex && mouseX <= ex + 8 && mouseY >= ey && mouseY <= ey + 46) {
            graphics.renderTooltip(this.font, Component.literal(this.menu.energyStored() + " / " + this.menu.maxEnergy() + " FE"), mouseX, mouseY);
            return;
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
