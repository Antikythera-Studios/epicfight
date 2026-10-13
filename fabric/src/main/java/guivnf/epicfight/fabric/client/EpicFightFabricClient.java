package guivnf.epicfight.fabric.client;

import guivnf.epicfight.client.input.EpicFightKeyMappings;
import guivnf.epicfight.fabric.client.networking.ClientBoundNetworkReceiverRegistrar;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;

public final class EpicFightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Initialize networking receivers. Other components are registered via `AkytheraFabric`
        ClientBoundNetworkReceiverRegistrar.registerTypes();
        ClientBoundNetworkReceiverRegistrar.registerReceivers();

        registerKeyMappings();
    }

    public void registerKeyMappings() {
        EpicFightKeyMappings.getModdedKeys().forEach(KeyMappingHelper::registerKeyMapping);
    }
}
