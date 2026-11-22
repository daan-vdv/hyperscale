package dev.daan.hyperscale.client;

import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.menu.RecorderMenu;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.record.RecordStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;

public class RecorderScreen extends AbstractContainerScreen<RecorderMenu> {
    private Button primaryButton;

    public RecorderScreen(RecorderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 206;
    }

    @Override
    protected void init() {
        super.init();
        this.primaryButton = this.addRenderableWidget(Button.builder(Component.translatable("gui.hyperscale.recorder.start"), button -> this.click(2))
                .bounds(this.leftPos + 116, this.topPos + 74, 52, 16)
                .build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.hyperscale.recorder.stop"), button -> this.click(3))
                .bounds(this.leftPos + 116, this.topPos + 94, 52, 16)
                .build());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.primaryButton != null) {
            boolean cont = this.menu.data(1) == 1;
            this.primaryButton.setMessage(Component.translatable(cont
                    ? "gui.hyperscale.recorder.continue"
                    : "gui.hyperscale.recorder.start"));
        }
    }

    private void click(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
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
        int cycles = this.menu.data(2);
        int barX = this.leftPos + 8;
        int barY = this.topPos + 64;
        for (int cycle = 0; cycle < RecordLimits.CYCLES; cycle++) {
            int x = barX + cycle * 14;
            graphics.fill(x, barY, x + 10, barY + 6, cycle < cycles ? 0xFF3DDC97 : 0xFF0C1C28);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xE8F6FF, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.hyperscale.recorder.runs",
                Math.min(this.menu.data(2) + (this.menu.data(0) == RecordPhase.LEARNING.ordinal() && this.menu.data(1) == 0 ? 1 : 0), RecordLimits.CYCLES),
                RecordLimits.CYCLES
        ), 8, 22, 0xE8F6FF, false);
        graphics.drawString(this.font, this.statusLine(RecordStatus.byId(this.menu.data(7))), 8, 40, 0x7FDBFF, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.hyperscale.recorder.binds",
                this.menu.data(10),
                this.menu.data(11)
        ), 8, 52, 0x7FDBFF, false);
        int width = this.menu.data(4);
        int height = this.menu.data(5);
        int depth = this.menu.data(6);
        if (width > 0 && height > 0 && depth > 0) {
            graphics.drawString(this.font, Component.translatable(
                    "gui.hyperscale.recorder.claim",
                    width, height, depth, this.menu.data(3)
            ), 56, 62, 0x7FDBFF, false);
        }
        graphics.drawString(this.font, Component.translatable("gui.hyperscale.recorder.card"), 48, 82, 0xC8D6E0, false);
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 112, 0xC8D6E0, false);
    }

    private Component statusLine(RecordStatus status) {
        if (status == RecordStatus.RUN_DONE) {
            return Component.translatable(status.key(), this.menu.data(2), RecordLimits.CYCLES);
        }
        if (status != RecordStatus.DRIFT) {
            return Component.translatable(status.key());
        }
        String detail = "";
        if (this.minecraft != null && this.minecraft.level != null
                && this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof RecorderBlockEntity recorder) {
            detail = recorder.detail();
        }
        ResourceLocation id = ResourceLocation.tryParse(detail);
        if (id == null) {
            return Component.translatable(status.key(), Component.translatable("gui.hyperscale.recorder.unknown_item"));
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return Component.translatable(status.key(), Component.translatable(item.getDescriptionId()));
    }
}
