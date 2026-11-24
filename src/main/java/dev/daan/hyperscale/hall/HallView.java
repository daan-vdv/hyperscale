package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public record HallView(
        boolean hasSwitch,
        HallCapacity capacity,
        int usedRamGb,
        int usedVcpu,
        int totalFePerTick,
        int computeFePerTick,
        int coolingFePerTick,
        double pue,
        int energyStored,
        int maxEnergyStored,
        int usedStorageItems,
        int totalStorageCapacity,
        int storageIoPerTick,
        double hallTempC,
        int heatWatts,
        int coolingWatts,
        int noiseDb,
        boolean thermalShutdown,
        boolean liquidCooled,
        int degradedParts,
        int failedParts,
        List<RegistryRow> registry,
        List<ContainerRow> containers,
        List<StackCount> storedItems,
        List<String> missingParts
) {
    public static final HallView EMPTY = new HallView(
            false, HallCapacity.EMPTY, 0, 0, 0, 0, 0, 1.00D, 0, 0, 0, 0, 0,
            HallThermal.AMBIENT_TEMP_C, 0, HallThermal.PASSIVE_COOLING_W, 24, false, false, 0, 0,
            List.of(), List.of(), List.of(), List.of()
    );

    public record RegistryRow(
            String key,
            ProcessImage image,
            ItemStack icon,
            Component name,
            int ramPerReplica,
            int vcpuPerReplica,
            int deployedReplicas
    ) {
    }

    public record ContainerRow(
            String key,
            ProcessImage image,
            ItemStack icon,
            Component name,
            int replicas,
            ExecutionProfile profile,
            int ramUsed,
            int vcpuUsed,
            DeploymentStatus status,
            boolean paused,
            double progress,
            int cycleTicks,
            int fePerTick
    ) {
    }

    public static HallView inspect(Level level, BlockPos origin) {
        HallParts parts = HallFinder.find(level, origin);
        if (!parts.hasSwitch()) {
            return EMPTY;
        }

        BlockPos primary = parts.switches.get(0);
        SwitchBlockEntity switchEntity = level.getBlockEntity(primary) instanceof SwitchBlockEntity found ? found : null;
        HallCapacity capacity = HallCapacity.compute(level, parts);
        HallStorage storage = HallStorage.scan(level, parts);
        Map<String, ProcessImage> indexed = HallRunner.indexImages(level, parts);

        List<RegistryRow> registryRows = new ArrayList<>();
        for (Map.Entry<String, ProcessImage> entry : indexed.entrySet()) {
            String key = entry.getKey();
            ProcessImage img = entry.getValue();
            HallDeployment existing = switchEntity == null ? null : switchEntity.findDeployment(key);
            int deployed = existing == null ? 0 : existing.replicas();
            registryRows.add(new RegistryRow(
                    key,
                    img,
                    iconFor(img),
                    nameFor(img),
                    ImageResources.ramGbPerReplica(img),
                    ImageResources.vcpuPerReplica(img),
                    deployed
            ));
        }

        List<ContainerRow> containerRows = new ArrayList<>();
        if (switchEntity != null) {
            for (HallDeployment deployment : switchEntity.deployments()) {
                ProcessImage img = indexed.get(deployment.imageKey());
                int ramUsed = img == null ? 0 : ImageResources.ramGbPerReplica(img) * deployment.replicas();
                int vcpuUsed = img == null ? 0 : ImageResources.vcpuPerReplica(img) * deployment.replicas();
                containerRows.add(new ContainerRow(
                        deployment.imageKey(),
                        img,
                        img == null ? new ItemStack(Items.BARRIER) : iconFor(img),
                        img == null ? Component.translatable("gui.hyperscale.orchestrator.missing_image") : nameFor(img),
                        deployment.replicas(),
                        deployment.profile(),
                        ramUsed,
                        vcpuUsed,
                        deployment.status(),
                        deployment.paused(),
                        deployment.progress(),
                        img == null ? 1 : img.cycleTicks(),
                        deployment.lastFePerTick()
                ));
            }
        }

        List<String> missing = new ArrayList<>();
        if (parts.racks.isEmpty()) {
            missing.add(Component.translatable("block.hyperscale.server_rack").getString());
        } else if (capacity.readyRacks() == 0) {
            missing.add(Component.translatable("gui.hyperscale.orchestrator.no_ready_rack").getString());
        }
        if (parts.drives.isEmpty()) {
            missing.add(Component.translatable("block.hyperscale.drive_bay").getString());
        } else if (storage.totalCapacity() == 0) {
            missing.add(Component.translatable("gui.hyperscale.orchestrator.no_drives").getString());
        }

        int totalEnergyStored = switchEntity == null ? 0 : switchEntity.energyStored();
        int totalMaxEnergy = switchEntity == null ? SwitchBlockEntity.ENERGY_CAPACITY : switchEntity.maxEnergyStored();
        for (BlockPos genPos : parts.generators) {
            if (level.isLoaded(genPos) && level.getBlockEntity(genPos) instanceof BackupGeneratorBlockEntity gen) {
                totalEnergyStored = (int) Math.min(Integer.MAX_VALUE, (long) totalEnergyStored + gen.energyStored());
                totalMaxEnergy = (int) Math.min(Integer.MAX_VALUE, (long) totalMaxEnergy + gen.maxEnergyStored());
            }
        }

        int degraded = 0;
        int failed = 0;
        for (BlockPos rackPos : parts.racks) {
            if (level.isLoaded(rackPos) && level.getBlockEntity(rackPos) instanceof ServerRackBlockEntity rack) {
                if (rack.hasFailedComponent()) failed++;
                if (rack.hasDegradedComponent()) degraded++;
            }
        }
        for (BlockPos drivePos : parts.drives) {
            if (level.isLoaded(drivePos) && level.getBlockEntity(drivePos) instanceof DriveBayBlockEntity bay) {
                failed += bay.failedDriveCount();
                degraded += bay.degradedDriveCount();
            }
        }

        return new HallView(
                true,
                capacity,
                switchEntity == null ? 0 : switchEntity.usedRamGb(),
                switchEntity == null ? 0 : switchEntity.usedVcpu(),
                switchEntity == null ? 0 : switchEntity.lastTotalFePerTick(),
                switchEntity == null ? 0 : switchEntity.lastComputeFePerTick(),
                switchEntity == null ? 0 : switchEntity.lastCoolingFePerTick(),
                switchEntity == null ? 1.00D : switchEntity.pue(),
                totalEnergyStored,
                totalMaxEnergy,
                storage.usedItems(),
                storage.totalCapacity(),
                storage.totalIoPerTick(),
                switchEntity == null ? HallThermal.AMBIENT_TEMP_C : switchEntity.hallTempC(),
                switchEntity == null ? 0 : switchEntity.heatWatts(),
                switchEntity == null ? HallThermal.PASSIVE_COOLING_W : switchEntity.coolingWatts(),
                switchEntity == null ? 24 : switchEntity.noiseDb(),
                switchEntity != null && switchEntity.thermalShutdown(),
                switchEntity != null && switchEntity.liquidCooled(),
                degraded,
                failed,
                List.copyOf(registryRows),
                List.copyOf(containerRows),
                storage.aggregatedItems(),
                List.copyOf(missing)
        );
    }

    private static ItemStack iconFor(ProcessImage image) {
        if (!image.outputs().isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(image.outputs().get(0).item());
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        if (!image.inputs().isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(image.inputs().get(0).item());
            if (item != Items.AIR) {
                return new ItemStack(item);
            }
        }
        return new ItemStack(Items.PAPER);
    }

    private static Component nameFor(ProcessImage image) {
        if (!image.outputs().isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(image.outputs().get(0).item());
            if (item != Items.AIR) {
                return Component.translatable(item.getDescriptionId());
            }
        }
        if (!image.inputs().isEmpty()) {
            Item item = BuiltInRegistries.ITEM.get(image.inputs().get(0).item());
            if (item != Items.AIR) {
                return Component.translatable(item.getDescriptionId());
            }
        }
        return Component.translatable("item.hyperscale.image");
    }
}
