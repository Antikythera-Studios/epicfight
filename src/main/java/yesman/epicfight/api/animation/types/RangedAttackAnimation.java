package yesman.epicfight.api.animation.types;

import net.minecraft.world.entity.monster.RangedAttackMob;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class RangedAttackAnimation extends AttackAnimation {
	public RangedAttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, Collider collider, Joint colliderJoint, AnimationAccessor<? extends RangedAttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		super(convertTime, antic, preDelay, contact, recovery, collider, colliderJoint, accessor, armature);
	}
	
	@Override
	public void hurtCollidingEntities(LivingEntityPatch<?> entitypatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, Phase phase) {
		if (entitypatch.getTarget() != null && (entitypatch.getOriginal() instanceof RangedAttackMob rangedAttackMob)) {
			rangedAttackMob.performRangedAttack(entitypatch.getTarget(), elapsedTime);
		}
	}
}