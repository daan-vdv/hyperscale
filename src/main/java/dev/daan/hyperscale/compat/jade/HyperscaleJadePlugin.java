package dev.daan.hyperscale.compat.jade;

import java.util.Locale;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.block.BackupGeneratorBlock;
import dev.daan.hyperscale.block.CableBlock;
import dev.daan.hyperscale.block.DriveBayBlock;
import dev.daan.hyperscale.block.ImageRegistryBlock;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PortMode;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.block.SwitchBlock;
import dev.daan.hyperscale.block.WatercoolerBlock;
import dev.daan.hyperscale.blockentity.BackupGeneratorBlockEntity;
import dev.daan.hyperscale.blockentity.DriveBayBlockEntity;
import dev.daan.hyperscale.blockentity.ImageRegistryBlockEntity;
import dev.daan.hyperscale.blockentity.PortBlockEntity;
import dev.daan.hyperscale.blockentity.ServerRackBlockEntity;
import dev.daan.hyperscale.blockentity.SwitchBlockEntity;
import dev.daan.hyperscale.blockentity.WatercoolerBlockEntity;
import dev.daan.hyperscale.hall.HallCapacity;
import dev.daan.hyperscale.hall.HallFinder;
import dev.daan.hyperscale.hall.HallParts;
import dev.daan.hyperscale.item.HardwareHealth;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

@WailaPlugin
public class HyperscaleJadePlugin implements IWailaPlugin {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(Hyperscale.MOD_ID, "datacenter_info");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        IBlockComponentProvider provider = new IBlockComponentProvider() {
            @Override
            public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
                Level level = accessor.getLevel();
                BlockPos pos = accessor.getPosition();
                var state = accessor.getBlockState();

                if (state.getBlock() instanceof SwitchBlock && level.getBlockEntity(pos) instanceof SwitchBlockEntity sw) {
                    HallParts parts = HallFinder.find(level, pos);
                    HallCapacity cap = HallCapacity.compute(level, parts);
                    tooltip.add(Component.literal(String.format(
                            Locale.ROOT,
                            "Cluster: %d Racks · %d/%d GB · %d/%d vCPU",
                            cap.readyRacks(), sw.usedRamGb(), cap.totalRamGb(), sw.usedVcpu(), cap.totalVcpu()
                    )).withStyle(ChatFormatting.AQUA));
                    int temp = (int) Math.round(sw.hallTempC());
                    ChatFormatting tCol = sw.thermalShutdown() || temp >= 90 ? ChatFormatting.RED : (temp >= 75 ? ChatFormatting.GOLD : ChatFormatting.GREEN);
                    tooltip.add(Component.literal(String.format(
                            Locale.ROOT,
                            "Thermal: %d°C (%dW/%dW) · PUE %.2f",
                            temp, sw.heatWatts(), sw.coolingWatts(), sw.pue()
                    )).withStyle(tCol));
                    tooltip.add(Component.literal("Images: " + sw.imageCardCount() + " · Containers: " + sw.deployments().size())
                            .withStyle(ChatFormatting.GRAY));
                } else if (state.getBlock() instanceof ServerRackBlock) {
                    BlockPos base = ServerRackBlock.basePos(pos, state);
                    if (level.getBlockEntity(base) instanceof ServerRackBlockEntity rack) {
                        addHardwareLine(tooltip, "CPU", rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_CPU));
                        addHardwareLine(tooltip, "RAM", rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_RAM));
                        ItemStack gpu = rack.items().getStackInSlot(ServerRackBlockEntity.SLOT_GPU);
                        if (!gpu.isEmpty()) {
                            addHardwareLine(tooltip, "GPU", gpu);
                        }
                    }
                } else if (state.getBlock() instanceof ImageRegistryBlock && level.getBlockEntity(pos) instanceof ImageRegistryBlockEntity reg) {
                    tooltip.add(Component.literal("Stored Images: " + reg.imageCount() + " / " + ImageRegistryBlockEntity.SLOTS)
                            .withStyle(ChatFormatting.AQUA));
                } else if (state.getBlock() instanceof DriveBayBlock && level.getBlockEntity(pos) instanceof DriveBayBlockEntity bay) {
                    tooltip.add(Component.literal("Mode: " + bay.raidMode().label() + " · Drives: " + bay.driveCount() + "/6")
                            .withStyle(ChatFormatting.AQUA));
                    tooltip.add(Component.literal("Stored: " + bay.usedItems() + " / " + bay.totalCapacity() + " items")
                            .withStyle(ChatFormatting.GRAY));
                    if (bay.failedDriveCount() > 0) {
                        tooltip.add(Component.literal("FAILED drives: " + bay.failedDriveCount()
                                + (bay.hasRaidFaultTolerance() ? " (RAID protected)" : " (Salvage needed!)"))
                                .withStyle(ChatFormatting.RED));
                    }
                } else if (state.getBlock() instanceof BackupGeneratorBlock) {
                    BlockPos primary = BackupGeneratorBlock.primaryPos(pos, state);
                    if (level.getBlockEntity(primary) instanceof BackupGeneratorBlockEntity gen) {
                        tooltip.add(Component.literal("UPS Buffer: " + gen.energyStored() + " / " + gen.maxEnergyStored() + " FE")
                                .withStyle(ChatFormatting.GOLD));
                    }
                } else if (state.getBlock() instanceof PortBlock && level.getBlockEntity(pos) instanceof PortBlockEntity port) {
                    PortMode mode = state.getValue(PortBlock.MODE);
                    if (mode == PortMode.OUTPUT && port.hasFilter()) {
                        StringBuilder sb = new StringBuilder("Whitelist: ");
                        var ids = port.filterItemIds();
                        for (int i = 0; i < ids.size(); i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(Component.translatable(BuiltInRegistries.ITEM.get(ids.get(i)).getDescriptionId()).getString());
                        }
                        tooltip.add(Component.literal(sb.toString()).withStyle(ChatFormatting.YELLOW));
                    }
                } else if (state.getBlock() instanceof CableBlock) {
                    boolean switched = HallFinder.isSwitchedCable(level, pos);
                    tooltip.add(Component.literal(switched ? "Switched Trunk (Data + FE Power)" : "Unswitched (Connect to Network Switch)")
                            .withStyle(switched ? ChatFormatting.AQUA : ChatFormatting.GRAY));
                } else if (state.getBlock() instanceof WatercoolerBlock && level.getBlockEntity(pos) instanceof WatercoolerBlockEntity cooler) {
                    int coolant = cooler.coolantAmount();
                    int savings = Math.min(75, WatercoolerBlockEntity.adjacentRadiatorFans(level, pos) * 25);
                    tooltip.add(Component.literal("Coolant: " + coolant + " / " + WatercoolerBlockEntity.CAPACITY_MB + " mB Water")
                            .withStyle(coolant > 0 ? ChatFormatting.AQUA : ChatFormatting.GOLD));
                    tooltip.add(Component.literal("Radiator Loop: -" + savings + "% water use (adjacent Server Fans)")
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            @Override
            public ResourceLocation getUid() {
                return UID;
            }
        };

        registration.registerBlockComponent(provider, SwitchBlock.class);
        registration.registerBlockComponent(provider, ServerRackBlock.class);
        registration.registerBlockComponent(provider, DriveBayBlock.class);
        registration.registerBlockComponent(provider, ImageRegistryBlock.class);
        registration.registerBlockComponent(provider, BackupGeneratorBlock.class);
        registration.registerBlockComponent(provider, PortBlock.class);
        registration.registerBlockComponent(provider, CableBlock.class);
        registration.registerBlockComponent(provider, WatercoolerBlock.class);
    }

    private static void addHardwareLine(ITooltip tooltip, String label, ItemStack stack) {
        if (stack.isEmpty()) {
            tooltip.add(Component.literal(label + ": Missing").withStyle(ChatFormatting.RED));
            return;
        }
        int pct = HardwareHealth.percent(stack);
        ChatFormatting col = pct <= 0 ? ChatFormatting.RED : (pct <= 20 ? ChatFormatting.GOLD : ChatFormatting.GRAY);
        tooltip.add(Component.literal(label + ": " + stack.getHoverName().getString() + " (" + pct + "%)").withStyle(col));
    }
}
