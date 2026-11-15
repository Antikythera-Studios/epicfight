package yesman.epicfight.compat.spatialgui;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.compat.ModMixinPlugin;

public final class SpatialGUIMixinPlugin extends ModMixinPlugin {
    @Override
    public @NotNull String getModId() {
        return "spatial-gui";
    }
}
