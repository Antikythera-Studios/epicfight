package yesman.epicfight.mixin.common;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

@Mixin(value = Player.class)
public abstract class MixinPlayer {
	// Player#actuallyHurt does not call super, so the LivingEntity mixin never sees players being hurt.
	// Without these, players get no Epic Fight stun/knockback and attackers get no weapon-innate charge on them.
	@ModifyVariable(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("HEAD"), argsOnly = true, ordinal = 0)
	private float epicfight$actuallyHurtPre(float amount, DamageSource damageSource) {
		float[] modified = {amount};
		VanillaEntityEventHooks.onCalculateDamagePre((Player)(Object)this, damageSource, amount, v -> modified[0] = v);
		return modified[0];
	}

	@Inject(method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V", at = @At("TAIL"))
	private void epicfight$actuallyHurtPost(DamageSource damageSource, float amount, CallbackInfo info) {
		VanillaEntityEventHooks.onCalculateDamagePost((Player)(Object)this, damageSource, amount);
	}

	@WrapOperation(
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/damagesource/CombatTracker;recordDamage(Lnet/minecraft/world/damagesource/DamageSource;F)V"
		),
		method = "actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"
	)
	private void epicfight$recordDamage(CombatTracker self, DamageSource damagesource, float damage, Operation<Void> operation) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(damagesource.getEntity(), LivingEntityPatch.class).ifPresent(entitypatch -> {
			entitypatch.setLastAttackEntity(self.mob);
		});
		
		self.recordDamage(damagesource, damage);
	}

    @WrapOperation(
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;getYRot()F"
        ),
        method = "serverAiStep()V"
    )
    private float epicfight$serverAiStep(Player player, Operation<Void> originalOperation) {
        if (player.isLocalPlayer()) {
            return EpicFightCameraAPI.getInstance().getYRotForHead(player);
        }

        return player.getYRot();
    }
}