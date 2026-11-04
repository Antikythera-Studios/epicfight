package yesman.epicfight.world.capabilities.entitypatch.mob;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.SwellGoal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.monster.Creeper;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.goal.CreeperSwellStoppableGoal;

import java.util.Set;

public class CreeperPatch extends MobPatch<Creeper> {
	public CreeperPatch(Creeper creeper) {
		super(creeper, Factions.NEUTRAL);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.CREEPER, EpicFightAttributes.STUN_ARMOR, 1.0D);
	}
	
	@Override
	protected void selectGoalToRemove(Set<Goal> toRemove) {
		for (WrappedGoal wrappedGoal : this.original.goalSelector.getAvailableGoals()) {
			Goal goal = wrappedGoal.getGoal();
			
			if (goal instanceof SwellGoal) {
				toRemove.add(goal);
			}
		}
	}
	
	@Override
	protected void initAI() {
		super.initAI();
		
        this.original.goalSelector.addGoal(2, new CreeperSwellStoppableGoal(this, this.original));
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.CREEPER_IDLE);
		animator.addLivingAnimation(LivingMotions.WALK, Animations.CREEPER_WALK);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.CREEPER_DEATH);
	}
	
	@Override
	public void preTickServer() {
		super.preTickServer();
		
		if (this.getEntityState().inaction()) {
			for (WrappedGoal goal : this.original.goalSelector.getAvailableGoals()) {
				if (goal.getGoal() instanceof CreeperSwellStoppableGoal && goal.isRunning()) {
					goal.stop();
				}
			}
		}
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonMobUpdateMotion(considerInaction);
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		OpenMatrix4f mat = super.getModelMatrix(partialTicks);

		if (this.isLogicalClient()) {
			float f = this.original.getSwelling(partialTicks);
			float f1 = 1.0F + Mth.sin(f * 100.0F) * f * 0.01F;
	        f = Mth.clamp(f, 0.0F, 1.0F);
	        f = f * f;
	        f = f * f;
	        float f2 = (1.0F + f * 0.4F) * f1;
	        float f3 = (1.0F + f * 0.1F) / f1;
	        
			OpenMatrix4f.scale(new Vec3f(f2, f3, f2), mat, mat);
		}
		
		return mat;
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		if (stunType == StunType.LONG) {
			return Animations.CREEPER_HIT_LONG;
		} else {
			return Animations.CREEPER_HIT_SHORT;
		}
	}
}