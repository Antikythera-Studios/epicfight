package yesman.epicfight.compat.playeranimator;

import dev.kosmx.playerAnim.impl.IAnimatedPlayer;
import dev.kosmx.playerAnim.impl.animation.AnimationApplier;

import yesman.epicfight.api.client.event.EpicFightClientEventHooks;
import yesman.epicfight.api.client.event.types.render.ValidatePlayerModelEvent;
import yesman.epicfight.compat.ICompatModule;

public class PlayerAnimatorCompat implements ICompatModule {
    @Override
	public void onInitialize() {}

    @Override
	public void onInitializeServer() {}

    @Override
	public void onInitializeClient() {}

    @Override
	public void onInitializeClientServer() {
        EpicFightClientEventHooks.Render.VALIDATE_PLAYER_MODEL_TO_RENDER.registerEvent(this::renderEvent);
    }

    private void renderEvent(ValidatePlayerModelEvent event) {
        // In Epic Fight mode the mod owns the player model; only hand the model
        // over to playerAnimator while the patch is in vanilla mode. Otherwise
        // looping pose animations (e.g. bettercombat pose_two_handed_katana)
        // keep AnimationApplier.isActive() true forever and hide every stance.
        if (event.getPlayerPatch().isEpicFightMode()) {
            return;
        }

        AnimationApplier playerAnimatorAnimation = ((IAnimatedPlayer) event.getPlayerPatch().getOriginal()).playerAnimator_getAnimation();

        if (!event.getPlayerPatch().getClientAnimator().getPlayerFor(null).getAnimation().get().isMainFrameAnimation() && // The case when playing EF animation that controls player location
            playerAnimatorAnimation.isActive()
        ) {
            event.setShouldRender(false);
        }
    }
}
