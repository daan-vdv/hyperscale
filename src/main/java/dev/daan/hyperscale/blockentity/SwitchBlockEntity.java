package dev.daan.hyperscale.blockentity;

import java.util.List;

import dev.daan.hyperscale.hall.HallImage;
import dev.daan.hyperscale.hall.HallRunner;
import dev.daan.hyperscale.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SwitchBlockEntity extends BlockEntity {
    private int progress;
    private int selectedImage;

    public SwitchBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SWITCH.get(), pos, state);
    }

    public static void serverTick(SwitchBlockEntity blockEntity) {
        HallRunner.tick(blockEntity);
    }

    public int progress() {
        return this.progress;
    }

    public void setProgress(int progress) {
        int next = Math.max(0, progress);
        if (this.progress != next) {
            this.progress = next;
            this.setChanged();
        }
    }

    public int selectedImage() {
        return this.selectedImage;
    }

    public void setSelectedImage(int index) {
        this.selectedImage = Math.max(0, index);
        this.sync();
    }

    public void clampSelection(Level level) {
        List<HallImage> images = HallRunner.listImages(level, this.getBlockPos());
        if (images.isEmpty()) {
            if (this.selectedImage != 0) {
                this.selectedImage = 0;
                this.sync();
            }
            return;
        }
        if (this.selectedImage >= images.size()) {
            this.selectedImage = images.size() - 1;
            this.sync();
        }
    }

    public void sync() {
        this.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Progress", this.progress);
        tag.putInt("SelectedImage", this.selectedImage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.progress = tag.getInt("Progress");
        this.selectedImage = tag.getInt("SelectedImage");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
