package yesman.epicfight.api.animation.types;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.property.AnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Optional;

public class AimAnimation extends StaticAnimation {
	public DirectStaticAnimation lookForward;
	public DirectStaticAnimation lookUp;
	public DirectStaticAnimation lookDown;
	public DirectStaticAnimation lying;
	
	public AimAnimation(boolean repeatPlay, AnimationAccessor<? extends AimAnimation> accessor, String path1, String path2, String path3, String path4, AssetAccessor<? extends Armature> armature) {
		this(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME, repeatPlay, accessor, path1, path2, path3, path4, armature);
	}
	
	public AimAnimation(float transitionTime, boolean repeatPlay, AnimationAccessor<? extends AimAnimation> accessor, String path1, String path2, String path3, String path4, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, repeatPlay, accessor, armature);
		
		this.lookForward = new DirectStaticAnimation(transitionTime, repeatPlay, ResourceLocation.fromNamespaceAndPath(accessor.registryName().getNamespace(), path1), armature);
		this.lookUp = new DirectStaticAnimation(transitionTime, repeatPlay, ResourceLocation.fromNamespaceAndPath(accessor.registryName().getNamespace(), path2), armature);
		this.lookDown = new DirectStaticAnimation(transitionTime, repeatPlay, ResourceLocation.fromNamespaceAndPath(accessor.registryName().getNamespace(), path3), armature);
		this.lying = new DirectStaticAnimation(transitionTime, repeatPlay, ResourceLocation.fromNamespaceAndPath(accessor.registryName().getNamespace(), path4), armature);
		
		this.addProperty(
			StaticAnimationProperty.PLAY_SPEED_MODIFIER,
			(DynamicAnimation animation, LivingEntityPatch<?> entitypatch, float speed, float prevElapsedTime, float elapsedTime) -> {
				if (animation.isLinkAnimation()) {
					return 1.0F;
				}
				
				if (entitypatch.getOriginal().isUsingItem()) {
					return (this.getTotalTime() - elapsedTime) / this.getTotalTime();
				}
				
				return 1.0F;
			}
		);
		
		this.addProperty(
			StaticAnimationProperty.POSE_MODIFIER,
			(DynamicAnimation animation, Pose pose, LivingEntityPatch<?> entitypatch, float elapsedTime, float partialTicks) -> {
				if (!entitypatch.isFirstPerson() && !animation.isLinkAnimation()) {
					JointTransform chest = pose.orElseEmpty("Chest");
					JointTransform head = pose.orElseEmpty("Head");
					float f = 90.0F;
					float ratio = (f - Math.abs(entitypatch.getOriginal().getXRot())) / f;
					float yRotHead = Mth.lerp(partialTicks, entitypatch.getOriginal().yHeadRotO, entitypatch.getOriginal().yHeadRot);
					float yRot = entitypatch.getOriginal().getVehicle() != null ? yRotHead : Mth.lerp(partialTicks, entitypatch.getOriginal().yBodyRotO, entitypatch.getOriginal().yBodyRot);
					// Pre-negate both yaw corrections in mirror mode. The pose pair (head.rotation
					// and chest.frontResult) is designed so the chest twists toward the head's
					// world direction while the head's local rotation cancels that twist, keeping
					// head world rotation matching the authored pose. PoseMirror's global flip
					// (qy negation) preserves the cancellation -- both terms flip together -- but
					// flips the chest twist's world direction, leaving the chest visually leaning
					// AWAY from where the camera is pointing on offhand. With the body still
					// tracking yHeadRot via vanilla tickHeadTurn, the rendered chest no longer
					// follows the head, so the head appears to rotate way past the body's clamp
					// limit (most visible when turning camera left, where the asymmetric head/arm
					// pose biases the visual). Negating both deltas pre-mirror means the global
					// flip restores them to the mainhand world signs while keeping the
					// cancellation intact.
					float mirrorSign = entitypatch.isMirrorMode() ? -1.0F : 1.0F;

					MathUtils.mulQuaternion(QuaternionUtils.YP.rotationDegrees(Mth.wrapDegrees(yRot - yRotHead) * ratio * mirrorSign), head.rotation(), head.rotation());
					chest.frontResult(JointTransform.rotation(QuaternionUtils.YP.rotationDegrees(Mth.wrapDegrees(yRotHead - yRot) * ratio * mirrorSign)), OpenMatrix4f::mulAsOriginInverse);
				}
			}
		);
	}
	
	@Override
	public void loadAnimation() {
		this.lookForward.loadAnimation();
		this.lookUp.loadAnimation();
		this.lookDown.loadAnimation();
		this.lying.loadAnimation();
	}
	
	private final ThreadLocal<Pose> poseScratch = ThreadLocal.withInitial(Pose::new);

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks) {
		return this.getPoseByTime(entitypatch, time, partialTicks, new Pose());
	}

	@Override
	public Pose getPoseByTime(LivingEntityPatch<?> entitypatch, float time, float partialTicks, Pose pose) {
		if (!entitypatch.isFirstPerson()) {
			LivingMotion livingMotion = entitypatch.getCurrentLivingMotion();

			if (livingMotion == LivingMotions.SWIM || livingMotion == LivingMotions.FLY || livingMotion == LivingMotions.CREATIVE_FLY) {
				pose.loadDeep(this.lying.getPoseByTime(entitypatch, time, partialTicks));
				this.modifyPose(this, pose, entitypatch, time, partialTicks);

				return pose;
			} else {
				float pitch = entitypatch.getOriginal().getViewXRot(partialTicks);
				StaticAnimation interpolateAnimation;
				interpolateAnimation = (pitch > 0) ? this.lookDown : this.lookUp;
				// Same as DynamicAnimation#getPoseByTime(3-arg) but writing into {@code pose};
				// calling super's 4-arg here would delegate back to our 3-arg and recurse.
				this.getRawPose(time, pose);
				this.modifyPose(this, pose, entitypatch, time, partialTicks);
				Pose pose2 = this.poseScratch.get();
				pose2.loadDeep(interpolateAnimation.getPoseByTime(entitypatch, time, partialTicks));

				return Pose.interpolatePose(pose, pose2, (Math.abs(pitch) / 90.0F), pose);
			}
		}

		return pose.loadDeep(this.lookForward.getPoseByTime(entitypatch, time, partialTicks));
	}
	
	@Override
	public List<AssetAccessor<? extends StaticAnimation>> getSubAnimations() {
		return List.of(this.lookForward, this.lookUp, this.lookDown, this.lying);
	}
	
	@Override
	public <V> Optional<V> getProperty(AnimationProperty<V> propertyType) {
		return this.lookForward.getProperty(propertyType);
	}
	
	@Override @ClientOnly
	public Layer.Priority getPriority() {
		return this.lookForward.getProperty(ClientAnimationProperties.PRIORITY).orElse(Layer.Priority.LOWEST);
	}
	
	@Override @ClientOnly
	public Layer.LayerType getLayerType() {
		return this.lookForward.getProperty(ClientAnimationProperties.LAYER_TYPE).orElse(Layer.LayerType.BASE_LAYER);
	}
	
	@Override
	public AnimationClip getAnimationClip() {
		return this.lookForward.getAnimationClip();
	}
	
	@Override
	public boolean isClientAnimation() {
		return true;
	}
}