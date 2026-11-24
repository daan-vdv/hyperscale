package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.menu.InterfaceMenu;
import dev.daan.hyperscale.menu.LithographyMenu;
import dev.daan.hyperscale.menu.RecorderMenu;
import dev.daan.hyperscale.menu.StorageKind;
import dev.daan.hyperscale.menu.StorageMenu;
import dev.daan.hyperscale.menu.WaferPressMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Hyperscale.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<InterfaceMenu>> INTERFACE = MENUS.register("interface",
            () -> IMenuTypeExtension.create(InterfaceMenu::fromBuffer));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> PORT = MENUS.register("port",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.PORT)));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> RACK = MENUS.register("rack",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.RACK)));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> DRIVE_BAY = MENUS.register("drive_bay",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.DRIVE)));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> IMAGE_REGISTRY = MENUS.register("image_registry",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.REGISTRY)));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> SWITCH = MENUS.register("switch",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.SWITCH)));
    public static final DeferredHolder<MenuType<?>, MenuType<StorageMenu>> PROBE = MENUS.register("probe",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    StorageMenu.fromBuffer(containerId, inventory, buffer, StorageKind.PROBE)));
    public static final DeferredHolder<MenuType<?>, MenuType<RecorderMenu>> RECORDER = MENUS.register("recorder",
            () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                    RecorderMenu.fromBuffer(containerId, inventory, buffer)));
    public static final DeferredHolder<MenuType<?>, MenuType<WaferPressMenu>> WAFER_PRESS = MENUS.register("wafer_press",
            () -> IMenuTypeExtension.create(WaferPressMenu::fromBuffer));
    public static final DeferredHolder<MenuType<?>, MenuType<LithographyMenu>> LITHOGRAPHY = MENUS.register("lithography",
            () -> IMenuTypeExtension.create(LithographyMenu::fromBuffer));

    private ModMenus() {
    }
}
