package yesman.epicfight.api.animation.types.grappling;

import java.util.List;

import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LongHitAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.MutableBoolean;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class GrapplingTryAnimation extends AttackAnimation {
	private final AnimationAccessor<? extends GrapplingAttackAnimation> grapplingAttackAnimation;
	private final AnimationAccessor<? extends ActionAnimation> failAnimation;
	private final AnimationAccessor<? extends LongHitAnimation> grapplingHitAnimation;
	
	public GrapplingTryAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, Collider collider, Joint colliderJoint, AnimationAccessor<? extends GrapplingTryAnimation> accessor,
		AnimationAccessor<? extends LongHitAnimation> grapplingHitAnimation,
		AnimationAccessor<? extends GrapplingAttackAnimation> grapplingAttackAnimation,
		AnimationAccessor<? extends ActionAnimation> failAnimation,
		AssetAccessor<? extends Armature> armature
	) {
		this(convertTime, antic, preDelay, contact, recovery, InteractionHand.MAIN_HAND, collider, colliderJoint, accessor, grapplingHitAnimation, grapplingAttackAnimation, failAnimation, armature);
	}
	
	public GrapplingTryAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, Collider collider, Joint colliderJoint, AnimationAccessor<? extends GrapplingTryAnimation> accessor,
		AnimationAccessor<? extends LongHitAnimation> grapplingHitAnimation,
		AnimationAccessor<? extends GrapplingAttackAnimation> grapplingAttackAnimation,
		AnimationAccessor<? extends ActionAnimation> failAnimation,
		AssetAccessor<? extends Armature> armature
	) {
		super(convertTime, antic, preDelay, contact, recovery, hand, collider, colliderJoint, accessor, armature);
		this.grapplingAttackAnimation = grapplingAttackAnimation;
		this.failAnimation = failAnimation;
		this.grapplingHitAnimation = grapplingHitAnimation;
		
		this.addProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR, 0.0F);
		this.addProperty(ActionAnimationProperty.MOVE_ON_LINK, false);
		this.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, null);
		this.addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_ORIGIN_AS_DESTINATION);
		this.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.WORLD_COORD);
		this.addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.LOOK_DEST);
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		super.begin(entitypatch);
		
		if (!entitypatch.isLogicalClient()) {
			LivingEntity hitEntity = entitypatch.getTarget();
			
			if (hitEntity != null) {
				EpicFightCapabilities.getUnparameterizedEntityPatch(hitEntity, LivingEntityPatch.class).ifPresent(LivingEntityPatch::notifyGrapplingWarning);
			}
		}
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		super.end(entitypatch, nextAnimation, isEnd);
		
		if (isEnd && !entitypatch.isLogicalClient()) {
			LivingEntity hitEntity = entitypatch.getTarget();
			MutableBoolean mb = new MutableBoolean(false);
			
			if (hitEntity != null) {
				Phase phase = this.getPhaseByTime(0.0F);
				AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(this.getAccessor());
				float prevPoseTime = player.getPrevElapsedTime();
				float poseTime = player.getElapsedTime();
				List<Entity> list = phase.getCollidingEntities(entitypatch, this, prevPoseTime, poseTime, this.getPlaySpeed(entitypatch, this));
				
				if (list.contains(hitEntity)) {
					DamageSource dmgSource = this.getEpicFightDamageSource(entitypatch, hitEntity, phase);
					EpicFightCapabilities.<LivingEntity, LivingEntityPatch<LivingEntity>>getParameterizedEntityPatch(hitEntity, LivingEntity.class, LivingEntityPatch.class).ifPresentOrElse(hitEntityPatch -> {
						if (hitEntityPatch.tryHurt(dmgSource, 0.0F).resultType.dealtDamage()) {
							entitypatch.reserveAnimation(this.grapplingAttackAnimation);
							entitypatch.setGrapplingTarget(hitEntity);
							hitEntity.lookAt(EntityAnchorArgument.Anchor.FEET, entitypatch.getOriginal().position());
							hitEntityPatch.playAnimationSynchronized(this.grapplingHitAnimation, 0.0F);
							mb.set(true);
						}
					}, () -> {
						entitypatch.reserveAnimation(this.grapplingAttackAnimation);
						entitypatch.setGrapplingTarget(hitEntity);
						mb.set(true);
					});
				}
			}
			
			if (!mb.value()) {
				entitypatch.reserveAnimation(this.failAnimation);
			}
		}
	}
	
	@Override
	protected void attackTick(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> animation) {
	}
}