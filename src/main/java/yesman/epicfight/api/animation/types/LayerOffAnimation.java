package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationClip;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.Optional;

public class LayerOffAnimation extends DynamicAnimation implements AnimationAccessor<LayerOffAnimation> {
	private AssetAccessor<? extends DynamicAnimation> lastAnimation;
	private Pose lastPose;
	private final Priority layerPriority;
	
	public LayerOffAnimation(Priority layerPriority) {
		this.layerPriority = layerPriority;
		this.animationClip = new AnimationClip();
	}
	
	public void setLastPose(Pose pose) {
		this.lastPose = pose;
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		if (entitypatch.isLogicalClient() && isEnd) {
			entitypatch.getClientAnimator().baseLayer.disableLayer(this.layerPriority);
		}
	}
	
	private final ThreadLocal<Pose> lowerPoseScratch = ThreadLocal.withInitial(Pose::new);

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		return this.getPoseByTime(entitypatch, time, partialTicks, new Pose());
	}

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
		entitypatch.getClientAnimator().getComposedLayerPoseBelow(this.layerPriority, partialTicks, this.lowerPoseScratch.get());
		Pose interpolatedPose = Pose.interpolatePose(this.lastPose, this.lowerPoseScratch.get(), time / this.getTotalTime(), pose);
		interpolatedPose.disableJoint((joint) -> !this.lastPose.hasTransform(joint.getKey()));

		return interpolatedPose;
	}
	
	@Override
	public Optional<JointMaskEntry> getJointMaskEntry(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion) {
		return this.lastAnimation.get().getJointMaskEntry(entitypatch, useCurrentMotion);
	}
	
	@Override
	public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
		return this.lastAnimation.get().getProperty(propertyType);
	}
	
	public void setLastAnimation(AssetAccessor<? extends DynamicAnimation> animation) {
		this.lastAnimation = animation;
	}
	
	@Override
	public boolean doesHeadRotFollowEntityHead() {
		return this.lastAnimation.get().doesHeadRotFollowEntityHead();
	}
	
	@Override
	public AssetAccessor<? extends StaticAnimation> getRealAnimation() {
		return Animations.EMPTY_ANIMATION;
	}

	@Override
	public AnimationClip getAnimationClip() {
		return this.animationClip;
	}
	
	@Override
	public boolean hasTransformFor(String joint) {
		return this.lastPose.hasTransform(joint);
	}
	
	@Override
	public boolean isLinkAnimation() {
		return true;
	}
	
	@Override
	public LayerOffAnimation get() {
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
	public AnimationAccessor<? extends LayerOffAnimation> getAccessor() {
		return this;
	}
	
	@Override
	public boolean inRegistry() {
		return false;
	}
}