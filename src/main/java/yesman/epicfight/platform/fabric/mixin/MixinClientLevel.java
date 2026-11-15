package yesman.epicfight.platform.fabric.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.EnderMan;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

/// On NeoForge, EntityJoinLevelEvent fired when an entity joined a world.
/// On Fabric, we inject into ClientLevel.addEntity to call the equivalent hook.
@Mixin(value = ClientLevel.class)
public abstract class MixinClientLevel {
	@Inject(at = @At(value = "TAIL"), method = "addEntity(Lnet/minecraft/world/entity/Entity;)V")
	private void epicfight$onAddEntity(Entity entity, CallbackInfo callbackInfo) {
		try {
			VanillaEntityEventHooks.onJoinLevel(entity, (ClientLevel)(Object)this, false);

			// On NeoForge, Entity.onAddedToLevel() is a patched method that fires when
			// an entity is added to the level's entity storage. Fabric doesn't have this
			// method, so we call the entity patch's onAddedToLevel() here instead.
			EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
				try { entitypatch.onAddedToLevel(); } catch (Throwable ignored) {}
			});

			// Cancel spawning enderman on the main island where Ender Dragon exists
			if (entity.getType() == EntityType.ENDERMAN) {
				if (VanillaEntityEventHooks.onEnderManSapwns((EnderMan) entity)) {
					entity.discard();
				}
			}
		} catch (Throwable ignored) {}
	}
}
