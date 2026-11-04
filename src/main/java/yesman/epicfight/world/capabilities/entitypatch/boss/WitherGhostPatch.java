package yesman.epicfight.world.capabilities.entitypatch.boss;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;

import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.WitherGhostClone;

public class WitherGhostPatch extends MobPatch<WitherGhostClone> {
	public WitherGhostPatch(WitherGhostClone entity) {
		super(entity);
	}

    @Override
    public void onJoinWorld(WitherGhostClone entity, Level level, boolean worldgenSpawn) {
        super.onJoinWorld(entity, level, worldgenSpawn);
		
		if (!this.original.isNoAi()) {
			this.playAnimation(Animations.WITHER_CHARGE, 0.0F);
			
			if (this.isLogicalClient()) {
				this.playSound(SoundEvents.WITHER_AMBIENT, -0.1F, 0.1F);
			}
		}
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.WITHER_IDLE);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.WITHER_IDLE);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EpicFightEntityTypes.WITHER_GHOST_CLONE.get(), EpicFightAttributes.IMPACT, 3.0D);
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		this.currentLivingMotion = LivingMotions.IDLE;
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		return null;
	}
}