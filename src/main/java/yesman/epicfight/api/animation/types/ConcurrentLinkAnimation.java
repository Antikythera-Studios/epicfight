package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Optional;

public class ConcurrentLinkAnimation extends DynamicAnimation implements AnimationAccessor<ConcurrentLinkAnimation> {
	protected AssetAccessor<? extends StaticAnimation> nextAnimation;
	protected AssetAccessor<? extends DynamicAnimation> currentAnimation;
	protected float startsAt;
	
	public ConcurrentLinkAnimation() {
		this.animationClip = new AnimationClip();
	}
	
	private static float cycleTime(float elapsed, float duration) {
		return Float.isFinite(elapsed) && Float.isFinite(duration) && duration > 0.0F ? elapsed % duration : 0.0F;
	}
	
	public void acceptFrom(AssetAccessor<? extends DynamicAnimation> currentAnimation, AssetAccessor<? extends StaticAnimation> nextAnimation, float time) {
		this.currentAnimation = currentAnimation;
		this.nextAnimation = nextAnimation;
		this.startsAt = Float.isFinite(time) ? time : 0.0F;
		this.setTotalTime(nextAnimation.get().getTransitionTime());
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		this.nextAnimation.get().linkTick(entitypatch, this);
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		if (!isEnd) {
			this.nextAnimation.get().end(entitypatch, nextAnimation, isEnd);
		} else {
			if (this.startsAt > 0.0F) {
				entitypatch.getAnimator().getPlayer(this).ifPresent(player -> {
					player.setElapsedTime(this.startsAt);
					player.markDoNotResetTime();
				});
				
				this.startsAt = 0.0F;
			}
		}
	}
	
	@Override
	public EntityState getState(LivingEntityPatch<?> entitypatch, float time) {
		return this.nextAnimation.get().getState(entitypatch, 0.0F);
	}
	
	@Override
	public <T> T getState(StateFactor<T> stateFactor, LivingEntityPatch<?> entitypatch, float time) {
		return this.nextAnimation.get().getState(stateFactor, entitypatch, 0.0F);
	}
	
	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		float elapsed = time + this.startsAt;
		float currentElapsed = cycleTime(elapsed, this.currentAnimation.get().getTotalTime());
		float nextElapsed = cycleTime(elapsed, this.nextAnimation.get().getTotalTime());
		Pose currentAnimPose = this.currentAnimation.get().getPoseByTime(entitypatch, currentElapsed, 1.0F);
		Pose nextAnimPose = this.nextAnimation.get().getPoseByTime(entitypatch, nextElapsed, 1.0F);
		float interpolate = Float.isFinite(time) && Float.isFinite(this.getTotalTime()) && this.getTotalTime() > 0.0F
			? Mth.clamp(time / this.getTotalTime(), 0.0F, 1.0F) : 1.0F;
		
		Pose interpolatedPose = Pose.interpolatePose(currentAnimPose, nextAnimPose, interpolate);
		JointMaskEntry maskEntry = this.nextAnimation.get().getJointMaskEntry(entitypatch, true).orElse(null);
		
		if (maskEntry != null && entitypatch.isLogicalClient()) {
			interpolatedPose.disableJoint((entry) ->
				maskEntry.isMasked(
				  this.nextAnimation.get().getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(Layer.LayerType.BASE_LAYER) == Layer.LayerType.BASE_LAYER ? entitypatch.getClientAnimator().currentMotion() : entitypatch.getClientAnimator().currentCompositeMotion()
				, entry.getKey()
			));
		}
		
		return interpolatedPose;
	}
	
	@Override
	public void modifyPose(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		this.nextAnimation.get().modifyPose(this, pose, entitypatch, time, partialTicks);
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
		return this.nextAnimation.get().getPlaySpeed(entitypatch, animation);
	}
	
	public void setNextAnimation(AnimationAccessor<? extends StaticAnimation> animation) {
		this.nextAnimation = animation;
	}
	
	@Override @ClientOnly
	public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
		return this.nextAnimation.get().getJointMaskEntry(entitypatch, useCurrentMotion);
	}
	
	@Override
	public boolean isMainFrameAnimation() {
		return this.nextAnimation.get().isMainFrameAnimation();
	}
	
	@Override
	public boolean isReboundAnimation() {
		return this.nextAnimation.get().isReboundAnimation();
	}
	
	@Override
	public AssetAccessor<? extends StaticAnimation> getRealAnimation() {
		return this.nextAnimation;
	}
	
	@Override
	public String toString() {
		return "ConcurrentLinkAnimation: Mix " + this.currentAnimation + " and " + this.nextAnimation;
	}
	
	@Override
	public AnimationClip getAnimationClip() {
		return this.animationClip;
	}
	
	@Override
	public boolean hasTransformFor(String joint) {
		return this.nextAnimation.get().hasTransformFor(joint);
	}
	
	@Override
	public boolean isLinkAnimation() {
		return true;
	}
	
	@Override
	public ConcurrentLinkAnimation get() {
		return this;
	}

	@Override
	public ResourceLocation registryName() {
		return null;
	}

	@Override
	public boolean isPresent() {
		return true;
	}

	@Override
	public int id() {
		return -1;
	}
	
	@Override
	public AnimationAccessor<? extends DynamicAnimation> getAccessor() {
		return this;
	}

	@Override
	public boolean inRegistry() {
		return false;
	}
}