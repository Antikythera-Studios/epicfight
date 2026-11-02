package guivnf.epicfight.client.core;

import com.yesman.akythera.platform.ModPlatformProvider;
import guivnf.epicfight.akytheralabs.compat.AkytheraLabsPlugin;

public final class EpicFightClient {
    private EpicFightClient() {}

    public static void onModInitialized() {
        if (ModPlatformProvider.get().isModLoaded("akytheralabs")) {
            new AkytheraLabsPlugin().onInitializeClient();
        }
    }
}
