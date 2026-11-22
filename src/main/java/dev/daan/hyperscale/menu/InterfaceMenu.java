package dev.daan.hyperscale.menu;

import java.util.Collections;
import java.util.List;

import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallImage;
import dev.daan.hyperscale.hall.HallParts;
import dev.daan.hyperscale.hall.HallRunner;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class InterfaceMenu extends AbstractContainerMenu {
    public static final int DATA_SELECTED = 0;
    public static final int DATA_PROGRESS = 1;
    public static final int DATA_CYCLE = 2;
    public static final int DATA_SIZE = 3;

    private final BlockPos pos;
    private final ContainerData data;

    public InterfaceMenu(int containerId, Inventory inventory, BlockPos pos, ContainerData data) {
        super(ModMenus.INTERFACE.get(), containerId);
        this.pos = pos;
        this.data = data;
        this.addDataSlots(data);
    }

    public static InterfaceMenu fromBuffer(int containerId, Inventory inventory, net.minecraft.network.RegistryFriendlyByteBuf buffer) {
        return new InterfaceMenu(containerId, inventory, buffer.readBlockPos(), new SimpleContainerData(DATA_SIZE));
    }

    public static InterfaceMenu create(int containerId, Inventory inventory, BlockPos pos) {
        Level level = inventory.player.level();
        SwitchBlockEntity switchEntity = findSwitch(level, pos);
        ContainerData data = new ContainerData() {
            @Override
            public int get(int index) {
                if (switchEntity == null) {
                    return 0;
                }
                return switch (index) {
                    case DATA_SELECTED -> switchEntity.selectedImage();
                    case DATA_PROGRESS -> switchEntity.progress();
                    case DATA_CYCLE -> {
                        List<HallImage> images = HallRunner.listImages(level, pos);
                        if (images.isEmpty()) {
                            yield 0;
                        }
                        int selected = Math.min(switchEntity.selectedImage(), images.size() - 1);
                        yield images.get(selected).image().cycleTicks();
                    }
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (switchEntity == null || index != DATA_SELECTED) {
                    return;
                }
                switchEntity.setSelectedImage(value);
                switchEntity.setProgress(0);
            }

            @Override
            public int getCount() {
                return DATA_SIZE;
            }
        };
        return new InterfaceMenu(containerId, inventory, pos, data);
    }

    public BlockPos pos() {
        return this.pos;
    }

    public int selectedImage() {
        return Math.max(0, this.data.get(DATA_SELECTED));
    }

    public int progress() {
        return Math.max(0, this.data.get(DATA_PROGRESS));
    }

    public int cycleTicks() {
        return Math.max(0, this.data.get(DATA_CYCLE));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id < 0) {
            return false;
        }
        Level level = player.level();
        HallParts parts = HallFinder.find(level, this.pos);
        if (!parts.hasSwitch()) {
            return false;
        }
        List<HallImage> images = HallRunner.listImages(level, parts);
        if (id >= images.size()) {
            return false;
        }
        SwitchBlockEntity switchEntity = findSwitch(level, this.pos);
        if (switchEntity == null) {
            return false;
        }
        switchEntity.setSelectedImage(id);
        switchEntity.setProgress(0);
        if (level.isClientSide) {
            this.data.set(DATA_SELECTED, id);
            this.data.set(DATA_PROGRESS, 0);
            this.data.set(DATA_CYCLE, images.get(id).image().cycleTicks());
        }
        return true;
    }

    private static SwitchBlockEntity findSwitch(Level level, BlockPos origin) {
        HallParts parts = HallFinder.find(level, origin);
        if (!parts.hasSwitch()) {
            return null;
        }
        BlockPos primary = Collections.min(parts.switches);
        return level.getBlockEntity(primary) instanceof SwitchBlockEntity switchEntity ? switchEntity : null;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.level().getBlockState(this.pos).is(ModBlocks.INTERFACE.get())
                && player.distanceToSqr(this.pos.getX() + 0.5D, this.pos.getY() + 0.5D, this.pos.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
