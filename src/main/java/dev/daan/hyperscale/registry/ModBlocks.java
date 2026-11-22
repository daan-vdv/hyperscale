package dev.daan.hyperscale.registry;

import dev.daan.hyperscale.Hyperscale;
import dev.daan.hyperscale.block.DriveBayBlock;
import dev.daan.hyperscale.block.InterfaceBlock;
import dev.daan.hyperscale.block.PortBlock;
import dev.daan.hyperscale.block.ProbeBlock;
import dev.daan.hyperscale.block.ProbeMode;
import dev.daan.hyperscale.block.RecorderBlock;
import dev.daan.hyperscale.block.ServerRackBlock;
import dev.daan.hyperscale.block.SwitchBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Hyperscale.MOD_ID);

    public static final DeferredBlock<InterfaceBlock> INTERFACE = BLOCKS.register("interface",
            () -> new InterfaceBlock(machine(MapColor.COLOR_CYAN).noOcclusion()));
    public static final DeferredBlock<PortBlock> PORT = BLOCKS.register("port",
            () -> new PortBlock(machine(MapColor.COLOR_GREEN)));
    public static final DeferredBlock<SwitchBlock> SWITCH = BLOCKS.register("switch",
            () -> new SwitchBlock(machine(MapColor.COLOR_YELLOW)));
    public static final DeferredBlock<ServerRackBlock> SERVER_RACK = BLOCKS.register("server_rack",
            () -> new ServerRackBlock(machine(MapColor.COLOR_BLACK).noOcclusion()));
    public static final DeferredBlock<DriveBayBlock> DRIVE_BAY = BLOCKS.register("drive_bay",
            () -> new DriveBayBlock(machine(MapColor.COLOR_GRAY).noOcclusion()));
    public static final DeferredBlock<RecorderBlock> RECORDER = BLOCKS.register("recorder",
            () -> new RecorderBlock(machine(MapColor.COLOR_PURPLE)));
    public static final DeferredBlock<ProbeBlock> RECORDER_INPUT = BLOCKS.register("recorder_input",
            () -> new ProbeBlock(machine(MapColor.COLOR_LIGHT_GREEN), ProbeMode.INPUT));
    public static final DeferredBlock<ProbeBlock> RECORDER_OUTPUT = BLOCKS.register("recorder_output",
            () -> new ProbeBlock(machine(MapColor.COLOR_ORANGE), ProbeMode.OUTPUT));

    private ModBlocks() {
    }

    private static BlockBehaviour.Properties machine(MapColor color) {
        return BlockBehaviour.Properties.of()
                .mapColor(color)
                .strength(1.5F, 6.0F)
                .sound(SoundType.METAL);
    }
}
