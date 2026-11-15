package yesman.epicfight.mixin.common;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.EpicFight;

/// Fires Epic Fight's preTick/postTick hooks around [Entity#tick].
///
/// On NeoForge, this was [EntityTickEvent.Pre]/[EntityTickEvent.Post] dispatched via the event bus to
/// [NeoForgeEntityEvent], which called [VanillaEntityEventHooks.preTick]/[postTick].
/// On Fabric, we inject directly into [Entity#tick], which is called exactly once per entity per tick
/// on both client and server. [VanillaEntityEventHooks.preTick] dispatches to [preTickClient] or
/// [preTickServer] based on [isLogicalClient].
@Mixin(value = Entity.class, priority = 500)
public abstract class MixinEntityTick {
	private static int epicfight$preTickErrorCount = 0;
	private static int epicfight$postTickErrorCount = 0;

	@Inject(method = "tick()V", at = @At("HEAD"))
	private void epicfight$preTick(CallbackInfo callbackInfo) {
		try {
			VanillaEntityEventHooks.preTick((Entity)(Object)this);
		} catch (Throwable e) {
			// Log first occurrence at WARN, then every 1000th at ERROR to avoid log spam.
			// These exceptions prevent animator.tick() from advancing, causing animation stutter.
			int count = epicfight$preTickErrorCount++;
			if (count == 0) {
				EpicFight.LOGGER.error("[EpicFight] preTick exception for entity {} (first occurrence): {}", ((Entity)(Object)this).getType(), e.toString(), e);
			} else if ((count % 1000) == 0) {
				EpicFight.LOGGER.warn("[EpicFight] preTick exception for entity {} ({} occurrences): {}", ((Entity)(Object)this).getType(), count, e.toString());
			}
		}
	}

	@Inject(method = "tick()V", at = @At("TAIL"))
	private void epicfight$postTick(CallbackInfo callbackInfo) {
		try {
			VanillaEntityEventHooks.postTick((Entity)(Object)this);
		} catch (Throwable e) {
			int count = epicfight$postTickErrorCount++;
			if (count == 0) {
				EpicFight.LOGGER.error("[EpicFight] postTick exception for entity {} (first occurrence): {}", ((Entity)(Object)this).getType(), e.toString(), e);
			} else if ((count % 1000) == 0) {
				EpicFight.LOGGER.warn("[EpicFight] postTick exception for entity {} ({} occurrences): {}", ((Entity)(Object)this).getType(), count, e.toString());
			}
		}
	}
}
