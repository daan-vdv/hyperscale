package dev.daan.hyperscale.client;

import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
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
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xE8F6FF, false);
        if (this.menu.kind() == StorageKind.RACK) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.server"), 26, 28, 0x7FDBFF, false);
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.cpu"), 80, 28, 0x7FDBFF, false);
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.ram"), 134, 28, 0x7FDBFF, false);
        } else if (this.menu.kind() == StorageKind.DRIVE) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.image"), 80, 28, 0x7FDBFF, false);
        } else if (this.minecraft != null && this.minecraft.level != null && this.menu.kind() == StorageKind.PORT) {
            PortMode mode = this.minecraft.level.getBlockState(this.menu.pos()).getValue(PortBlock.MODE);
            graphics.drawString(this.font, Component.translatable("message.hyperscale.port." + mode.getSerializedName()), 8, 28, 0x7FDBFF, false);
        }
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0xC8D6E0, false);
    }
}
