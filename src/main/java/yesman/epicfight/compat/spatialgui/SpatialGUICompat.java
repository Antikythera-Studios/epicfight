package yesman.epicfight.compat.spatialgui;

import net.minecraft.client.Minecraft;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;
import org.tastytrash.spatialGUI.render.SpatialGUIRenderer;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.compat.ICompatModule;

/// Spatial GUI compatibility: while one of its 3D world-space screens is open,
/// Epic Fight must not engage the TPS camera rig or force a third-person
/// perspective switch (the spatial plane owns the camera).
public class SpatialGUICompat implements ICompatModule {
    @Override
    public void onInitializeClient() {
        EpicFightClientEventHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(event -> {
            if (spatialActive()) {
                event.cancel();
            }
        });
    }

    @Override
    public void onInitializeClientServer() {
    }

    @Override
    public void onInitialize() {
    }

    @Override
    public void onInitializeServer() {
    }

    /// True while Spatial GUI is actively rendering the current screen in world space.
    /// hookedScreen is only assigned after the mod's own enabled/hook checks.
    /// Only called from contexts where Spatial GUI is installed (compat-gated).
    public static boolean spatialActive() {
        SpatialGUIRenderer renderer = SpatialGUIClient.renderer();
        return renderer != null && renderer.getHookedScreen() != null
                && renderer.getHookedScreen() == Minecraft.getInstance().screen;
    }
}
