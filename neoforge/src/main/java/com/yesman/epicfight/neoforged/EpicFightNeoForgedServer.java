package com.yesman.epicfight.neoforged;

import com.yesman.epicfight.EpicFight;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;

@Mod(value = EpicFight.MODID, dist = Dist.DEDICATED_SERVER)
public final class EpicFightNeoForgedServer {
    public EpicFightNeoForgedServer(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::doServerStuff);
    }

    private void doServerStuff(final FMLDedicatedServerSetupEvent event) {
        EpicFight.onModInitializedInDedicatedServer();
    }
}
