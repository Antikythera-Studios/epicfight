package yesman.epicfight.world.capabilities.entitypatch.mob;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

public class WitherSkeletonPatch<T extends PathfinderMob> extends SkeletonPatch<T> {
	public WitherSkeletonPatch(T original) {
		super(original, Factions.WITHER);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.WITHER_SKELETON, EpicFightAttributes.STUN_ARMOR, 6.0D);
	}
	
	@Override
	protected void setWeaponMotions() {
		super.setWeaponMotions();
		this.weaponLivingMotions.put(WeaponCategories.SWORD, ImmutableMap.of(
			CapabilityItem.Styles.ONE_HAND, Set.of(
				Pair.of(LivingMotions.CHASE, Animations.WITHER_SKELETON_CHASE),
				Pair.of(LivingMotions.WALK, Animations.WITHER_SKELETON_WALK),
				Pair.of(LivingMotions.IDLE, Animations.WITHER_SKELETON_IDLE)
			)
		));
		
		this.weaponAttackMotions.put(WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.SKELETON_SWORD));
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonAggressiveRangedMobUpdateMotion(considerInaction);
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		OpenMatrix4f mat = super.getModelMatrix(partialTicks);
		return OpenMatrix4f.scale(new Vec3f(1.2F, 1.2F, 1.2F), mat, mat);
	}
}