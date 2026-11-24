package dev.daan.hyperscale.client;

import java.util.List;

import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import dev.daan.hyperscale.network.DriveBayActionPayload;
import dev.daan.hyperscale.network.PortFilterPayload;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

public class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final int FILTER_X = 110;
    private static final int FILTER_Y = 40;
    private static final int FILTER_STEP = 18;

    private Button raidButton;
    private Button salvageButton;

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        if (this.menu.isRemoteProbe()) {
            this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.hyperscale.storage.back"),
                    button -> {
                        if (this.minecraft != null && this.minecraft.gameMode != null) {
                            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, StorageMenu.BUTTON_BACK);
                        }
                    }
            ).bounds(this.leftPos + 128, this.topPos + 4, 42, 14).build());
        }
        if (this.menu.kind() == StorageKind.DRIVE) {
            this.raidButton = this.addRenderableWidget(Button.builder(
                    Component.literal("RAID"),
                    button -> PacketDistributor.sendToServer(new DriveBayActionPayload(this.menu.pos(), DriveBayActionPayload.ACTION_CYCLE_RAID))
            ).bounds(this.leftPos + 112, this.topPos + 4, 58, 14).build());

            this.salvageButton = this.addRenderableWidget(Button.builder(
                    Component.translatable("gui.hyperscale.drive.salvage"),
                    button -> PacketDistributor.sendToServer(new DriveBayActionPayload(this.menu.pos(), DriveBayActionPayload.ACTION_SALVAGE_FAILED))
            ).bounds(this.leftPos + 118, this.topPos + 62, 52, 14).build());
            this.updateDriveBayButtons();
        }
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (this.menu.kind() == StorageKind.DRIVE) {
            this.updateDriveBayButtons();
        }
    }

    private void updateDriveBayButtons() {
        DriveBayBlockEntity bay = this.driveBay();
        if (this.raidButton != null && bay != null) {
            this.raidButton.setMessage(Component.literal(bay.raidMode().label()));
        }
        if (this.salvageButton != null) {
            this.salvageButton.visible = bay != null && bay.failedDriveCount() > 0;
        }
    }

    private DriveBayBlockEntity driveBay() {
        if (this.menu.kind() != StorageKind.DRIVE || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof DriveBayBlockEntity bay ? bay : null;
    }

    private PortBlockEntity outputPort() {
        if (this.menu.kind() != StorageKind.PORT || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        BlockState state = this.minecraft.level.getBlockState(this.menu.pos());
        if (!state.is(ModBlocks.PORT.get()) || !state.hasProperty(PortBlock.MODE) || state.getValue(PortBlock.MODE) != PortMode.OUTPUT) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof PortBlockEntity port ? port : null;
    }

    private int hoveredFilterSlot(double mouseX, double mouseY) {
        if (this.outputPort() == null) {
            return -1;
        }
        int relX = (int) mouseX - this.leftPos;
        int relY = (int) mouseY - this.topPos;
        if (relY < FILTER_Y || relY >= FILTER_Y + 16) {
            return -1;
        }
        for (int i = 0; i < PortBlockEntity.FILTER_SLOTS; i++) {
            int sx = FILTER_X + i * FILTER_STEP;
            if (relX >= sx && relX < sx + 16) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int filterSlot = this.hoveredFilterSlot(mouseX, mouseY);
        if (filterSlot >= 0 && (button == 0 || button == 1)) {
            ItemStack carried = this.menu.getCarried();
            ItemStack next = (button == 1 || carried.isEmpty()) ? ItemStack.EMPTY : carried.copyWithCount(1);
            PacketDistributor.sendToServer(new PortFilterPayload(this.menu.pos(), filterSlot, next));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
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

        PortBlockEntity port = this.outputPort();
        if (port != null) {
            int hovered = this.hoveredFilterSlot(mouseX, mouseY);
            for (int i = 0; i < PortBlockEntity.FILTER_SLOTS; i++) {
                int x = this.leftPos + FILTER_X + i * FILTER_STEP;
                int y = this.topPos + FILTER_Y;
                graphics.fill(x - 1, y - 1, x + 17, y + 17, hovered == i ? 0xFFFBBF24 : 0xFFB8862B);
                graphics.fill(x, y, x + 16, y + 16, 0xFF1A2A38);
                ItemStack ghost = port.getFilterSlot(i);
                if (!ghost.isEmpty()) {
                    graphics.renderItem(ghost, x, y);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        int maxTitleWidth = this.menu.isRemoteProbe() ? 118 : 160;
        var titleLines = this.font.split(this.title, maxTitleWidth);
        if (!titleLines.isEmpty()) {
            graphics.drawString(this.font, titleLines.get(0), 8, 6, 0xE8F6FF, false);
        }
        if (this.menu.kind() == StorageKind.RACK) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.server"), 12, 28, 0x7FDBFF, false);
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.cpu"), 59, 28, 0x7FDBFF, false);
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.ram"), 101, 28, 0x7FDBFF, false);
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.gpu"), 143, 28, 0xC084FC, false);
        } else if (this.menu.kind() == StorageKind.DRIVE) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.drives"), 62, 12, 0x7FDBFF, false);
        } else if (this.menu.kind() == StorageKind.SWITCH) {
            graphics.drawString(this.font, Component.translatable("gui.hyperscale.slot.image"), 62, 12, 0x7FDBFF, false);
        } else if (this.minecraft != null && this.minecraft.level != null && this.menu.kind() == StorageKind.PORT) {
            BlockState state = this.minecraft.level.getBlockState(this.menu.pos());
            if (state.is(ModBlocks.PORT.get()) && state.hasProperty(PortBlock.MODE)) {
                PortMode mode = state.getValue(PortBlock.MODE);
                graphics.drawString(this.font, Component.translatable("message.hyperscale.port." + mode.getSerializedName()), 8, 28, 0x7FDBFF, false);
                if (mode == PortMode.OUTPUT) {
                    graphics.drawString(this.font, Component.translatable("gui.hyperscale.port.whitelist"), FILTER_X, 28, 0xFBBF24, false);
                }
            }
        }
        graphics.drawString(this.font, this.playerInventoryTitle, 8, 72, 0xC8D6E0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int filterSlot = this.hoveredFilterSlot(mouseX, mouseY);
        PortBlockEntity port = this.outputPort();
        if (filterSlot >= 0 && port != null) {
            ItemStack ghost = port.getFilterSlot(filterSlot);
            if (ghost.isEmpty()) {
                graphics.renderComponentTooltip(this.font, List.of(
                        Component.translatable("tooltip.hyperscale.port.whitelist_empty").withStyle(ChatFormatting.YELLOW),
                        Component.translatable("tooltip.hyperscale.port.whitelist_hint").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
            } else {
                graphics.renderComponentTooltip(this.font, List.of(
                        ghost.getHoverName().copy().withStyle(ChatFormatting.WHITE),
                        Component.translatable("tooltip.hyperscale.port.whitelist_set").withStyle(ChatFormatting.GRAY)
                ), mouseX, mouseY);
            }
            return;
        }
        this.renderTooltip(graphics, mouseX, mouseY);
    }
}
