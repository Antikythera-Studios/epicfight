package guivnf.epicfight.neoforged.client;

import guivnf.epicfight.EpicFight;
import guivnf.epicfight.client.core.EpicFightClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(value = EpicFight.MODID, dist = Dist.CLIENT)
public final class EpicFightNeoForgedClient {
    public EpicFightNeoForgedClient(final IEventBus modEventBus) {
        modEventBus.addListener((final FMLClientSetupEvent event) -> EpicFightClient.onModInitialized());
    }
}
