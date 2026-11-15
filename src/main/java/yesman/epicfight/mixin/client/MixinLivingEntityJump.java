package yesman.epicfight.mixin.client;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.client.events.engine.ControlEngine;

/// Fires Epic Fight's jump hook when a [LivingEntity] jumps.
///
/// On NeoForge, this was [LivingJumpEvent] dispatched to:
/// - [NeoForgeEntityEvent#epicfight$livingJump] → [VanillaEntityEventHooks#onJump] (plays jump animation)
/// - [ControlEngine#epicfight$livingJumpEvent] → sets [tickSinceLastJump = 5] for local player
///
/// On Fabric, we inject directly into [LivingEntity#jumpFromGround] and handle both.
/// This is a client mixin because [VanillaEntityEventHooks#onJump] only acts when [isLogicalClient]
/// is true, and [ControlEngine] is client-only.
@Mixin(value = LivingEntity.class)
public abstract class MixinLivingEntityJump {
	@Inject(method = "jumpFromGround()V", at = @At("HEAD"))
	private void epicfight$onJump(CallbackInfo callbackInfo) {
		LivingEntity self = (LivingEntity)(Object)this;

		try {
			VanillaEntityEventHooks.onJump(self);
		} catch (Throwable ignored) {}

		// Set tickSinceLastJump for the local player (mover skill double-jump detection)
		if (self instanceof LocalPlayer) {
			try {
				ControlEngine.getInstance().setTickSinceLastJump(5);
			} catch (Throwable ignored) {}
		}
	}
}
