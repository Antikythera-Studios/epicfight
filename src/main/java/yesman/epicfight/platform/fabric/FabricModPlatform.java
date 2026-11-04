package yesman.epicfight.platform.fabric;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.platform.ModPlatform;

public final class FabricModPlatform implements ModPlatform {
    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public boolean isModLoaded(@NotNull final String id) {
        return FabricLoader.getInstance().isModLoaded(id);
    }
}
