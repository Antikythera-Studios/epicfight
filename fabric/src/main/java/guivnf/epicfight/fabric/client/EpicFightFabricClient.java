package guivnf.epicfight.fabric.client;

import guivnf.epicfight.client.core.EpicFightClient;
import net.fabricmc.api.ClientModInitializer;

public final class EpicFightFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        EpicFightClient.onModInitialized();
    }
}
