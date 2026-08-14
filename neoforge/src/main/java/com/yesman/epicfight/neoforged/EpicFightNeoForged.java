package com.yesman.epicfight.neoforged;

import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.neoforged.networking.NeoForgedNetworkingHelper;
import com.yesman.epicfight.platform.ModPlatformProvider;
import com.yesman.epicfight.platform.neoforged.NeoForgedModPlatform;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@Mod(EpicFight.MODID)
public class EpicFightNeoForged {
    public EpicFightNeoForged(IEventBus modEventBus, ModContainer modContainer) {
        NeoForgedModPlatform neoForgedModPlatform = (NeoForgedModPlatform) ModPlatformProvider.get();
        EpicFight.onModConstructed();
        EpicFight.registerDataBlockEnumClasses();

        // Register mod bus events
		modEventBus.addListener(this::doCommonStuff);
        modEventBus.addListener(NeoForgedNetworkingHelper::register);
        neoForgedModPlatform.addListeners(modEventBus);

        // Register game bus events
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
	}

	private void doCommonStuff(final FMLCommonSetupEvent event) {
        EpicFight.onModInitialized();
    }

    private void registerCommands(final RegisterCommandsEvent event) {
        EpicFight.onRegisterCommands(event.getDispatcher());
    }
}