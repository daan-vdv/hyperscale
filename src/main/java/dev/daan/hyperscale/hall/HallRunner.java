package dev.daan.hyperscale.hall;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.daan.hyperscale.HyperscaleConfig;
import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.FluidPortBlockEntity;
import dev.daan.hyperscale.blockentity.ImageRegistryBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.item.CpuItem;
import dev.daan.hyperscale.item.GpuItem;
import dev.daan.hyperscale.item.HardwareHealth;
import dev.daan.hyperscale.item.RamItem;
import dev.daan.hyperscale.item.StorageDriveItem;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class HallRunner {
    private static final int MAX_CYCLES_PER_TICK = 64;

    private HallRunner() {
    }

    public static void tick(SwitchBlockEntity switchEntity) {
        Level level = switchEntity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }

        HallParts parts = switchEntity.hallParts(level);
        if (!parts.hasSwitch()) {
            return;
        }
        BlockPos primary = parts.switches.get(0);
        if (!primary.equals(switchEntity.getBlockPos())) {
            return;
        }

        Map<String, ProcessImage> registry = switchEntity.cachedImages(level, parts);
        HallCapacity capacity = switchEntity.cachedCapacity(level, parts);
        HallStorage storage = HallStorage.scan(level, parts);
        boolean thermalEnabled = HyperscaleConfig.ENABLE_THERMAL.get();
        HallThermal thermal = HallThermal.computeAndConsume(level, parts, switchEntity.hallTempC(), thermalEnabled);

        // 0. Pull backup power from connected 2x2x2 Backup Generators (UPS) into the Switch
        pullBackupGeneratorEnergy(level, parts, switchEntity);

        // 1. Ingest items from Input Ports into Drive Bay storage
        storage.ingestInputPorts(level, parts);

        int freeRam = capacity.totalRamGb();
        int freeVcpu = capacity.totalVcpu();
        int usedRam = 0;
        int usedVcpu = 0;
        int totalFeThisTick = 0;
        int computeFeThisTick = 0;
        int coolingFeThisTick = 0;
        int heatWattsThisTick = 0;
        boolean statusChanged = false;
        boolean anyRunning = false;

        double currentTemp = switchEntity.hallTempC();
        boolean shutdown = thermalEnabled && switchEntity.thermalShutdown();
        if (shutdown && currentTemp <= HallThermal.RECOVERY_TEMP_C) {
            shutdown = false;
        }

        // Cooling power draw (active if cooling blocks exist & hall is warm)
        int effectiveCoolingW = thermal.coolingWatts();
        if (thermalEnabled && thermal.coolingFePerTick() > 0 && currentTemp > HallThermal.AMBIENT_TEMP_C + 2.0D) {
            if (switchEntity.consumeEnergy(thermal.coolingFePerTick())) {
                coolingFeThisTick = thermal.coolingFePerTick();
                totalFeThisTick += coolingFeThisTick;
            } else {
                effectiveCoolingW = HallThermal.PASSIVE_COOLING_W;
            }
        }

        // Hyperscale PUE efficiency bonus: well-cooled halls with PUE <= 1.20 get -10% compute FE draw
        boolean pueBonus = !shutdown && currentTemp < 75.0D && switchEntity.lastComputeFePerTick() > 0 && switchEntity.pue() <= 1.20D;
        double pueDiscount = pueBonus ? 0.90D : 1.00D;

        double thermalSpeedFactor = thermalEnabled
                ? HallThermal.speedFactor(currentTemp, thermal.hasLiquidCooling(), shutdown)
                : 1.0D;

        List<HallDeployment> deployments = switchEntity.deployments();
        int activeCandidates = 0;
        for (HallDeployment d : deployments) {
            if (!d.paused() && d.replicas() > 0 && registry.containsKey(d.imageKey())) {
                activeCandidates++;
            }
        }
        int gpuFeShare = activeCandidates > 0 ? (int) Math.ceil((double) capacity.gpuActiveFePerTick() / activeCandidates) : 0;

        // 2. Run all active container deployments
        for (HallDeployment deployment : deployments) {
            DeploymentStatus prevStatus = deployment.status();
            ProcessImage image = registry.get(deployment.imageKey());
            if (image == null
                    || (image.inputs().isEmpty() && image.fluidInputs().isEmpty())
                    || (image.outputs().isEmpty() && image.fluidOutputs().isEmpty())) {
                deployment.setStatus(DeploymentStatus.MISSING_IMAGE);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }
            if (deployment.paused() || deployment.replicas() <= 0) {
                deployment.setStatus(DeploymentStatus.PAUSED);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }

            int ramNeed = ImageResources.ramGbPerReplica(image) * deployment.replicas();
            int vcpuNeed = ImageResources.vcpuPerReplica(image) * deployment.replicas();
            if (capacity.readyRacks() <= 0 || ramNeed > freeRam || vcpuNeed > freeVcpu) {
                deployment.setStatus(DeploymentStatus.INSUFFICIENT_RESOURCES);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }

            freeRam -= ramNeed;
            freeVcpu -= vcpuNeed;
            usedRam += ramNeed;
            usedVcpu += vcpuNeed;

            if (shutdown) {
                deployment.setStatus(DeploymentStatus.OVERHEATED);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }

            boolean needsItemStorage = !image.inputs().isEmpty() || !image.outputs().isEmpty();
            if ((needsItemStorage && !storage.hasDrives())
                    || !storage.canInsertAll(image.outputs())
                    || !canAcceptFluids(level, parts, image.fluidOutputs())) {
                deployment.setStatus(DeploymentStatus.OUTPUT_FULL);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }
            if (!storage.hasInputs(image) || !hasFluidInputs(level, parts, image.fluidInputs())) {
                deployment.setStatus(DeploymentStatus.WAITING_INPUT);
                deployment.setProgress(0.0D);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }

            ExecutionProfile profile = deployment.profile();
            // Overclock only engages full 1.5x speed when hall is cooled below 55°C
            boolean canOverclock = !thermalEnabled || currentTemp <= HallThermal.OVERCLOCK_MAX_TEMP_C;
            double profileSpeed = (profile == ExecutionProfile.OVERCLOCK && !canOverclock)
                    ? ExecutionProfile.BALANCED.speedMultiplier()
                    : profile.speedMultiplier();
            double profileEnergy = (profile == ExecutionProfile.OVERCLOCK && !canOverclock)
                    ? ExecutionProfile.BALANCED.energyMultiplier()
                    : profile.energyMultiplier();
            double profileHeat = (profile == ExecutionProfile.OVERCLOCK && !canOverclock)
                    ? ExecutionProfile.BALANCED.heatMultiplier()
                    : profile.heatMultiplier();

            double overhead = ImageResources.replicaOverheadMultiplier(deployment.replicas());
            double step = deployment.replicas() * capacity.speedMultiplier() * profileSpeed * thermalSpeedFactor;
            int recipeFe = energyCostForStep(image, step, capacity.energyEfficiency());
            int baseComputeFe = (int) Math.ceil(
                    deployment.replicas()
                            * HyperscaleConfig.BASE_REPLICA_FE_PER_TICK.get()
                            * capacity.energyEfficiency()
                            * overhead
                            * profileEnergy
            );
            int feThisTick = (int) Math.ceil(((recipeFe * overhead * profileEnergy) + baseComputeFe + gpuFeShare) * pueDiscount);

            if (feThisTick > 0 && !switchEntity.consumeEnergy(feThisTick)) {
                deployment.setStatus(DeploymentStatus.NO_POWER);
                deployment.setLastFePerTick(0);
                if (prevStatus != deployment.status()) {
                    statusChanged = true;
                }
                continue;
            }

            anyRunning = true;
            computeFeThisTick += feThisTick;
            totalFeThisTick += feThisTick;
            heatWattsThisTick += (int) Math.ceil((deployment.replicas() * 45.0D + feThisTick * 0.25D) * profileHeat * overhead);
            deployment.setStatus(DeploymentStatus.RUNNING);
            deployment.setLastFePerTick(feThisTick);

            double progress = deployment.progress() + step;
            int cycleTicks = Math.max(1, image.cycleTicks());
            int cycles = 0;
            while (progress >= cycleTicks && cycles < MAX_CYCLES_PER_TICK) {
                if (!storage.hasInputs(image) || !storage.canInsertAll(image.outputs())
                        || !hasFluidInputs(level, parts, image.fluidInputs())
                        || !canAcceptFluids(level, parts, image.fluidOutputs())) {
                    break;
                }
                for (StackCount in : image.inputs()) {
                    storage.extract(in.item(), in.count());
                }
                for (StackCount out : image.outputs()) {
                    storage.insert(out.item(), out.count());
                }
                consumeFluids(level, parts, image.fluidInputs());
                produceFluids(level, parts, image.fluidOutputs());
                progress -= cycleTicks;
                cycles++;
            }
            if (progress > cycleTicks) {
                progress = cycleTicks;
            }
            deployment.setProgress(progress);

            if (prevStatus != deployment.status()) {
                statusChanged = true;
            }
        }

        if (anyRunning && capacity.gpuActiveFePerTick() > 0) {
            heatWattsThisTick += (int) Math.ceil(capacity.gpuActiveFePerTick() * 1.5D);
        }

        // 3. Update Hall Temperature
        double nextTemp = thermalEnabled
                ? HallThermal.nextTemperature(currentTemp, heatWattsThisTick, effectiveCoolingW)
                : HallThermal.AMBIENT_TEMP_C;
        if (thermalEnabled && nextTemp >= HallThermal.SHUTDOWN_TEMP_C) {
            shutdown = true;
        }
        boolean tempChanged = Math.abs(nextTemp - switchEntity.hallTempC()) >= 0.5D
                || switchEntity.thermalShutdown() != shutdown
                || switchEntity.coolingWatts() != effectiveCoolingW
                || switchEntity.noiseDb() != thermal.noiseDb();
        switchEntity.setThermalState(nextTemp, heatWattsThisTick, effectiveCoolingW, thermal.noiseDb(), shutdown, thermal.hasLiquidCooling());

        // 4. Apply thermal-scaled hardware wear once per second while active
        if (anyRunning && level.getGameTime() % 20L == 0L) {
            applyHardwareWear(level, parts, nextTemp);
        }

        // 5. Export finished outputs from Drive Bay storage into Output Ports
        storage.exportOutputPorts(level, parts, registry, deployments);

        int usedStorage = storage.usedItems();
        int totalStorage = storage.totalCapacity();
        int storageIo = storage.totalIoPerTick();

        boolean telemetryChanged = switchEntity.usedRamGb() != usedRam
                || switchEntity.usedVcpu() != usedVcpu
                || switchEntity.lastTotalFePerTick() != totalFeThisTick
                || switchEntity.lastComputeFePerTick() != computeFeThisTick
                || switchEntity.lastCoolingFePerTick() != coolingFeThisTick
                || switchEntity.usedStorageItems() != usedStorage
                || switchEntity.totalStorageCapacity() != totalStorage
                || switchEntity.storageIoPerTick() != storageIo;
        switchEntity.setTelemetry(usedRam, usedVcpu, totalFeThisTick, computeFeThisTick, coolingFeThisTick, usedStorage, totalStorage, storageIo);

        if (storage.hasDirtyBays() && (statusChanged || level.getGameTime() % 10L == 0L)) {
            storage.syncDirtyBays();
        }
        if (statusChanged || telemetryChanged || (tempChanged && level.getGameTime() % 10L == 0L) || (anyRunning && level.getGameTime() % 10L == 0L)) {
            switchEntity.sync();
        }
    }

    public static int energyCostForStep(ProcessImage image, double step, double efficiency) {
        if (image == null || image.energyPerCycle() <= 0 || step <= 0.0D) {
            return 0;
        }
        double scaled = image.energyPerCycle() * HyperscaleConfig.HALL_ENERGY_MULTIPLIER.get() * efficiency;
        if (scaled <= 0.0D) {
            return 0;
        }
        return Math.max(1, (int) Math.ceil(scaled * step / Math.max(1, image.cycleTicks())));
    }

    public static List<HallImage> listImages(Level level, BlockPos origin) {
        HallParts parts = HallFinder.find(level, origin);
        return listImages(level, parts);
    }

    public static List<HallImage> listImages(Level level, HallParts parts) {
        List<HallImage> images = new ArrayList<>();
        // Primary: dedicated Image Registry blocks
        for (BlockPos pos : parts.registries) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ImageRegistryBlockEntity reg)) {
                continue;
            }
            for (int slot = 0; slot < reg.items().getSlots(); slot++) {
                ItemStack stack = reg.items().getStackInSlot(slot);
                if (!stack.is(ModItems.IMAGE.get())) {
                    continue;
                }
                ProcessImage image = stack.get(ModDataComponents.PROCESS_IMAGE.get());
                if (image != null) {
                    images.add(new HallImage(pos, slot, image));
                }
            }
        }
        // Backwards-compatible fallbacks: Switch / Drive Bay
        for (BlockPos pos : parts.switches) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof SwitchBlockEntity switchEntity)) {
                continue;
            }
            for (int slot = 0; slot < switchEntity.imageCards().getSlots(); slot++) {
                ItemStack stack = switchEntity.imageCards().getStackInSlot(slot);
                if (!stack.is(ModItems.IMAGE.get())) {
                    continue;
                }
                ProcessImage image = stack.get(ModDataComponents.PROCESS_IMAGE.get());
                if (image != null) {
                    images.add(new HallImage(pos, slot, image));
                }
            }
        }
        for (BlockPos pos : parts.drives) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
                continue;
            }
            for (int slot = 0; slot < bay.items().getSlots(); slot++) {
                ItemStack stack = bay.items().getStackInSlot(slot);
                if (!stack.is(ModItems.IMAGE.get())) {
                    continue;
                }
                ProcessImage image = stack.get(ModDataComponents.PROCESS_IMAGE.get());
                if (image != null) {
                    images.add(new HallImage(pos, slot, image));
                }
            }
        }
        return images;
    }

    public static Map<String, ProcessImage> indexImages(Level level, HallParts parts) {
        Map<String, ProcessImage> indexed = new LinkedHashMap<>();
        for (HallImage entry : listImages(level, parts)) {
            String key = ImageResources.keyOf(entry.image());
            indexed.putIfAbsent(key, entry.image());
        }
        return indexed;
    }

    private static void pullBackupGeneratorEnergy(Level level, HallParts parts, SwitchBlockEntity switchEntity) {
        if (parts.generators.isEmpty()) {
            return;
        }
        int lowThreshold = (int) (switchEntity.maxEnergyStored() * 0.80D);
        int highThreshold = (int) (switchEntity.maxEnergyStored() * 0.90D);
        int stored = switchEntity.energyStored();

        if (stored < lowThreshold) {
            // Discharge UPS banks into the Switch
            int deficit = lowThreshold - stored;
            for (BlockPos pos : parts.generators) {
                if (deficit <= 0) {
                    break;
                }
                if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BackupGeneratorBlockEntity gen)) {
                    continue;
                }
                int toTransfer = Math.min(deficit, BackupGeneratorBlockEntity.MAX_TRANSFER);
                int accepted = switchEntity.energyCapability().receiveEnergy(toTransfer, true);
                if (accepted > 0) {
                    int taken = gen.extractForHall(accepted);
                    if (taken > 0) {
                        switchEntity.energyCapability().receiveEnergy(taken, false);
                        deficit -= taken;
                        if (level.getGameTime() % 20L == 0L) {
                            gen.sync();
                        }
                    }
                }
            }
        } else if (stored > highThreshold) {
            // Charge UPS banks with surplus FE from the Switch
            int surplus = stored - highThreshold;
            for (BlockPos pos : parts.generators) {
                if (surplus <= 0) {
                    break;
                }
                if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof BackupGeneratorBlockEntity gen)) {
                    continue;
                }
                int toTransfer = Math.min(surplus, BackupGeneratorBlockEntity.MAX_TRANSFER);
                int canStore = gen.energyCapability().receiveEnergy(toTransfer, true);
                if (canStore > 0 && switchEntity.consumeEnergy(canStore)) {
                    gen.energyCapability().receiveEnergy(canStore, false);
                    surplus -= canStore;
                    if (level.getGameTime() % 20L == 0L) {
                        gen.sync();
                    }
                }
            }
        }
    }

    private static void applyHardwareWear(Level level, HallParts parts, double tempC) {
        double mult = HyperscaleConfig.WEAR_RATE_MULTIPLIER.get();
        if (mult <= 0.0D) {
            return;
        }
        double thermalMult = tempC < 55.0D ? 0.5D : (tempC > 80.0D ? 3.0D : 1.0D);
        double scaled = mult * thermalMult;

        for (BlockPos pos : parts.racks) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack) || !rack.isReady()) {
                continue;
            }
            boolean changed = false;
            ItemStack cpu = rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_CPU);
            if (cpu.getItem() instanceof CpuItem c) {
                int wear = Math.max(1, (int) Math.round(scaled * (c.spec().cores() >= 64 ? 1.0D : 2.0D)));
                changed |= HardwareHealth.damage(cpu, wear);
            }
            ItemStack ram = rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_RAM);
            if (ram.getItem() instanceof RamItem r) {
                boolean ecc = r.spec().generation().contains("ECC");
                int wear = Math.max(1, (int) Math.round(scaled * (ecc ? 1.0D : 2.0D)));
                changed |= HardwareHealth.damage(ram, wear);
            }
            ItemStack gpu = rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_GPU);
            if (gpu.getItem() instanceof GpuItem && !HardwareHealth.isFailed(gpu)) {
                int wear = Math.max(1, (int) Math.round(scaled * 2.5D));
                changed |= HardwareHealth.damage(gpu, wear);
            }
            if (changed) {
                rack.sync();
            }
        }

        for (BlockPos pos : parts.drives) {
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay)) {
                continue;
            }
            boolean changed = false;
            for (int slot = 0; slot < bay.items().getSlots(); slot++) {
                ItemStack stack = bay.items().getStackInSlot(slot);
                if (stack.getItem() instanceof StorageDriveItem drive && !HardwareHealth.isFailed(stack)) {
                    boolean nvmeOrSsd = drive.spec().driveType().contains("SSD");
                    int wear = Math.max(1, (int) Math.round(scaled * (nvmeOrSsd ? 1.0D : 1.5D)));
                    changed |= HardwareHealth.damage(stack, wear);
                }
            }
            if (changed) {
                bay.sync();
            }
        }
    }

    private static boolean hasFluidInputs(Level level, HallParts parts, List<FluidStackCount> needs) {
        if (needs.isEmpty()) {
            return true;
        }
        for (FluidStackCount need : needs) {
            Fluid fluid = BuiltInRegistries.FLUID.get(need.fluid());
            if (fluid == Fluids.EMPTY) {
                return false;
            }
            int available = 0;
            for (BlockPos pos : parts.fluidInputs) {
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidPortBlockEntity fp) {
                    FluidStack stored = fp.tank().getFluid();
                    if (!stored.isEmpty() && stored.getFluid() == fluid) {
                        available += stored.getAmount();
                    }
                }
            }
            if (available < need.amount()) {
                return false;
            }
        }
        return true;
    }

    private static boolean canAcceptFluids(Level level, HallParts parts, List<FluidStackCount> outputs) {
        if (outputs.isEmpty()) {
            return true;
        }
        for (FluidStackCount out : outputs) {
            Fluid fluid = BuiltInRegistries.FLUID.get(out.fluid());
            if (fluid == Fluids.EMPTY) {
                return false;
            }
            int left = out.amount();
            for (BlockPos pos : parts.fluidOutputs) {
                if (left <= 0) {
                    break;
                }
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidPortBlockEntity fp) {
                    left -= fp.tank().fill(new FluidStack(fluid, left), IFluidHandler.FluidAction.SIMULATE);
                }
            }
            if (left > 0) {
                return false;
            }
        }
        return true;
    }

    private static void consumeFluids(Level level, HallParts parts, List<FluidStackCount> needs) {
        for (FluidStackCount need : needs) {
            Fluid fluid = BuiltInRegistries.FLUID.get(need.fluid());
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            int left = need.amount();
            for (BlockPos pos : parts.fluidInputs) {
                if (left <= 0) {
                    break;
                }
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidPortBlockEntity fp) {
                    FluidStack drained = fp.tank().drain(new FluidStack(fluid, left), IFluidHandler.FluidAction.EXECUTE);
                    left -= drained.getAmount();
                }
            }
        }
    }

    private static void produceFluids(Level level, HallParts parts, List<FluidStackCount> outputs) {
        for (FluidStackCount out : outputs) {
            Fluid fluid = BuiltInRegistries.FLUID.get(out.fluid());
            if (fluid == Fluids.EMPTY) {
                continue;
            }
            int left = out.amount();
            for (BlockPos pos : parts.fluidOutputs) {
                if (left <= 0) {
                    break;
                }
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof FluidPortBlockEntity fp) {
                    left -= fp.tank().fill(new FluidStack(fluid, left), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    public static int countReadyRacks(Level level, HallParts parts) {
        int ready = 0;
        for (BlockPos pos : parts.racks) {
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof ServerRackBlockEntity rack && rack.isReady()) {
                ready++;
            }
        }
        return ready;
    }
}
