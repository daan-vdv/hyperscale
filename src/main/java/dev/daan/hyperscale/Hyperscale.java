package dev.daan.hyperscale;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import dev.daan.hyperscale.registry.ModBlockEntities;
import dev.daan.hyperscale.registry.ModBlocks;
import dev.daan.hyperscale.registry.ModCreativeTabs;
import dev.daan.hyperscale.registry.ModDataComponents;
import dev.daan.hyperscale.registry.ModItems;
import dev.daan.hyperscale.registry.ModMenus;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(Hyperscale.MOD_ID)
public class Hyperscale {
    public static final String MOD_ID = "hyperscale";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Hyperscale(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        ModDataComponents.COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.TABS.register(modEventBus);
        modEventBus.addListener(ModBlockEntities::registerCapabilities);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Hyperscale hall loaded");
    }
}
