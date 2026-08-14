package com.yesman.epicfight.neoforged.client;

import com.yesman.epicfight.EpicFight;
import com.yesman.epicfight.client.core.EpicFightClient;
import com.yesman.epicfight.client.input.EpicFightInputCategories;
import com.yesman.epicfight.client.input.EpicFightKeyMappings;
import com.yesman.epicfight.platform.ModPlatformProvider;
import com.yesman.epicfight.platform.neoforged.NeoForgedModPlatform;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@Mod(value = EpicFight.MODID, dist = Dist.CLIENT)
public final class EpicFightNeoForgedClient {
    public EpicFightNeoForgedClient(IEventBus modEventBus, ModContainer modContainer) {
        // The same instance the main entry point registered its listeners on;
        // a second instance would receive the client events with empty listener maps
        NeoForgedModPlatform neoForgedModPlatform = (NeoForgedModPlatform) ModPlatformProvider.get();
        EpicFightClient.onModConstructed();

        // Register mod bus events
        modEventBus.addListener(this::doClientStuff);
        //modEventBus.addListener(this::dataGen);
        modEventBus.addListener(this::registerKeyMappings);
        neoForgedModPlatform.addClientListeners(modEventBus);
    }

    private void doClientStuff(final FMLClientSetupEvent event) {
        EpicFightClient.onModInitialized();
    }
/*
    private void dataGen(final GatherDataEvent.Client event) {
        event.createDatapackRegistryObjects(
            new RegistrySetBuilder().add(AkytheraRegistries.DataPack.ANIM_SEQUENCE_KEY, EpicFightAnimationSequences::bootstrap)
        );

        event.createProvider(EpicFightAnimationSequences::new);
        event.createProvider(EpicFightAnimationMontages::new);
        event.createProvider(EpicFightBlendSpace2Ds::new);
    }
*/
    private void registerKeyMappings(final RegisterKeyMappingsEvent event) {
        event.registerCategory(EpicFightInputCategories.COMBAT);
        event.registerCategory(EpicFightInputCategories.GUI);
        event.registerCategory(EpicFightInputCategories.SYSTEM);
        event.registerCategory(EpicFightInputCategories.CAMERA);

        EpicFightKeyMappings.getModdedKeys().forEach(event::register);
    }
}
