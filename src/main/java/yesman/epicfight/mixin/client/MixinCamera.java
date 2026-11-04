package yesman.epicfight.mixin.client;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.event.types.camera.BuildCameraTransform;

@Mixin(value = Camera.class)
public abstract class MixinCamera {
    @Shadow
    private boolean initialized;
    @Shadow
    private BlockGetter level;
    @Shadow
    private Entity entity;
    @Shadow
    private boolean detached;
    @Shadow
    private float partialTickTime;
    
    @Inject(at = @At(value = "HEAD"), method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V", cancellable = true)
    public void epicfight$setup(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo callbackInfo) {
        this.initialized = true;
        this.level = level;
        this.entity = entity;
        this.detached = detached;
        this.partialTickTime = partialTick;

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        Camera camera = (Camera)(Object)this;
        BuildCameraTransform.Pre buildEvent = cameraApi.setupCamera(camera, partialTick);

        if (!buildEvent.isCanceled()) {
            if (buildEvent.isVanillaCameraSetupCanceled()) {
                callbackInfo.cancel();
            } else {
                cameraApi.fireCameraBuildPost(camera, partialTick);
            }
        }
    }

    @Inject(at = @At(value = "TAIL"), method = "setup(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF)V")
    public void epicfight$computeCameraAngles(BlockGetter level, Entity entity, boolean detached, boolean thirdPersonReverse, float partialTick, CallbackInfo callbackInfo) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        Camera camera = (Camera)(Object)this;
        cameraApi.computeCameraAngles(camera, partialTick);
    }
}
