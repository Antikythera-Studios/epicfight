package yesman.epicfight.world.capabilities.entitypatch.mob;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Ravager;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

public class RavagerPatch extends MobPatch<Ravager> {
	public RavagerPatch(Ravager original) {
		super(original, Factions.ILLAGER);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.RAVAGER, EpicFightAttributes.MAX_STRIKES, 8.0D);
		event.add(EntityType.RAVAGER, EpicFightAttributes.IMPACT, 6.0D);
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.RAVAGER_IDLE);
		animator.addLivingAnimation(LivingMotions.WALK, Animations.RAVAGER_WALK);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.RAVAGER_DEATH);
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonMobUpdateMotion(considerInaction);
	}
	
	@Override
	protected void initAI() {
		super.initAI();
		this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, MobCombatBehaviors.RAVAGER.build(this)));
		this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.original, 1.0D, false));
	}
	
	@Override
	public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> opponent) {
		if (this.original.getStunnedTick() > 0) {
			this.playAnimationSynchronized(Animations.RAVAGER_STUN, 0.0F);
		}
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		return null;
	}

	@Override
	public SoundEvent getWeaponHitSound(InteractionHand hand) {
		return EpicFightSounds.BLUNT_HIT_HARD.get();
	}

	@Override
	public SoundEvent getSwingSound(InteractionHand hand) {
		return EpicFightSounds.WHOOSH_BIG.get();
	}
}