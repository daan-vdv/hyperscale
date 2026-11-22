package dev.daan.hyperscale;

import dev.daan.hyperscale.client.InterfaceScreen;
import dev.daan.hyperscale.client.RecorderScreen;
import dev.daan.hyperscale.client.StorageScreen;
import dev.daan.hyperscale.registry.ModMenus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = Hyperscale.MOD_ID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Hyperscale.MOD_ID, value = Dist.CLIENT)
public class HyperscaleClient {
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenus.INTERFACE.get(), InterfaceScreen::new);
        event.register(ModMenus.PORT.get(), StorageScreen::new);
        event.register(ModMenus.RACK.get(), StorageScreen::new);
        event.register(ModMenus.DRIVE_BAY.get(), StorageScreen::new);
        event.register(ModMenus.PROBE.get(), StorageScreen::new);
        event.register(ModMenus.RECORDER.get(), RecorderScreen::new);
    }
}
