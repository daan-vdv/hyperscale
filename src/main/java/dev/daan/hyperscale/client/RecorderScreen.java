package dev.daan.hyperscale.client;

import java.util.ArrayList;
import java.util.List;

import dev.daan.hyperscale.blockentity.ProbeBlockEntity;
import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import dev.daan.hyperscale.menu.RecorderMenu;
import dev.daan.hyperscale.record.RecordLimits;
import dev.daan.hyperscale.record.RecordPhase;
import dev.daan.hyperscale.record.RecordStatus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class RecorderScreen extends AbstractContainerScreen<RecorderMenu> {
    private static final int VIEW_X = 8;
    private static final int VIEW_Y = 22;
    private static final int VIEW_W = 160;
    private static final int VIEW_H = 102;

    /** Remember tab & camera orientation when bouncing between Compiler 3D View and Probe Back button. */
    private static boolean lastTab3D = false;
    private static float savedYaw = -35.0F;
    private static float savedPitch = 28.0F;
    private static float savedZoom = 1.0F;

    private boolean tab3D = lastTab3D;
    private float yaw = savedYaw;
    private float pitch = savedPitch;
    private float zoom = savedZoom;
    private boolean dragging;
    private boolean draggedEnough;
    private double dragStartX;
    private double dragStartY;
    private CasingPreviewRenderer.HoveredProbe hoveredProbe;

    private Button overviewTabButton;
    private Button view3DTabButton;
    private Button primaryButton;
    private Button stopButton;
    private Button openInputButton;
    private Button cycleInputButton;
    private Button openOutputButton;
    private Button cycleOutputButton;
    private int selectedInput;
    private int selectedOutput;

    public RecorderScreen(RecorderMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 230;
    }

    @Override
    protected void init() {
        super.init();
        // Top-right tab switcher: Overview | 3D
        this.overviewTabButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.tab_overview"),
                button -> this.setTab(false)
        ).bounds(this.leftPos + 96, this.topPos + 4, 42, 14).build());

        this.view3DTabButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.tab_3d"),
                button -> this.setTab(true)
        ).bounds(this.leftPos + 140, this.topPos + 4, 30, 14).build());

        this.primaryButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.start"),
                button -> this.click(RecorderMenu.BUTTON_START)
        ).bounds(this.leftPos + 116, this.topPos + 64, 52, 16).build());

        this.stopButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.stop"),
                button -> this.click(RecorderMenu.BUTTON_STOP)
        ).bounds(this.leftPos + 116, this.topPos + 82, 52, 16).build());

        this.openInputButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.open_input", 1),
                button -> this.click(RecorderMenu.BUTTON_OPEN_INPUT_BASE + this.selectedInput)
        ).bounds(this.leftPos + 8, this.topPos + 100, 36, 15).build());

        this.cycleInputButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    int count = this.menu.data(10);
                    if (count > 1) {
                        this.selectedInput = (this.selectedInput + 1) % count;
                        this.updateProbeButtons();
                    }
                }
        ).bounds(this.leftPos + 45, this.topPos + 100, 14, 15).build());

        this.openOutputButton = this.addRenderableWidget(Button.builder(
                Component.translatable("gui.hyperscale.recorder.open_output", 1),
                button -> this.click(RecorderMenu.BUTTON_OPEN_OUTPUT_BASE + this.selectedOutput)
        ).bounds(this.leftPos + 8, this.topPos + 117, 36, 15).build());

        this.cycleOutputButton = this.addRenderableWidget(Button.builder(
                Component.literal(">"),
                button -> {
                    int count = this.menu.data(11);
                    if (count > 1) {
                        this.selectedOutput = (this.selectedOutput + 1) % count;
                        this.updateProbeButtons();
                    }
                }
        ).bounds(this.leftPos + 45, this.topPos + 117, 14, 15).build());

        this.applyTabVisibility();
        this.updateProbeButtons();
    }

    private void setTab(boolean show3D) {
        this.tab3D = show3D;
        lastTab3D = show3D;
        this.dragging = false;
        this.hoveredProbe = null;
        this.applyTabVisibility();
    }

    private void applyTabVisibility() {
        boolean overview = !this.tab3D;
        if (this.overviewTabButton != null) {
            this.overviewTabButton.active = this.tab3D;
        }
        if (this.view3DTabButton != null) {
            this.view3DTabButton.active = !this.tab3D;
        }
        if (this.primaryButton != null) this.primaryButton.visible = overview;
        if (this.stopButton != null) this.stopButton.visible = overview;
        if (this.openInputButton != null) this.openInputButton.visible = overview;
        if (this.cycleInputButton != null) this.cycleInputButton.visible = overview;
        if (this.openOutputButton != null) this.openOutputButton.visible = overview;
        if (this.cycleOutputButton != null) this.cycleOutputButton.visible = overview;
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
        this.updateProbeButtons();
    }

    private void updateProbeButtons() {
        int inputCount = this.menu.data(10);
        int outputCount = this.menu.data(11);
        boolean locked = this.menu.sampling();
        if (inputCount <= 0) {
            this.selectedInput = 0;
        } else if (this.selectedInput >= inputCount) {
            this.selectedInput = inputCount - 1;
        }
        if (outputCount <= 0) {
            this.selectedOutput = 0;
        } else if (this.selectedOutput >= outputCount) {
            this.selectedOutput = outputCount - 1;
        }
        if (this.openInputButton != null) {
            this.openInputButton.active = !locked && inputCount > 0;
            this.openInputButton.setMessage(Component.translatable("gui.hyperscale.recorder.open_input", this.selectedInput + 1));
            this.openInputButton.setTooltip(this.probeTooltip(true, this.selectedInput));
        }
        if (this.cycleInputButton != null) {
            this.cycleInputButton.active = !locked && inputCount > 1;
        }
        if (this.openOutputButton != null) {
            this.openOutputButton.active = !locked && outputCount > 0;
            this.openOutputButton.setMessage(Component.translatable("gui.hyperscale.recorder.open_output", this.selectedOutput + 1));
            this.openOutputButton.setTooltip(this.probeTooltip(false, this.selectedOutput));
        }
        if (this.cycleOutputButton != null) {
            this.cycleOutputButton.active = !locked && outputCount > 1;
        }
    }

    private RecorderBlockEntity recorderEntity() {
        if (this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        return this.minecraft.level.getBlockEntity(this.menu.pos()) instanceof RecorderBlockEntity recorder ? recorder : null;
    }

    private BlockPos selectedProbePos(boolean input, int index) {
        RecorderBlockEntity recorder = this.recorderEntity();
        if (recorder == null) {
            return null;
        }
        List<BlockPos> list = input ? recorder.inputPositions() : recorder.outputPositions();
        return (index >= 0 && index < list.size()) ? list.get(index) : null;
    }

    private Component probeTargetLabel(boolean input, int index) {
        BlockPos pos = this.selectedProbePos(input, index);
        if (pos == null || this.minecraft == null || this.minecraft.level == null) {
            return Component.translatable("gui.hyperscale.recorder.probe_none");
        }
        return RecorderBlockEntity.describeProbeTarget(this.minecraft.level, pos);
    }

    private Tooltip probeTooltip(boolean input, int index) {
        BlockPos pos = this.selectedProbePos(input, index);
        if (pos == null || this.minecraft == null || this.minecraft.level == null) {
            return null;
        }
        Component target = RecorderBlockEntity.describeProbeTarget(this.minecraft.level, pos);
        StringBuilder extra = new StringBuilder();
        extra.append("\n(").append(pos.getX()).append(", ").append(pos.getY()).append(", ").append(pos.getZ()).append(")");
        if (this.minecraft.level.getBlockEntity(pos) instanceof ProbeBlockEntity probe) {
            for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                ItemStack stack = probe.items().getStackInSlot(slot);
                if (!stack.isEmpty()) {
                    extra.append("\n").append(stack.getCount()).append("× ").append(stack.getHoverName().getString());
                    break;
                }
            }
        }
        return Tooltip.create(target.copy().append(Component.literal(extra.toString())));
    }

    private boolean inViewport(double mouseX, double mouseY) {
        int vx = this.leftPos + VIEW_X;
        int vy = this.topPos + VIEW_Y;
        return mouseX >= vx && mouseX <= vx + VIEW_W && mouseY >= vy && mouseY <= vy + VIEW_H;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.tab3D && this.inViewport(mouseX, mouseY) && (button == 0 || button == 1)) {
            this.dragging = true;
            this.draggedEnough = false;
            this.dragStartX = mouseX;
            this.dragStartY = mouseY;
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.tab3D && this.dragging) {
            if (Math.abs(mouseX - this.dragStartX) > 3.0D || Math.abs(mouseY - this.dragStartY) > 3.0D) {
                this.draggedEnough = true;
            }
            this.yaw += (float) dragX * 1.1F;
            this.pitch = Mth.clamp(this.pitch + (float) dragY * 1.1F, -85.0F, 85.0F);
            savedYaw = this.yaw;
            savedPitch = this.pitch;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (this.tab3D && this.dragging) {
            boolean wasClick = !this.draggedEnough && button == 0;
            this.dragging = false;
            if (wasClick && this.hoveredProbe != null && !this.menu.sampling()) {
                int id = (this.hoveredProbe.input() ? RecorderMenu.BUTTON_OPEN_INPUT_BASE : RecorderMenu.BUTTON_OPEN_OUTPUT_BASE)
                        + this.hoveredProbe.index();
                this.click(id);
            }
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.tab3D && this.inViewport(mouseX, mouseY)) {
            this.zoom = Mth.clamp(this.zoom + (float) scrollY * 0.15F, 0.45F, 2.6F);
            savedZoom = this.zoom;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    protected void renderSlot(GuiGraphics graphics, Slot slot) {
        if (this.tab3D && slot.index == 0) {
            return;
        }
        super.renderSlot(graphics, slot);
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
            if (this.tab3D && slot.index == 0) {
                continue;
            }
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF8B8B8B);
            graphics.fill(x, y, x + 16, y + 16, 0xFF373737);
        }

        if (this.tab3D) {
            int vx = this.leftPos + VIEW_X;
            int vy = this.topPos + VIEW_Y;
            graphics.fill(vx - 1, vy - 1, vx + VIEW_W + 1, vy + VIEW_H + 1, 0xFF2B4C66);
            graphics.fill(vx, vy, vx + VIEW_W, vy + VIEW_H, 0xFF0A141D);
            RecorderBlockEntity recorder = this.recorderEntity();
            if (recorder != null && recorder.hasBox() && this.minecraft != null && this.minecraft.level != null) {
                this.hoveredProbe = CasingPreviewRenderer.render(
                        graphics, this.minecraft.level, recorder,
                        vx, vy, VIEW_W, VIEW_H,
                        this.yaw, this.pitch, this.zoom,
                        mouseX, mouseY
                );
            } else {
                this.hoveredProbe = null;
            }
        } else {
            this.hoveredProbe = null;
            int cycles = this.menu.data(2);
            int barX = this.leftPos + 8;
            int barY = this.topPos + 54;
            for (int cycle = 0; cycle < RecordLimits.CYCLES; cycle++) {
                int x = barX + cycle * 14;
                graphics.fill(x, barY, x + 10, barY + 6, cycle < cycles ? 0xFF3DDC97 : 0xFF0C1C28);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, 8, 6, 0xE8F6FF, false);

        if (this.tab3D) {
            RecorderBlockEntity recorder = this.recorderEntity();
            if (recorder == null || !recorder.hasBox()) {
                this.drawTrimmed(graphics, Component.translatable("gui.hyperscale.recorder.view3d_empty"), 14, 68, 148, 0x7FDBFF);
            }
            this.drawTrimmed(graphics, Component.translatable("gui.hyperscale.recorder.view3d_hint"), 8, 127, 160, 0x8EB4CC);
            graphics.drawString(this.font, this.playerInventoryTitle, 8, 137, 0xC8D6E0, false);
            return;
        }

        graphics.drawString(this.font, Component.translatable(
                "gui.hyperscale.recorder.runs",
                Math.min(this.menu.data(2) + (this.menu.data(0) == RecordPhase.LEARNING.ordinal() && this.menu.data(1) == 0 ? 1 : 0), RecordLimits.CYCLES),
                RecordLimits.CYCLES
        ), 8, 22, 0xE8F6FF, false);
        graphics.drawString(this.font, Component.translatable(
                "gui.hyperscale.recorder.binds",
                this.menu.data(10),
                this.menu.data(11)
        ), 80, 22, 0x7FDBFF, false);

        List<FormattedCharSequence> statusLines = this.font.split(this.statusLine(RecordStatus.byId(this.menu.data(7))), 160);
        int statusY = 33;
        for (int i = 0; i < Math.min(2, statusLines.size()); i++) {
            graphics.drawString(this.font, statusLines.get(i), 8, statusY + i * 10, 0x7FDBFF, false);
        }

        int width = this.menu.data(4);
        int height = this.menu.data(5);
        int depth = this.menu.data(6);
        if (width > 0 && height > 0 && depth > 0) {
            Component claim = Component.translatable("gui.hyperscale.recorder.claim", width, height, depth, this.menu.data(3));
            this.drawTrimmed(graphics, claim, 56, 53, 114, 0x7FDBFF);
        }
        graphics.drawString(this.font, Component.translatable("gui.hyperscale.recorder.card"), 48, 78, 0xC8D6E0, false);

        this.drawTrimmed(graphics, this.probeTargetLabel(true, this.selectedInput), 63, 104, 106, 0xA0E8B0);
        this.drawTrimmed(graphics, this.probeTargetLabel(false, this.selectedOutput), 63, 121, 106, 0xF5C28B);

        graphics.drawString(this.font, this.playerInventoryTitle, 8, 137, 0xC8D6E0, false);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        if (this.tab3D && this.hoveredProbe != null && this.minecraft != null && this.minecraft.level != null) {
            List<Component> lines = new ArrayList<>();
            Component prefix = Component.translatable(
                    this.hoveredProbe.input() ? "gui.hyperscale.recorder.probe_input" : "gui.hyperscale.recorder.probe_output",
                    this.hoveredProbe.index() + 1
            );
            Component target = RecorderBlockEntity.describeProbeTarget(this.minecraft.level, this.hoveredProbe.pos());
            lines.add(Component.translatable("gui.hyperscale.recorder.probe_title", prefix, target));
            if (this.minecraft.level.getBlockEntity(this.hoveredProbe.pos()) instanceof ProbeBlockEntity probe) {
                for (int slot = 0; slot < probe.items().getSlots(); slot++) {
                    ItemStack stack = probe.items().getStackInSlot(slot);
                    if (!stack.isEmpty()) {
                        lines.add(Component.literal(stack.getCount() + "× " + stack.getHoverName().getString()));
                        break;
                    }
                }
            }
            lines.add(Component.translatable(this.menu.sampling()
                    ? "gui.hyperscale.recorder.status.claim_locked"
                    : "gui.hyperscale.recorder.view3d_click"));
            graphics.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        } else {
            this.renderTooltip(graphics, mouseX, mouseY);
        }
    }

    private void drawTrimmed(GuiGraphics graphics, Component text, int x, int y, int maxWidth, int color) {
        List<FormattedCharSequence> lines = this.font.split(text, maxWidth);
        if (!lines.isEmpty()) {
            graphics.drawString(this.font, lines.get(0), x, y, color, false);
        }
    }

    private Component statusLine(RecordStatus status) {
        if (status == RecordStatus.RUN_DONE) {
            return Component.translatable(status.key(), this.menu.data(2), RecordLimits.CYCLES);
        }
        RecorderBlockEntity recorder = this.recorderEntity();
        String detail = recorder != null ? recorder.detail() : "";
        if (status == RecordStatus.FORBIDDEN_BLOCK) {
            ResourceLocation id = ResourceLocation.tryParse(detail);
            if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) {
                return Component.translatable(status.key(), BuiltInRegistries.BLOCK.get(id).getName());
            }
            return Component.translatable(status.key(), detail.isEmpty() ? Component.translatable("gui.hyperscale.recorder.unknown_item") : Component.literal(detail));
        }
        if (status == RecordStatus.FLUID_DRIFT) {
            ResourceLocation id = ResourceLocation.tryParse(detail);
            if (id != null && BuiltInRegistries.FLUID.containsKey(id)) {
                return Component.translatable(status.key(), BuiltInRegistries.FLUID.get(id).getFluidType().getDescription());
            }
            return Component.translatable(status.key(), Component.translatable("gui.hyperscale.recorder.unknown_item"));
        }
        if (status != RecordStatus.DRIFT) {
            return Component.translatable(status.key());
        }
        ResourceLocation id = ResourceLocation.tryParse(detail);
        if (id == null) {
            return Component.translatable(status.key(), Component.translatable("gui.hyperscale.recorder.unknown_item"));
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        return Component.translatable(status.key(), Component.translatable(item.getDescriptionId()));
    }
}
