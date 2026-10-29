package yesman.epicfight.client.renderer;

import net.minecraft.client.renderer.culling.Frustum;

/// Holds the current render frustum captured by [MixinLevelRenderer].
/// On NeoForge, [LevelRenderer.getFrustum()] provides this directly.
/// On Fabric, the frustum is a local variable in [renderLevel], so the mixin
/// captures it and stores it here for use by [EpicFightCameraAPI.setNextLockOnTarget].
public final class EpicFightFrustumHolder {
    private static Frustum currentFrustum;

    public static void set(Frustum frustum) {
        currentFrustum = frustum;
    }

    public static Frustum get() {
        return currentFrustum;
    }

    private EpicFightFrustumHolder() {}
}
