package dev.daan.hyperscale.client;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import dev.daan.hyperscale.blockentity.RecorderBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;

/**
 * Renders an interactive 3D miniature of the sealed compiler casing's interior inside the Compiler GUI
 * and performs screen-space hit-testing on interior Input/Output Probes so the player can click them directly.
 */
public final class CasingPreviewRenderer {
    public record HoveredProbe(boolean input, int index, BlockPos pos) {
    }

    private CasingPreviewRenderer() {
    }

    public static HoveredProbe render(
            GuiGraphics graphics,
            Level level,
            RecorderBlockEntity recorder,
            int viewX,
            int viewY,
            int viewW,
            int viewH,
            float yaw,
            float pitch,
            float zoom,
            int mouseX,
            int mouseY
    ) {
        if (level == null || recorder == null || !recorder.hasBox()) {
            return null;
        }
        BlockPos min = recorder.innerMin();
        BlockPos max = recorder.innerMax();
        int sizeX = max.getX() - min.getX() + 1;
        int sizeY = max.getY() - min.getY() + 1;
        int sizeZ = max.getZ() - min.getZ() + 1;
        if (sizeX <= 0 || sizeY <= 0 || sizeZ <= 0 || sizeX > 32 || sizeY > 32 || sizeZ > 32) {
            return null;
        }

        float centerX = (min.getX() + max.getX() + 1) * 0.5F;
        float centerY = (min.getY() + max.getY() + 1) * 0.5F;
        float centerZ = (min.getZ() + max.getZ() + 1) * 0.5F;
        float maxDim = Math.max(1.0F, Math.max(sizeX, Math.max(sizeY, sizeZ)));
        float scale = (Math.min(viewW, viewH) * 0.62F / maxDim) * zoom;

        graphics.flush();
        graphics.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        PoseStack pose = graphics.pose();
        pose.pushPose();
        pose.translate(viewX + viewW * 0.5F, viewY + viewH * 0.5F, 200.0F);
        // GUI ortho already flips Y (det = -1), so scaling (s, -s, +s) gives total det = +1
        // (correct front-face culling), maps world +Y up, and keeps +Z closer to the camera.
        pose.scale(scale, -scale, scale);
        pose.mulPose(Axis.XP.rotationDegrees(pitch));
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.translate(-centerX, -centerY, -centerZ);

        Matrix4f viewMatrix = new Matrix4f(pose.last().pose());
        HoveredProbe hovered = pickProbe(viewMatrix, recorder, viewX, viewY, viewW, viewH, mouseX, mouseY);

        Minecraft minecraft = Minecraft.getInstance();
        BlockRenderDispatcher blockRenderer = minecraft.getBlockRenderer();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        Lighting.setupFor3DItems();
        RenderSystem.enableDepthTest();

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    cursor.set(x, y, z);
                    if (!level.isLoaded(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (state.isAir()) {
                        continue;
                    }
                    pose.pushPose();
                    pose.translate(x, y, z);
                    RenderShape shape = state.getRenderShape();
                    if (shape == RenderShape.MODEL) {
                        blockRenderer.renderSingleBlock(
                                state,
                                pose,
                                bufferSource,
                                LightTexture.FULL_BRIGHT,
                                OverlayTexture.NO_OVERLAY,
                                ModelData.EMPTY,
                                null
                        );
                    } else if (shape == RenderShape.ENTITYBLOCK_ANIMATED) {
                        ItemStack stack = new ItemStack(state.getBlock());
                        if (!stack.isEmpty()) {
                            pose.translate(0.5F, 0.5F, 0.5F);
                            itemRenderer.renderStatic(
                                    stack,
                                    ItemDisplayContext.FIXED,
                                    LightTexture.FULL_BRIGHT,
                                    OverlayTexture.NO_OVERLAY,
                                    pose,
                                    bufferSource,
                                    level,
                                    0
                            );
                        }
                    }
                    pose.popPose();
                }
            }
        }

        // Subtle interior wireframe + color-coded outlines around Input/Output probes
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                pose, lines,
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1.0D, max.getY() + 1.0D, max.getZ() + 1.0D,
                0.35F, 0.65F, 0.85F, 0.35F
        );
        drawProbeHighlights(pose, lines, recorder.inputPositions(), true, hovered);
        drawProbeHighlights(pose, lines, recorder.outputPositions(), false, hovered);

        bufferSource.endBatch();
        pose.popPose();
        graphics.disableScissor();
        return hovered;
    }

    private static void drawProbeHighlights(
            PoseStack pose,
            VertexConsumer lines,
            List<BlockPos> probes,
            boolean input,
            HoveredProbe hovered
    ) {
        for (int i = 0; i < probes.size(); i++) {
            BlockPos pos = probes.get(i);
            boolean isHovered = hovered != null && hovered.input() == input && hovered.index() == i;
            float r = isHovered ? 1.0F : (input ? 0.25F : 0.95F);
            float g = isHovered ? 1.0F : (input ? 0.92F : 0.55F);
            float b = isHovered ? 1.0F : (input ? 0.50F : 0.20F);
            float pad = isHovered ? 0.04F : 0.015F;
            LevelRenderer.renderLineBox(
                    pose, lines,
                    pos.getX() - pad, pos.getY() - pad, pos.getZ() - pad,
                    pos.getX() + 1.0D + pad, pos.getY() + 1.0D + pad, pos.getZ() + 1.0D + pad,
                    r, g, b, 1.0F
            );
        }
    }

    private static HoveredProbe pickProbe(
            Matrix4f viewMatrix,
            RecorderBlockEntity recorder,
            int viewX,
            int viewY,
            int viewW,
            int viewH,
            int mouseX,
            int mouseY
    ) {
        if (mouseX < viewX || mouseX > viewX + viewW || mouseY < viewY || mouseY > viewY + viewH) {
            return null;
        }
        HoveredProbe best = null;
        float bestZ = Float.NEGATIVE_INFINITY;
        Vector3f tmp = new Vector3f();

        List<BlockPos> inputs = recorder.inputPositions();
        for (int i = 0; i < inputs.size(); i++) {
            BlockPos pos = inputs.get(i);
            float z = hitDepth(viewMatrix, pos, mouseX, mouseY, tmp);
            if (z > bestZ) {
                bestZ = z;
                best = new HoveredProbe(true, i, pos);
            }
        }
        List<BlockPos> outputs = recorder.outputPositions();
        for (int i = 0; i < outputs.size(); i++) {
            BlockPos pos = outputs.get(i);
            float z = hitDepth(viewMatrix, pos, mouseX, mouseY, tmp);
            if (z > bestZ) {
                bestZ = z;
                best = new HoveredProbe(false, i, pos);
            }
        }
        return best;
    }

    private static float hitDepth(Matrix4f matrix, BlockPos pos, int mouseX, int mouseY, Vector3f tmp) {
        float minX = Float.POSITIVE_INFINITY;
        float maxX = Float.NEGATIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float sumZ = 0.0F;
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                for (int dz = 0; dz <= 1; dz++) {
                    tmp.set(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz);
                    matrix.transformPosition(tmp);
                    if (tmp.x < minX) minX = tmp.x;
                    if (tmp.x > maxX) maxX = tmp.x;
                    if (tmp.y < minY) minY = tmp.y;
                    if (tmp.y > maxY) maxY = tmp.y;
                    sumZ += tmp.z;
                }
            }
        }
        if (mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY) {
            return sumZ * 0.125F;
        }
        return Float.NEGATIVE_INFINITY;
    }
}
