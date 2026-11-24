package dev.daan.hyperscale.network;

import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallParts;
import dev.daan.hyperscale.hall.HallStorage;
import dev.daan.hyperscale.hall.StackCount;
import dev.daan.hyperscale.item.DriveContents;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class ModNetwork {
    private ModNetwork() {
    }

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(
                InterfaceActionPayload.TYPE,
                InterfaceActionPayload.STREAM_CODEC,
                ModNetwork::handleInterfaceAction
        );
        registrar.playToServer(
                PortFilterPayload.TYPE,
                PortFilterPayload.STREAM_CODEC,
                ModNetwork::handlePortFilter
        );
        registrar.playToServer(
                DriveBayActionPayload.TYPE,
                DriveBayActionPayload.STREAM_CODEC,
                ModNetwork::handleDriveBayAction
        );
    }

    private static void handleDriveBayAction(DriveBayActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            BlockPos pos = payload.bayPos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.DRIVE_BAY.get())) {
                return;
            }
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (!(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
                return;
            }
            if (payload.actionId() == DriveBayActionPayload.ACTION_CYCLE_RAID) {
                bay.cycleRaidMode();
            } else if (payload.actionId() == DriveBayActionPayload.ACTION_SALVAGE_FAILED) {
                salvageFailedDrives(level, pos, bay, player);
            }
        });
    }

    private static void salvageFailedDrives(Level level, BlockPos bayPos, DriveBayBlockEntity bay, ServerPlayer player) {
        HallParts parts = HallFinder.find(level, bayPos);
        HallStorage storage = HallStorage.scan(level, parts);
        int totalRecovered = 0;
        int totalLost = 0;
        boolean changed = false;

        for (int slot = 0; slot < bay.items().getSlots(); slot++) {
            ItemStack stack = bay.items().getStackInSlot(slot);
            if (!(stack.getItem() instanceof StorageDriveItem drive) || !HardwareHealth.isFailed(stack)) {
                continue;
            }
            DriveContents contents = StorageDriveItem.getContents(stack);
            double recoveryRate = drive.spec().driveType().contains("SSD") ? 0.90D : 0.80D;
            for (StackCount entry : contents.items()) {
                int recovered = (int) Math.floor(entry.count() * recoveryRate);
                int lost = entry.count() - recovered;
                totalLost += lost;
                if (recovered > 0) {
                    int movedToStorage = storage.insert(entry.item(), recovered);
                    int remainder = recovered - movedToStorage;
                    totalRecovered += recovered;
                    if (remainder > 0) {
                        Item item = BuiltInRegistries.ITEM.get(entry.item());
                        if (item != Items.AIR) {
                            int left = remainder;
                            while (left > 0) {
                                int chunk = Math.min(left, item.getDefaultMaxStackSize());
                                player.getInventory().placeItemBackInInventory(new ItemStack(item, chunk));
                                left -= chunk;
                            }
                        }
                    }
                }
            }
            StorageDriveItem.setContents(stack, DriveContents.EMPTY);
            // Refurbish salvaged drive to 50% health so it can be reused or fully repaired
            HardwareHealth.set(stack, HardwareHealth.MAX_HEALTH / 2);
            changed = true;
        }
        if (changed) {
            storage.syncDirtyBays();
            bay.sync();
            player.displayClientMessage(
                    Component.translatable("message.hyperscale.drive.salvaged", totalRecovered, totalLost),
                    true
            );
        }
    }

    private static void handlePortFilter(PortFilterPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            BlockPos pos = payload.portPos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.PORT.get())) {
                return;
            }
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            if (level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                port.setFilterSlot(payload.slot(), payload.filterItem());
            }
        });
    }

    private static void handleInterfaceAction(InterfaceActionPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            Level level = player.level();
            BlockPos pos = payload.interfacePos();
            if (!level.isLoaded(pos) || !level.getBlockState(pos).is(ModBlocks.INTERFACE.get())) {
                return;
            }
            if (player.distanceToSqr(pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D) > 64.0D) {
                return;
            }
            HallParts parts = HallFinder.find(level, pos);
            if (!parts.hasSwitch()) {
                return;
            }
            if (!(level.getBlockEntity(parts.switches.get(0)) instanceof SwitchBlockEntity switchEntity)) {
                return;
            }
            int amount = Math.max(1, Math.min(64, payload.amount()));
            String key = payload.imageKey();
            switch (payload.actionId()) {
                case InterfaceActionPayload.ACTION_DEPLOY -> switchEntity.deployOrScaleUp(key, amount);
                case InterfaceActionPayload.ACTION_SCALE_UP -> switchEntity.scaleDeployment(key, amount);
                case InterfaceActionPayload.ACTION_SCALE_DOWN -> switchEntity.scaleDeployment(key, -amount);
                case InterfaceActionPayload.ACTION_TOGGLE_PAUSE -> switchEntity.togglePause(key);
                case InterfaceActionPayload.ACTION_REMOVE -> switchEntity.removeDeployment(key);
                case InterfaceActionPayload.ACTION_EXTRACT_STORAGE -> extractFromStorage(level, parts, switchEntity, player, key, amount);
                case InterfaceActionPayload.ACTION_CYCLE_PROFILE -> switchEntity.cycleProfile(key);
                default -> {
                }
            }
        });
    }

    private static void extractFromStorage(
            Level level,
            HallParts parts,
            SwitchBlockEntity switchEntity,
            ServerPlayer player,
            String itemIdStr,
            int requested
    ) {
        ResourceLocation itemId = ResourceLocation.tryParse(itemIdStr);
        if (itemId == null) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == Items.AIR) {
            return;
        }
        int toTake = Math.min(requested, item.getDefaultMaxStackSize());
        HallStorage storage = HallStorage.scan(level, parts);
        int extracted = storage.extract(itemId, toTake);
        if (extracted > 0) {
            player.getInventory().placeItemBackInInventory(new ItemStack(item, extracted));
            storage.syncDirtyBays();
            switchEntity.sync();
        }
    }
}
