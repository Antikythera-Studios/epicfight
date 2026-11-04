package yesman.epicfight.api.client.event.types.camera;

import net.minecraft.client.Camera;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.event.CancelableEvent;
import yesman.epicfight.mixin.client.MixinCamera;

public abstract class BuildCameraTransform extends CameraAPIEvent {
    private final Camera camera;
    private final float partialTick;

    public BuildCameraTransform(EpicFightCameraAPI cameraApi, Camera camera, float partialTick) {
        super(cameraApi);
        this.camera = camera;
        this.partialTick = partialTick;
    }

    public Camera getCamera() {
        return this.camera;
    }

    public float getPartialTick() {
        return this.partialTick;
    }

    public static final class Pre extends BuildCameraTransform implements CancelableEvent {
        private boolean cancelVanillaCameraSetup = false;

        public Pre(EpicFightCameraAPI cameraApi, Camera camera, float partialTick) {
            super(cameraApi, camera, partialTick);
        }

        /// Set to `true` to disable the vanilla camera setup process.
        /// Indicates that the camera transform has been modified through this event
        /// and should not be overwritten by vanilla camera setups.
        ///
        /// @see MixinCamera#epicfight$setup
        public void setVanillaCameraSetupCanceled(boolean flag) {
            this.cancelVanillaCameraSetup = flag;
        }

        public boolean isVanillaCameraSetupCanceled() {
            return this.cancelVanillaCameraSetup;
        }
    }

    /// This replaces [yesman.epicfight.platform.neoforged.client.event.Object.ComputeCameraAngles] event in NeoForge, allowing
    /// developers to modify local rotation of the camera
    public static final class Post extends BuildCameraTransform {
        public Post(EpicFightCameraAPI cameraApi, Camera camera, float partialTick) {
            super(cameraApi, camera, partialTick);
        }
    }
}