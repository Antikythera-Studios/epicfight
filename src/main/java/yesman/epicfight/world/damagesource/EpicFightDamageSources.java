package yesman.epicfight.world.damagesource;

import javax.annotation.Nullable;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.WitherSkull;

public final class EpicFightDamageSources {
	private EpicFightDamageSources() {}
	
	public static EpicFightDamageSource fromVanillaDamageSource(DamageSource damageSource) {
		return new EpicFightDamageSource(damageSource);
	}

	public static EpicFightDamageSource shockwave(LivingEntity owner) {
		return new EpicFightDamageSource(getDamageTypeHolder(owner, EpicFightDamageTypes.SHOCKWAVE), owner, owner, null);
	}

	public static EpicFightDamageSource witherBeam(LivingEntity owner) {
		return new EpicFightDamageSource(getDamageTypeHolder(owner, EpicFightDamageTypes.WITHER_BEAM), owner, owner, null);
	}
	
	public static EpicFightDamageSource arrow(AbstractArrow arrow, @Nullable Entity shooter) {
		return new EpicFightDamageSource(getDamageTypeHolder(arrow, DamageTypes.ARROW), arrow, shooter, null);
	}
	
	public static EpicFightDamageSource trident(Entity trident, Entity thrower) {
		return new EpicFightDamageSource(getDamageTypeHolder(trident, DamageTypes.TRIDENT), trident, thrower, null);
	}

	public static EpicFightDamageSource witherSkull(WitherSkull witherSkull, Entity shooter) {
		return new EpicFightDamageSource(getDamageTypeHolder(witherSkull, DamageTypes.WITHER_SKULL), witherSkull, shooter, null);
	}
	
	public static EpicFightDamageSource mobAttack(LivingEntity owner) {
		return new EpicFightDamageSource(getDamageTypeHolder(owner, DamageTypes.MOB_ATTACK), owner, owner, null);
	}

	public static EpicFightDamageSource playerAttack(Player owner) {
		return new EpicFightDamageSource(getDamageTypeHolder(owner, DamageTypes.PLAYER_ATTACK), owner, owner, null);
	}

	public static EpicFightDamageSource enderDragonBreath(LivingEntity owner, Entity directEntity) {
		return fromVanillaDamageSource(owner.level().damageSources().indirectMagic(directEntity, owner));
	}
	
	private static Holder<DamageType> getDamageTypeHolder(Entity entity, ResourceKey<DamageType> damageTypeKey) {
		return entity.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey);
	}
}