package guivnf.epicfight.fabric.client;

import guivnf.epicfight.client.core.EpicFightClient;
import guivnf.epicfight.client.input.EpicFightKeyMappings;
import guivnf.epicfight.fabric.client.networking.ClientBoundNetworkReceiverRegistrar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class EpicFightFabricClient implements ClientModInitializer {
    public EpicFightFabricClient() {
        EpicFightClient.onModConstructed();
    }

    @Override
    public void onInitializeClient() {
        EpicFightClient.onModInitialized();

        // Initialize networking receivers. Other components are registered via `AkytheraFabric`
        ClientBoundNetworkReceiverRegistrar.registerTypes();
        ClientBoundNetworkReceiverRegistrar.registerReceivers();

        registerKeyMappings();
    }

    public void registerKeyMappings() {
        EpicFightKeyMappings.getModdedKeys().forEach(KeyMappingHelper::registerKeyMapping);
    }
}
