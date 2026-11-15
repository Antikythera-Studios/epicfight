package yesman.epicfight.compat.controlify;

import dev.isxander.controlify.api.bind.ControlifyBindApi;
import dev.isxander.controlify.bindings.BindContext;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.EpicFight;

public final class EpicFightControlifyBindContexts {
    private EpicFightControlifyBindContexts() {
    }

    public static final class EpicFight {
        private EpicFight() {
        }

        public static final BindContext COMBAT_MODE = new BindContext(
                yesman.epicfight.EpicFight.identifier("epicfight_combat"),
                mc -> {
                    final boolean isInGame = isInGame(mc);
                    return isInGame && ClientEngine.getInstance().isEpicFightMode();
                }
        );
        public static final BindContext LOCK_ON = new BindContext(
                yesman.epicfight.EpicFight.identifier("epicfight_lock_on"),
                mc -> {
                    final boolean isInGame = isInGame(mc);
                    return isInGame && EpicFightCameraAPI.getInstance().isLockingOnTarget();
                }
        );

        public static void register(@NotNull ControlifyBindApi registrar) {
            registrar.registerBindContext(COMBAT_MODE);
            registrar.registerBindContext(LOCK_ON);
        }
    }

    public static final BindContext IN_GAME = BindContext.IN_GAME;
    public static final BindContext ANY_SCREEN = BindContext.ANY_SCREEN;

    public static boolean isInGame(@NotNull Minecraft mc) {
        return mc.screen == null && mc.level != null && mc.player != null;
    }
}
