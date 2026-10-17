package yesman.epicfight.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.config.ClientConfig;

@Mixin(value = Minecraft.class)
public class MixinMinecraft {
    @Shadow @Final
    public GameRenderer gameRenderer;

    @Shadow @Final
    public Options options;

    @Shadow
    public LocalPlayer player;

    @Shadow
    public ClientLevel level;

    @Shadow
    public HitResult hitResult;

    @Shadow
    public Entity getCameraEntity() { throw new AbstractMethodError(); }
	
	@Inject(at = @At(value = "HEAD"), method = "handleKeybinds()V")
	private void epicfight$handleKeybindsHEAD(CallbackInfo info) {
		ControlEngine.getInstance().handleEpicFightKeyMappings();
	}

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;setCameraType(Lnet/minecraft/client/CameraType;)V", shift = At.Shift.AFTER), method = "handleKeybinds()V")
    private void epicfight$handleKeybindsINVOKE(CallbackInfo callbackInfo) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();
        if (cameraApi.isTPSMode()) cameraApi.setCameraRotations(this.player.getXRot(), this.player.getYRot(), true);
        if (this.options.getCameraType() != CameraType.THIRD_PERSON_BACK && cameraApi.isLockingOnTarget()) cameraApi.setLockOn(false);
    }

	@Inject(at = @At(value = "HEAD"), method = "shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z", cancellable = true)
	private void epicfight$shouldEntityAppearGlowing(Entity entity, CallbackInfoReturnable<Boolean> callbackInfo) {
        if (EpicFightCameraAPI.getInstance().shouldHighlightTarget(entity)) {
            callbackInfo.setReturnValue(true);
            callbackInfo.cancel();
        }
	}

    @Inject(at = @At("HEAD"), method = "startAttack", cancellable = true)
    private void onStartVanillaAttack(CallbackInfoReturnable<Boolean> cir) {
        if (ControlEngine.shouldDisableVanillaAttack()) {
            // Prevents the player from performing vanilla attack actions while in Epic Fight mode.
            cir.cancel();
        } else if (ControlEngine.shouldCancelVanillaAttackOnEntity()) {
            // Blocks vanilla attacks against living entities when in vanilla mode (if gamerule disallows)
            cir.cancel();
        } else if (ControlEngine.shouldCancelVanillaAttackOnBlock()) {
            // Blocks vanilla attacks against blocks when holding a combat-categorized item that can't break them
            cir.cancel();
        }
    }

    @Inject(at = @At("HEAD"), method = "continueAttack", cancellable = true)
    private void onContinueVanillaAttack(boolean leftClick, CallbackInfo ci) {
        if (ControlEngine.shouldDisableVanillaAttack()) {
            // Prevents the player from breaking blocks such as grass while in Epic Fight mode.
            ci.cancel();
        }
    }

    /// Cancels vanilla use-item when in EpicFight mode and guard is available.
    /// Replaces NeoForge's InputEvent.InteractionKeyMappingTriggered handler for USE action.
    @Inject(at = @At("HEAD"), method = "startUseItem()V", cancellable = true)
    private void epicfight$startUseItem(CallbackInfo ci) {
        if (ControlEngine.shouldCancelVanillaUseItem()) {
            ci.cancel();
        }
    }

    @WrapOperation(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/GameRenderer;pick(F)V"
        ),
        method = "tick()V"
    )
    private void epicfight$tick(GameRenderer gameRenderer, float partialTick, Operation<Void> originalOperation) {
        if (EpicFightCameraAPI.getInstance().isTPSMode()) {
            this.pickInTPSPerspectiveMode();
        } else {
            this.gameRenderer.pick(partialTick);
        }
    }

    @Inject(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"
        ),
        method = "startUseItem"
    )
    private void startUseItem(CallbackInfo ci) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();

        if (cameraApi.isTPSMode()) {
            cameraApi.alignPlayerLookToCrosshair(false, true, false);
        }
    }

    /**
     * Code copy from {@link GameRenderer#pick(float)}
     */
    @Unique
    private void pickInTPSPerspectiveMode() {
        Entity entity = this.getCameraEntity();
        if (entity != null) {
            if (this.level != null) {
                this.hitResult = EpicFightCameraAPI.getInstance().getCrosshairHitResult();

                if (this.hitResult != null) {
                    double d0 = this.player.blockInteractionRange();
                    double entityReach = this.player.entityInteractionRange();
                    double distanceLimit = Math.max(d0, entityReach) + ClientConfig.cameraZoom * 0.5D;
                    Vec3 hitPos = this.hitResult.getLocation();

                    if (hitPos.distanceToSqr(this.gameRenderer.getMainCamera().getPosition()) > distanceLimit * distanceLimit) {
                        Vec3 cameraPos = this.gameRenderer.getMainCamera().getPosition();
                        this.hitResult = BlockHitResult.miss(hitPos, Direction.getNearest(cameraPos.x, cameraPos.y, cameraPos.z), BlockPos.containing(hitPos));
                    }
                }
            }
        }
    }
}
