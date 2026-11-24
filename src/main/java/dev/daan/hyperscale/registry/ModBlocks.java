package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.block.BackupGeneratorBlock;
import dev.daan.hyperscale.block.CableBlock;
import dev.daan.hyperscale.block.DecorativeFacingBlock;
import dev.daan.hyperscale.block.DriveBayBlock;
import dev.daan.hyperscale.block.FluidPortBlock;
import dev.daan.hyperscale.block.FluidProbeBlock;
import dev.daan.hyperscale.block.ImageRegistryBlock;
import dev.daan.hyperscale.block.InterfaceBlock;
import dev.daan.hyperscale.block.LithographyEtcherBlock;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.PowerPortBlock;
import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.block.RecorderBlock;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.block.SwitchBlock;
import dev.daan.hyperscale.block.WaferPressBlock;
import dev.daan.hyperscale.block.WatercoolerBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Hyperscale.MOD_ID);

    public static final DeferredBlock<InterfaceBlock> INTERFACE = BLOCKS.register("interface",
            () -> new InterfaceBlock(machine(MapColor.COLOR_CYAN).noOcclusion()));
    public static final DeferredBlock<PortBlock> PORT = BLOCKS.register("port",
            () -> new PortBlock(machine(MapColor.COLOR_GREEN).noOcclusion()));
    public static final DeferredBlock<FluidPortBlock> FLUID_PORT = BLOCKS.register("fluid_port",
            () -> new FluidPortBlock(machine(MapColor.COLOR_LIGHT_BLUE).noOcclusion()));
    public static final DeferredBlock<SwitchBlock> SWITCH = BLOCKS.register("switch",
            () -> new SwitchBlock(machine(MapColor.COLOR_YELLOW).noOcclusion()));
    public static final DeferredBlock<ServerRackBlock> SERVER_RACK = BLOCKS.register("server_rack",
            () -> new ServerRackBlock(machine(MapColor.COLOR_BLACK).noOcclusion()));
    public static final DeferredBlock<DriveBayBlock> DRIVE_BAY = BLOCKS.register("drive_bay",
            () -> new DriveBayBlock(machine(MapColor.COLOR_GRAY).noOcclusion()));
    public static final DeferredBlock<ImageRegistryBlock> IMAGE_REGISTRY = BLOCKS.register("image_registry",
            () -> new ImageRegistryBlock(machine(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<RecorderBlock> RECORDER = BLOCKS.register("recorder",
            () -> new RecorderBlock(machine(MapColor.COLOR_PURPLE).pushReaction(PushReaction.BLOCK)));
    public static final DeferredBlock<ProbeBlock> RECORDER_INPUT = BLOCKS.register("recorder_input",
            () -> new ProbeBlock(machine(MapColor.COLOR_LIGHT_GREEN), ProbeMode.INPUT));
    public static final DeferredBlock<ProbeBlock> RECORDER_OUTPUT = BLOCKS.register("recorder_output",
            () -> new ProbeBlock(machine(MapColor.COLOR_ORANGE), ProbeMode.OUTPUT));
    public static final DeferredBlock<FluidProbeBlock> RECORDER_FLUID_INPUT = BLOCKS.register("recorder_fluid_input",
            () -> new FluidProbeBlock(machine(MapColor.COLOR_LIGHT_BLUE), ProbeMode.INPUT));
    public static final DeferredBlock<FluidProbeBlock> RECORDER_FLUID_OUTPUT = BLOCKS.register("recorder_fluid_output",
            () -> new FluidProbeBlock(machine(MapColor.COLOR_CYAN), ProbeMode.OUTPUT));
    public static final DeferredBlock<PowerPortBlock> RECORDER_POWER = BLOCKS.register("recorder_power",
            () -> new PowerPortBlock(machine(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<TransparentBlock> RECORDER_CASING = BLOCKS.register("recorder_casing",
            () -> new TransparentBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(1.5F, 6.0F)
                    .sound(SoundType.GLASS)
                    .noOcclusion()
                    .pushReaction(PushReaction.BLOCK)
                    .isValidSpawn((state, level, pos, entityType) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos) -> false)));
    public static final DeferredBlock<BackupGeneratorBlock> BACKUP_GENERATOR = BLOCKS.register("backup_generator",
            () -> new BackupGeneratorBlock(machine(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<CableBlock> CABLE = BLOCKS.register("cable",
            () -> new CableBlock(machine(MapColor.COLOR_BLACK)));
    public static final DeferredBlock<DecorativeFacingBlock> SERVER_FAN = BLOCKS.register("server_fan",
            () -> new DecorativeFacingBlock(machine(MapColor.COLOR_GRAY)));
    public static final DeferredBlock<WatercoolerBlock> WATERCOOLER_CONTROLLER = BLOCKS.register("watercooler_controller",
            () -> new WatercoolerBlock(machine(MapColor.COLOR_LIGHT_BLUE)));
    public static final DeferredBlock<WaferPressBlock> WAFER_PRESS = BLOCKS.register("wafer_press",
            () -> new WaferPressBlock(machine(MapColor.COLOR_CYAN)));
    public static final DeferredBlock<LithographyEtcherBlock> LITHOGRAPHY_ETCHER = BLOCKS.register("lithography_etcher",
            () -> new LithographyEtcherBlock(machine(MapColor.COLOR_PURPLE)));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties machine(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL);
    }
}
