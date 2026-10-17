package yesman.epicfight.compat.betterthirdperson;

import io.socol.betterthirdperson.BetterThirdPerson;
import io.socol.betterthirdperson.api.action.MouseAction;
import io.socol.betterthirdperson.impl.PlayerAdapter;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisableState;
import yesman.epicfight.client.camera.EpicFightTpsCameraDisabledReason;
import yesman.epicfight.compat.ICompatModule;

public final class BetterThirdPersonCompat implements ICompatModule {
    @Override
	public void onInitialize() {

    }

    @Override
	public void onInitializeServer() {

    }

    @Override
	public void onInitializeClient() {
        EpicFightTpsCameraDisableState.disable(EpicFightTpsCameraDisabledReason.BetterThirdPerson);
        EpicFightTpsCameraDisableState.setActionDeferral(BetterThirdPersonCompat::deferToCameraMod);
    }

    private static boolean deferToCameraMod(Runnable action) {
        Player player = Minecraft.getInstance().player;

        if (player == null) {
            return false;
        }

        return BetterThirdPerson.getCameraManager().onMouseAction(new PlayerAdapter(player), new MouseAction(action));
    }

    @Override
	public void onInitializeClientServer() {

    }
}