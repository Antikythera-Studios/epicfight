package yesman.epicfight.mixin.client;

import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.client.events.engine.ControlEngine;

/// Fires Epic Fight's movement input update hook after [Input#tick] during [LocalPlayer#aiStep].
///
/// On NeoForge, this was [MovementInputUpdateEvent] dispatched to [ControlEngine#epicfight$moveInputEvent],
/// which called [ControlEngine#inputTick] and posted [MappedMovementInputUpdateEvent].
/// On Fabric, we inject directly after the [Input#tick] INVOKE in [LocalPlayer#aiStep].
@Mixin(value = LocalPlayer.class)
public abstract class MixinLocalPlayerInput {
	@Inject(
		method = "aiStep()V",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/player/Input;tick(ZF)V",
			shift = At.Shift.AFTER
		)
	)
	private void epicfight$onMovementInputUpdate(CallbackInfo callbackInfo) {
		LocalPlayer self = (LocalPlayer)(Object)this;
		ControlEngine controlEngine = ControlEngine.getInstance();

		// Pass the player's Input to ControlEngine for inputTick + MAPPED_MOVEMENT_INPUT_UPDATE
		controlEngine.handleMovementInput(self.input);
	}
}
