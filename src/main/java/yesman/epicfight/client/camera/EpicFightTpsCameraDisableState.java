package yesman.epicfight.client.camera;
import yesman.epicfight.EpicFight;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.main.EpicFightMod;

import java.util.Objects;
import java.util.function.Predicate;

public final class EpicFightTpsCameraDisableState {
    private EpicFightTpsCameraDisableState() {
    }

    private static @Nullable EpicFightTpsCameraDisabledReason reason = null;
    private static boolean eventRegistered = false;

    public static void disable(@NotNull EpicFightTpsCameraDisabledReason reason) {
        Objects.requireNonNull(reason, "reason must not be null");

        EpicFightTpsCameraDisableState.reason = reason;
        EpicFight.LOGGER.info("Epic Fight TPS mode has been disabled due to a mod conflict with {}", reason.getModName());

        if (!eventRegistered) {
            EpicFightClientEventHooks.Camera.ACTIVATE_TPS_CAMERA.registerEvent(e -> {
                if (EpicFightTpsCameraDisableState.reason != null) {
                    e.cancel();
                }
            });
            eventRegistered = true;
        }
    }

    public static @Nullable EpicFightTpsCameraDisabledReason getReason() {
        return reason;
    }

    private static @Nullable Predicate<Runnable> actionDeferral = null;

    public static void setActionDeferral(@NotNull Predicate<Runnable> deferral) {
        actionDeferral = Objects.requireNonNull(deferral, "deferral must not be null");
    }

    public static boolean deferAction(@NotNull Runnable action) {
        return actionDeferral != null && actionDeferral.test(action);
    }
}
