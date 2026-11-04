package yesman.epicfight.platform.fabric.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.EpicFight;

/// On NeoForge, EntityJoinLevelEvent fired when an entity joined a world.
/// On Fabric, we inject into ServerLevel.addEntity to call the equivalent hook.
@Mixin(value = ServerLevel.class)
public abstract class MixinServerLevel {
	static {
		EpicFight.LOGGER.info("[EpicFight] MixinServerLevel static initializer fired — mixin is loaded");
	}

	@Inject(at = @At(value = "TAIL"), method = "addEntity(Lnet/minecraft/world/entity/Entity;)Z")
	private void epicfight$onAddEntity(Entity entity, CallbackInfoReturnable<Boolean> cir) {
		try {
			if (entity instanceof net.minecraft.server.level.ServerPlayer) {
				EpicFight.LOGGER.info("[EpicFight] MixinServerLevel.addEntity TAIL — ServerPlayer added, cir={}", cir.getReturnValue());
			}
			VanillaEntityEventHooks.onJoinLevel(entity, (ServerLevel)(Object)this, false);
		} catch (Throwable e) {
			EpicFight.LOGGER.error("[EpicFight] MixinServerLevel.addEntity exception", e);
		}
	}
}
