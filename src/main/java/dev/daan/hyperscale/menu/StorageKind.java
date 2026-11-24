package dev.daan.hyperscale.menu;

import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModMenus;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

public enum StorageKind {
    PORT(1, "block.hyperscale.port"),
    RACK(4, "block.hyperscale.server_rack"),
    DRIVE(6, "block.hyperscale.drive_bay"),
    REGISTRY(9, "block.hyperscale.image_registry"),
    SWITCH(6, "gui.hyperscale.switch.registry_title"),
    PROBE(9, "block.hyperscale.recorder_input");

    private final int slots;
    private final String titleKey;

    StorageKind(int slots, String titleKey) {
        this.slots = slots;
        this.titleKey = titleKey;
    }

    public int slots() {
        return this.slots;
    }

    public String titleKey() {
        return this.titleKey;
    }

    public MenuType<StorageMenu> menu() {
        return switch (this) {
            case PORT -> ModMenus.PORT.get();
            case RACK -> ModMenus.RACK.get();
            case DRIVE -> ModMenus.DRIVE_BAY.get();
            case REGISTRY -> ModMenus.IMAGE_REGISTRY.get();
            case SWITCH -> ModMenus.SWITCH.get();
            case PROBE -> ModMenus.PROBE.get();
        };
    }

    public boolean matches(BlockState state) {
        return switch (this) {
            case PORT -> state.is(ModBlocks.PORT.get());
            case RACK -> state.is(ModBlocks.SERVER_RACK.get());
            case DRIVE -> state.is(ModBlocks.DRIVE_BAY.get());
            case REGISTRY -> state.is(ModBlocks.IMAGE_REGISTRY.get());
            case SWITCH -> state.is(ModBlocks.SWITCH.get());
            case PROBE -> state.is(ModBlocks.RECORDER_INPUT.get()) || state.is(ModBlocks.RECORDER_OUTPUT.get());
        };
    }
}
