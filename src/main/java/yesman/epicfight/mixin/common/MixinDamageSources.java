package yesman.epicfight.mixin.common;

import javax.annotation.Nullable;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

@Mixin(value = DamageSources.class)
public abstract class MixinDamageSources {
	@Inject(at = @At(value = "HEAD"), method = "mobAttack(Lnet/minecraft/world/entity/LivingEntity;)Lnet/minecraft/world/damagesource/DamageSource;", cancellable = true)
	public void epicfight$mobAttack(LivingEntity mob, CallbackInfoReturnable<DamageSource> callback) {
		MobPatch<?> mobpatch = EpicFightCapabilities.getEntityPatch(mob, MobPatch.class);
		
		if (mobpatch != null && mobpatch.getEpicFightDamageSource() != null) {
			callback.setReturnValue(mobpatch.getEpicFightDamageSource());
			callback.cancel();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "playerAttack(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/damagesource/DamageSource;", cancellable = true)
	public void epicfight$playerAttack(Player player, CallbackInfoReturnable<DamageSource> callback) {
		PlayerPatch<?> playerpatch = EpicFightCapabilities.getEntityPatch(player, PlayerPatch.class);
		
		if (playerpatch != null && playerpatch.getEpicFightDamageSource() != null) {
			callback.setReturnValue(playerpatch.getEpicFightDamageSource());
			callback.cancel();
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "arrow(Lnet/minecraft/world/entity/projectile/AbstractArrow;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", cancellable = true)
	public void epicfight$arrow(AbstractArrow arrow, Entity shooter, CallbackInfoReturnable<DamageSource> callback) {
		ProjectilePatch<?> projectilePatch = EpicFightCapabilities.getEntityPatch(arrow, ProjectilePatch.class);
		
		if (projectilePatch != null) {
			EpicFightDamageSource epicfightDamagesource = projectilePatch.createEpicFightDamageSource();
			
			if (epicfightDamagesource != null) {
				callback.setReturnValue(epicfightDamagesource);
				callback.cancel();
			}
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "witherSkull(Lnet/minecraft/world/entity/projectile/WitherSkull;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", cancellable = true)
	public void epicfight$witherSkull(WitherSkull witherSkull, Entity shooter, CallbackInfoReturnable<DamageSource> callback) {
		ProjectilePatch<?> projectilePatch = EpicFightCapabilities.getEntityPatch(witherSkull, ProjectilePatch.class);
		
		if (projectilePatch != null) {
			EpicFightDamageSource epicfightDamagesource = projectilePatch.createEpicFightDamageSource();
			
			if (epicfightDamagesource != null) {
				callback.setReturnValue(epicfightDamagesource);
				callback.cancel();
			}
		}
	}
	
	@Inject(at = @At(value = "HEAD"), method = "trident(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Lnet/minecraft/world/damagesource/DamageSource;", cancellable = true)
	public void epicfight$trident(Entity trident, @Nullable Entity thrower, CallbackInfoReturnable<DamageSource> callback) {
		ProjectilePatch<?> projectilePatch = EpicFightCapabilities.getEntityPatch(trident, ProjectilePatch.class);
		
		if (projectilePatch != null) {
			EpicFightDamageSource epicfightDamagesource = projectilePatch.createEpicFightDamageSource();
			
			if (epicfightDamagesource != null) {
				callback.setReturnValue(epicfightDamagesource);
				callback.cancel();
			}
		}
	}
}
