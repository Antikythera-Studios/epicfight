package yesman.epicfight.mixin.common;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import yesman.epicfight.api.event.impl.VanillaPlayerEventHooks;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = ServerEntity.class)
public abstract class MixinServerEntity {

	@Shadow
	@Final
	private Entity entity;

	@Inject(at = @At(value = "TAIL"), method = "sendDirtyEntityData()V", cancellable = false)
	public void epicfight$sendDirtyEntityData(CallbackInfo callback) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(this.entity, LivingEntityPatch.class).ifPresent(entitypatch -> {
			EpicFightNetworkManager.PayloadBundleBuilder payloadsBuilder = entitypatch.getExpandedSynchedData().prepareDataToSend();

			if (payloadsBuilder != null) {
				payloadsBuilder.send((payload, payloads) -> entitypatch.sendToAllPlayersTrackingMe(payload, payloads));
			}
		});
	}

	/// PlayerEvent.StartTracking — fires when a server player starts tracking an entity
	@Inject(at = @At(value = "TAIL"), method = "addPairing(Lnet/minecraft/server/level/ServerPlayer;)V")
	public void epicfight$startTracking(ServerPlayer serverPlayer, CallbackInfo callback) {
		VanillaPlayerEventHooks.onStartTracking(this.entity, serverPlayer);
	}

	/// PlayerEvent.StopTracking — fires when a server player stops tracking an entity
	@Inject(at = @At(value = "HEAD"), method = "removePairing(Lnet/minecraft/server/level/ServerPlayer;)V")
	public void epicfight$stopTracking(ServerPlayer serverPlayer, CallbackInfo callback) {
		VanillaPlayerEventHooks.onStopTracking(this.entity, serverPlayer);
	}
}
