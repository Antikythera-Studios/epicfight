package yesman.epicfight.compat.shouldersurfing;

import com.github.exopandora.shouldersurfing.api.client.IShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.ShoulderSurfing;
import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputePlayerAttackStateEventHandler;
import com.github.exopandora.shouldersurfing.api.client.event.handler.ComputeTemporaryFirstPersonStateEventHandler;
import com.github.exopandora.shouldersurfing.api.event.IEventBus;
import com.github.exopandora.shouldersurfing.api.plugin.IShoulderSurfingPlugin;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.camera.BuildCameraTransform;
import yesman.epicfight.api.client.event.types.camera.LockOnEvent;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisableState;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisabledReason;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

/// Adds compatibility for Shoulder Surfing Reloaded mod.
///
/// The Shoulder Surfing Reloaded mod includes a camera decoupling feature,
/// allowing the player to rotate the camera 360° without rotating their controlled character.
///
/// ### Fixes
///
/// This makes the following changes:
///
/// - Automatically rotates the player to face the crosshair target as needed to attack,
///   when attacking via sword or charging a skill (e.g., demolition leap).
///   This is regardless of the Shoulder Surfing's config,
///   as the default configs (e.g., `"REQUIRES_TARGET"``) are meant for vanilla combat mechanics.
/// - Explicit lock-on support
/// - Disables Epic Fight TPS camera, to prevent undefined behavior from
///   using both Shoulder Surfing and Epic Fight perspectives.
///
/// ### Related issues
///
/// For reference:
///
/// - [Add lock-on support for Shoulder Surfing](https://github.com/Epic-Fight/epicfight/issues/2258)
/// - [Player doesn't turn in Shoulder Surfing when holding Epic Fight skills](https://github.com/Epic-Fight/epicfight/issues/2114)
/// - [Shoulder Surfing mod doesn't turn player when attacking by default](https://github.com/Epic-Fight/epicfight/issues/2113)
/// - [Shoulder Surfing mod does not detect Epic Fight custom Attack keybind](https://github.com/Epic-Fight/epicfight/issues/2111)
/// - [Handle Epic Fight Breaking Changes from Shoulder Surfing's side](https://github.com/Exopandora/ShoulderSurfing/issues/359)
@SuppressWarnings("unused") // Referenced in src/main/resources/shouldersurfing_plugin.json
public class ShoulderSurfingCompat implements IShoulderSurfingPlugin {
    @Override
    public void register(IEventBus eventBus) {
        disableEpicFightCamera();
        registerEpicFightEvents();
        registerShoulderSurfingEvents(eventBus);
    }

    private void disableEpicFightCamera() {
        EpicFightTpsCameraDisableState.disable(EpicFightTpsCameraDisabledReason.ShoulderSurfing);
    }

    private void registerShoulderSurfingEvents(IEventBus eventBus) {
        // Cast lambdas to specific handler types to resolve overload ambiguity in v5 API
        eventBus.register((ComputePlayerAttackStateEventHandler) event -> {
            event.setResult(InputManager.isActionActive(EpicFightInputAction.ATTACK) || InputManager.isActionActive(MinecraftInputAction.ATTACK_DESTROY));
        });
        eventBus.register((ComputeTemporaryFirstPersonStateEventHandler) event -> {
            final LocalPlayerPatch localPlayerPatch = EpicFightCapabilities.ClientModule.getCachedLocalPlayerPatch();
            if (localPlayerPatch != null) {
                event.setResult(localPlayerPatch.isHoldingAny());
            }
        });
    }

    private void registerEpicFightEvents() {
        EpicFightClientEventHooks.Camera.BUILD_TRANSFORM_PRE.registerEvent(ShoulderSurfingCompat::buildCameraTransform);
        EpicFightClientEventHooks.Camera.LOCK_ON_TICK.registerEvent(ShoulderSurfingCompat::lockOnTick);
    }

    private static void buildCameraTransform(BuildCameraTransform.Pre event) {
        final IShoulderSurfing shoulderSurfing = ShoulderSurfing.getInstance();

        // Prevents Epic Fight from applying camera transform modifications to Shoulder Surfing's perspective
        if (shoulderSurfing.isShoulderSurfing()) {
            if (event.getCameraApi().isLockingOnTarget()) {
                syncLockOnRotations(event, shoulderSurfing);
            }

            event.cancel();
        }
    }

    /// Sync the Epic Fight's lock-on rotation updates to the Shoulder Surfing's camera perspective
    private static void syncLockOnRotations(BuildCameraTransform.Pre event, IShoulderSurfing shoulderSurfing) {
        final float camXRot = Mth.rotLerp(event.getPartialTick(), event.getCameraApi().getCameraXRotO(), event.getCameraApi().getCameraXRot());
        final float camYRot = Mth.rotLerp(event.getPartialTick(), event.getCameraApi().getCameraYRotO(), event.getCameraApi().getCameraYRot());

        shoulderSurfing.getCamera().setXRot(camXRot);
        shoulderSurfing.getCamera().setYRot(camYRot);
    }

    private static void lockOnTick(LockOnEvent.Tick event) {
        final IShoulderSurfing instance = ShoulderSurfing.getInstance();

        // Calculates lock-on rotations based on the SSR's camera position, store those rotations to Epic Fight camera API's rotations
        // since they will eventually be written to SSR's camera rotation in BUILD_TRANSFORM_PRE.
        if (!instance.isShoulderSurfing()) {
            return;
        }
        final LocalPlayer localPlayer = event.getCameraApi().getMinecraft().player;
        assert localPlayer != null;
        final double toTargetDistanceSqr = localPlayer.position().distanceToSqr(event.getLockOnTarget().position());

        // Leaps the start and end location of the camera arm for lock-on based on the distance between the player and the focusing entity
        final Vec3 lockStart = MathUtils.lerpVector(localPlayer.getEyePosition(), event.getCameraApi().getMinecraft().gameRenderer.getMainCamera().getPosition(), (float) Mth.clampedMap(toTargetDistanceSqr, 1.0F, 18.0F, 0.2F, 1.0F));
        final Vec3 lockEnd = MathUtils.lerpVector(event.getLockOnTarget().getEyePosition(), event.getLockOnTarget().getBoundingBox().getCenter(), (float) Mth.clampedMap(toTargetDistanceSqr, 0.0F, 18.0F, 0.5F, 1.0F));

        final float clamp = 30.0F;
        final Vec3 toTarget = lockEnd.subtract(lockStart);
        float xRot = (float) MathUtils.getXRotOfVector(toTarget);
        final float yRot = (float) MathUtils.getYRotOfVector(toTarget);

        final CameraType cameraType = event.getCameraApi().getMinecraft().options.getCameraType();
        if (!cameraType.isFirstPerson()) xRot = Mth.clamp(xRot, -clamp, clamp);

        final float xLerp = Mth.clamp(Mth.wrapDegrees(xRot - instance.getCamera().getXRot()) * 0.4F, -clamp, clamp);
        final float yLerp = Mth.clamp(Mth.wrapDegrees(yRot - instance.getCamera().getYRot()) * 0.4F, -clamp, clamp);
        final Vec3 playerToTarget = lockEnd.subtract(localPlayer.getEyePosition());
        event.getCameraApi().setCameraRotations(instance.getCamera().getXRot() + xLerp, instance.getCamera().getYRot() + yLerp, false);
        event.setXRot((float) MathUtils.getXRotOfVector(playerToTarget));
        event.setYRot((float) MathUtils.getYRotOfVector(playerToTarget));
    }
}
