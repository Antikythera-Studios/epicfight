package guivnf.epicfight.fabric;

import guivnf.epicfight.EpicFight;
import net.fabricmc.api.ModInitializer;

public final class EpicFightFabric implements ModInitializer {
    public EpicFightFabric() {
        EpicFight.onModConstructed();
    }

    @Override
    public void onInitialize() {
        EpicFight.registerObjects();
        EpicFight.onModInitialized();
    }
}
