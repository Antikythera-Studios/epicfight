package yesman.epicfight.world.capabilities.entitypatch.mob;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;

import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;

public class VindicatorPatch<T extends PathfinderMob> extends AbstractIllagerPatch<T> {
	public VindicatorPatch(T original) {
		super(original, Factions.ILLAGER);
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		
		animator.addLivingAnimation(LivingMotions.ANGRY, Animations.VINDICATOR_IDLE_AGGRESSIVE);
		animator.addLivingAnimation(LivingMotions.CHASE, Animations.VINDICATOR_CHASE);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.VINDICATOR, EpicFightAttributes.IMPACT, 1.0D);
	}
	
	@Override
	protected void setWeaponMotions() {
		super.setWeaponMotions();
		
		this.weaponLivingMotions.put(WeaponCategories.GREATSWORD, ImmutableMap.of(
			CapabilityItem.Styles.TWO_HAND, Set.of(
				Pair.of(LivingMotions.WALK, Animations.ILLAGER_WALK),
				Pair.of(LivingMotions.CHASE, Animations.BIPED_WALK_TWOHAND)
			)
		));
		
		this.weaponAttackMotions.put(WeaponCategories.AXE, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.VINDICATOR_ONEHAND));
		this.weaponAttackMotions.put(WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.VINDICATOR_ONEHAND));
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		if (this.original.getHealth() <= 0.0F) {
			currentLivingMotion = LivingMotions.DEATH;
		} else if (this.state.inaction() && considerInaction) {
			currentLivingMotion = LivingMotions.INACTION;
		} else {
			boolean isAngry = this.original.isAggressive();
			
			if (this.original.walkAnimation.speed() > 0.01F) {
				currentLivingMotion = isAngry ? LivingMotions.CHASE : LivingMotions.WALK;
			} else {
				currentLivingMotion = isAngry ? LivingMotions.ANGRY : LivingMotions.IDLE;
			}
		}
	}
	
	@Override
	public void setAIAsMounted(Entity ridingEntity) {

	}
}