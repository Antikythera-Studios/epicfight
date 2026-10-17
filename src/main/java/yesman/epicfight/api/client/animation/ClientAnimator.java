package yesman.epicfight.api.client.animation;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.Layer.Priority;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.JointMask.BindModifier;
import yesman.epicfight.api.client.animation.property.JointMask.JointMaskSet;
import yesman.epicfight.api.client.animation.property.JointMaskEntry;
import yesman.epicfight.api.utils.datastructure.ParameterizedHashMap;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ClientAnimator extends Animator {
	public static Animator getAnimator(LivingEntityPatch<?> entitypatch) {
		return entitypatch.isLogicalClient() ? new ClientAnimator(entitypatch) : ServerAnimator.getAnimator(entitypatch);
	}
	
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> compositeLivingAnimations;
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultLivingAnimations;
	private final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> defaultCompositeLivingAnimations;
	public final Layer.BaseLayer baseLayer;
	private LivingMotion currentMotion;
	private LivingMotion currentCompositeMotion;
	private boolean hardPaused;
	
	public ClientAnimator(LivingEntityPatch<?> entitypatch) {
		this(entitypatch, Layer.BaseLayer::new);
	}
	
	public ClientAnimator(LivingEntityPatch<?> entitypatch, Supplier<Layer.BaseLayer> layerSupplier) {
		super(entitypatch);
		
		this.currentMotion = LivingMotions.IDLE;
		this.currentCompositeMotion = LivingMotions.IDLE;
		this.compositeLivingAnimations = Maps.newHashMap();
		this.defaultLivingAnimations = Maps.newHashMap();
		this.defaultCompositeLivingAnimations = Maps.newHashMap();
		this.baseLayer = layerSupplier.get();
	}
	
	/** Play an animation by animation instance **/
	@Override
	public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, float transitionTimeModifier) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		layer.paused = false;
		layer.playAnimation(nextAnimation, this.entitypatch, transitionTimeModifier);
	}
	
	/** Play an animation with specifying layer and priority **/
	@ApiStatus.Internal
	public void playAnimationAt(AssetAccessor<? extends StaticAnimation> nextAnimation, float transitionTimeModifier, AbstractAnimatorControl.Layer layerType, AbstractAnimatorControl.Priority priority) {
		Layer layer = layerType == AbstractAnimatorControl.Layer.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(AbstractAnimatorControl.getPriority(priority));
		layer.paused = false;
		layer.playAnimation(nextAnimation, this.entitypatch, transitionTimeModifier);
	}
	
	@Override
	public void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> nextAnimation) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		layer.paused  = false;
		layer.playAnimationInstantly(nextAnimation, this.entitypatch);
	}
	
	@Override
	public void reserveAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation) {
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		
		if (nextAnimation.get().getPriority().isHigherThan(layer.animationPlayer.getRealAnimation().get().getPriority())) {
			if (!layer.animationPlayer.isEnd() && layer.animationPlayer.getAnimation() != null) {
				layer.animationPlayer.getAnimation().get().end(this.entitypatch, nextAnimation, false);
			}
			
			layer.animationPlayer.terminate(this.entitypatch);
		}
		
		layer.nextAnimation = nextAnimation;
		layer.paused = false;
	}
	
	@Override
	public boolean stopPlaying(AssetAccessor<? extends StaticAnimation> targetAnimation) {
		Layer layer = targetAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(targetAnimation.get().getPriority());
		
		if (layer.animationPlayer.getRealAnimation() == targetAnimation) {
			layer.animationPlayer.terminate(this.entitypatch);
			return true;
		}
		
		return false;
	}
	
	@Override
	public void setSoftPause(boolean paused) {
		this.iterAllLayers(layer -> layer.paused = paused);
	}
	
	@Override
	public void setHardPause(boolean paused) {
		this.hardPaused = paused;
	}
	
	@Override
	public void addLivingAnimation(LivingMotion livingMotion, AssetAccessor<? extends StaticAnimation> animation) {
		if (AnimationManager.checkNull(animation)) {
			EpicFight.LOGGER.warn("Unable to put an empty animation for {}", livingMotion);
			return;
		}
		
		Layer.LayerType layerType = animation.get().getLayerType();
		boolean isBaseLayer = (layerType == Layer.LayerType.BASE_LAYER);
		
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> storage = layerType == Layer.LayerType.BASE_LAYER ? this.livingAnimations : this.compositeLivingAnimations;
		LivingMotion compareMotion = layerType == Layer.LayerType.BASE_LAYER ? this.currentMotion : this.currentCompositeMotion;
		Layer layer = layerType == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(animation.get().getPriority());
		storage.put(livingMotion, animation);
		
		if (livingMotion == compareMotion) {
			EntityState state = this.getEntityState();
			
			if (!state.inaction()) {
				layer.playLivingAnimation(animation, this.entitypatch);
			}
		}
		
		if (isBaseLayer) {
			animation.get().getProperty(ClientAnimationProperties.MULTILAYER_ANIMATION).ifPresent(multilayerAnimation -> {
				this.compositeLivingAnimations.put(livingMotion, multilayerAnimation);
				
				if (livingMotion == this.currentCompositeMotion) {
					EntityState state = this.getEntityState();
					
					if (!state.inaction()) {
						layer.playLivingAnimation(multilayerAnimation, this.entitypatch);
					}
				}
			});
		}
	}
	
	public void setCurrentMotionsAsDefault() {
		this.defaultLivingAnimations.putAll(this.livingAnimations);
		this.defaultCompositeLivingAnimations.putAll(this.compositeLivingAnimations);
	}
	
	@Override
	public void resetLivingAnimations() {
		super.resetLivingAnimations();
		this.compositeLivingAnimations.clear();
		this.defaultLivingAnimations.forEach(this::addLivingAnimation);
		this.defaultCompositeLivingAnimations.forEach(this::addLivingAnimation);
	}

	public AssetAccessor<? extends StaticAnimation> getLivingMotion(LivingMotion motion) {
		// SPChangeLivingMotion replaces the whole map with the item capability's modifier
		// set. When that set is empty or lacks both the queried motion and IDLE, every
		// animation lookup in tick() used to NPE and freeze the animator (stuck walk
		// animation). Fall back to biped idle instead of returning null.
		AssetAccessor<? extends StaticAnimation> animation =
			this.livingAnimations.getOrDefault(motion, this.livingAnimations.get(LivingMotions.IDLE));
		return animation != null ? animation : Animations.BIPED_IDLE;
	}
	
	public AssetAccessor<? extends StaticAnimation> getCompositeLivingMotion(LivingMotion motion) {
		return this.compositeLivingAnimations.get(motion);
	}
	
	@Override
	public void postInit() {
		super.postInit();
		
		this.setCurrentMotionsAsDefault();
		
		AssetAccessor<? extends StaticAnimation> idleMotion = this.livingAnimations.get(this.currentMotion);
		this.baseLayer.playAnimationInstantly(idleMotion, this.entitypatch);
	}
	
	@Override
	public void tick() {
		/*
		// Layer debugging
		for (Layer layer : this.getAllLayers()) {
			System.out.println(layer);
		}
		System.out.println();
		*/

		if (this.hardPaused) {
			return;
		}
		
		this.baseLayer.update(this.entitypatch);
		
		if (this.baseLayer.animationPlayer.isEnd() && this.baseLayer.nextAnimation == null && this.currentMotion != LivingMotions.DEATH) {
			this.entitypatch.updateMotion(false);
			
			if (this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
				this.playAnimation(this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion), 0.0F);
			}
			
			this.baseLayer.playAnimation(this.getLivingMotion(this.entitypatch.currentLivingMotion), this.entitypatch, 0.0F);
		} else {
			if (!this.compareCompositeMotion(this.entitypatch.currentCompositeMotion)) {
				/* Turns off the multilayer of the base layer */
				this.getLivingMotion(this.currentCompositeMotion).get().getProperty(ClientAnimationProperties.MULTILAYER_ANIMATION).ifPresent((multilayerAnimation) -> {
					if (!this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
						this.getCompositeLayer(multilayerAnimation.get().getPriority()).off(this.entitypatch);
					}
				});
				
				if (this.compositeLivingAnimations.containsKey(this.currentCompositeMotion)) {
					AssetAccessor<? extends StaticAnimation> nextLivingAnimation = this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion);
					
					if (nextLivingAnimation == null || nextLivingAnimation.get().getPriority() != this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()) {
						this.getCompositeLayer(this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()).off(this.entitypatch);
					}
				}
				
				if (this.compositeLivingAnimations.containsKey(this.entitypatch.currentCompositeMotion)) {
					this.playAnimation(this.getCompositeLivingMotion(this.entitypatch.currentCompositeMotion), 0.0F);
				}
			}
			
			if (!this.compareMotion(this.entitypatch.currentLivingMotion) && this.entitypatch.currentLivingMotion != LivingMotions.DEATH) {
				if (this.livingAnimations.containsKey(this.entitypatch.currentLivingMotion)) {
					this.baseLayer.playAnimation(this.getLivingMotion(this.entitypatch.currentLivingMotion), this.entitypatch, 0.0F);
				}
			}
		}
		
		this.currentMotion = this.entitypatch.currentLivingMotion;
		this.currentCompositeMotion = this.entitypatch.currentCompositeMotion;
	}
	
	@Override
	public void playDeathAnimation() {
		if (!this.getPlayerFor(null).getAnimation().get().getProperty(ActionAnimationProperty.IS_DEATH_ANIMATION).orElse(false)) {
			this.playAnimation(this.livingAnimations.getOrDefault(LivingMotions.DEATH, Animations.EMPTY_ANIMATION), 0.0F);
			this.currentMotion = LivingMotions.DEATH;
		}
	}
	
	public AssetAccessor<? extends StaticAnimation> getJumpAnimation() {
		return this.livingAnimations.get(LivingMotions.JUMP);
	}
	
	public Layer getCompositeLayer(Layer.Priority priority) {
		return this.baseLayer.compositeLayers.get(priority);
	}
	
	public void renderDebuggingInfoForAllLayers(PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
		this.iterAllLayers((layer) -> {
			if (layer.isOff()) {
				return;
			}
			
			AnimationPlayer animPlayer = layer.animationPlayer;
			float playTime = Mth.lerp(partialTicks, animPlayer.getPrevElapsedTime(), animPlayer.getElapsedTime());
			animPlayer.getAnimation().get().renderDebugging(poseStack, buffer, entitypatch, playTime, partialTicks);
		});
	}
	
	public Collection<Layer> getAllLayers() {
		List<Layer> layerList = Lists.newArrayList();
		layerList.add(this.baseLayer);
		layerList.addAll(this.baseLayer.compositeLayers.values());
		
		return layerList;
	}
	
	/**
	 * Iterates all layers
	 * @param task
	 */
	public void iterAllLayers(Consumer<Layer> task) {
		task.accept(this.baseLayer);
		this.baseLayer.compositeLayers.values().forEach(task);
	}
	
	/**
	 * Iterate layers that is visible by priority
	 * @param task
	 * @return
	 */
	public void iterVisibleLayers(Consumer<Layer> task) {
		task.accept(this.baseLayer);
		this.baseLayer.compositeLayers.values().stream()
			.filter(layer -> layer.isDisabled() || layer.animationPlayer.isEmpty() || !layer.priority.isHigherOrEqual(this.baseLayer.baseLayerPriority))
			.forEach(task);
	}
	
	/**
	 * Iterates all activated layers from the highest layer
	 * when base layer = highest, iterates only base layer
	 * when base layer = middle, iterates base layer and highest composite layer
	 * when base layer = lowest, iterates base layer and all composite layers
	 * 
	 * @param task
	 * @return true if all layers didn't return false by @param task
	 */
	public boolean iterVisibleLayersUntilFalse(Function<Layer, Boolean> task) {
		Layer.Priority[] highers = this.baseLayer.baseLayerPriority.highers();
		
		for (int i = highers.length - 1; i >= 0; i--) {
			Layer layer = this.baseLayer.getLayer(highers[i]);
			
			if (layer.isDisabled() || layer.animationPlayer.isEmpty()) {
				if (highers[i] == this.baseLayer.baseLayerPriority) {
					return task.apply(this.baseLayer);
				}
				
				continue;
			}
			
			if (!task.apply(layer)) {
				return false;
			}
			
			if (highers[i] == this.baseLayer.baseLayerPriority) {
				return task.apply(this.baseLayer);
			}
		}
		
		return true;
	}
	
	@Override
	public Pose getPose(float partialTicks) {
		float mirrorBlend = this.entitypatch.getMirrorBlend(partialTicks);
		Pose composedPose = this.getPose(partialTicks, true);

		if (mirrorBlend <= 0.0F) {
			return composedPose;
		}
		Pose mirrored = yesman.epicfight.api.animation.PoseMirror.mirrorPose(composedPose);
		if (mirrorBlend >= 1.0F) {
			return mirrored;
		}
		return Pose.interpolatePose(composedPose, mirrored, mirrorBlend);
	}

	public Pose getPose(float partialTicks, boolean useCurrentMotion) {
		Pose composedPose = new Pose();
		Pose baseLayerPose = this.baseLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks);

		Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> layerPoses = Maps.newLinkedHashMap();
		composedPose.load(baseLayerPose, Pose.LoadOperation.OVERWRITE);

		for (Layer.Priority priority : this.baseLayer.baseLayerPriority.highers()) {
			Layer compositeLayer = this.baseLayer.compositeLayers.get(priority);

			if (!compositeLayer.isDisabled() && !compositeLayer.animationPlayer.isEmpty()) {
				Pose layerPose = compositeLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks);
				layerPoses.put(priority, Pair.of(compositeLayer.animationPlayer.getAnimation(), layerPose));
				composedPose.load(layerPose, Pose.LoadOperation.OVERWRITE);
			}
		}

		Joint rootJoint = this.entitypatch.getArmature().rootJoint;
		this.applyBindModifier(baseLayerPose, composedPose, rootJoint, layerPoses, useCurrentMotion);

		return composedPose;
	}
	
	public Pose getComposedLayerPoseBelow(Layer.Priority priorityLimit, float partialTicks) {
		Pose composedPose = this.baseLayer.getEnabledPose(this.entitypatch, true, partialTicks);
		Pose baseLayerPose = this.baseLayer.getEnabledPose(this.entitypatch, true, partialTicks);
		Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> layerPoses = Maps.newLinkedHashMap();
		
		for (Layer.Priority priority : priorityLimit.lowers()) {
			Layer compositeLayer = this.baseLayer.compositeLayers.get(priority);
			
			if (!compositeLayer.isDisabled()) {
				Pose layerPose = compositeLayer.getEnabledPose(this.entitypatch, true, partialTicks);
				layerPoses.put(priority, Pair.of(compositeLayer.animationPlayer.getAnimation(), layerPose));
				composedPose.load(layerPose, Pose.LoadOperation.OVERWRITE);
			}
		}
		
		if (!layerPoses.isEmpty()) {
			this.applyBindModifier(baseLayerPose, composedPose, this.entitypatch.getArmature().rootJoint, layerPoses, true);
		}
		
		return composedPose;
	}
	
	public void applyBindModifier(Pose basePose, Pose result, Joint joint, Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> poses, boolean useCurrentMotion) {
		List<Priority> list = Lists.newArrayList(poses.keySet());
		Collections.reverse(list);
		
		for (Layer.Priority priority : list) {
			AssetAccessor<? extends DynamicAnimation> nowPlaying = poses.get(priority).getFirst();
			JointMaskEntry jointMaskEntry = nowPlaying.get().getJointMaskEntry(this.entitypatch, useCurrentMotion).orElse(null);
			
			if (jointMaskEntry != null) {
				LivingMotion livingMotion = this.getCompositeLayer(priority).getLivingMotion(this.entitypatch, useCurrentMotion);
				
				if (nowPlaying.get().hasTransformFor(joint.getName()) && !jointMaskEntry.isMasked(livingMotion, joint.getName())) {
					JointMaskSet jointmaskset = jointMaskEntry.getMask(livingMotion);
					BindModifier bindModifier = jointmaskset.getBindModifier(joint.getName());
					
					if (bindModifier != null) {
						bindModifier.modify(this.entitypatch, basePose, result, livingMotion, jointMaskEntry, priority, joint, poses);
						break;
					}
				}
			}
		}
		
		for (Joint subJoints : joint.getSubJoints()) {
			this.applyBindModifier(basePose, result, subJoints, poses, useCurrentMotion);
		}
	}
	
	public boolean compareMotion(LivingMotion motion) {
		return this.currentMotion.isSame(motion);
	}
	
	public boolean compareCompositeMotion(LivingMotion motion) {
		return this.currentCompositeMotion.isSame(motion);
	}
	
	public void forceResetBeforeAction(LivingMotion livingMotion, LivingMotion compositeLivingMotion) {
		if (!this.currentMotion.equals(livingMotion)) {
			if (this.livingAnimations.containsKey(livingMotion)) {
				this.baseLayer.playAnimation(this.getLivingMotion(livingMotion), this.entitypatch, 0.0F);
			}
		}
		
		this.entitypatch.currentLivingMotion = livingMotion;
		this.currentMotion = livingMotion;
		
		if (!this.currentCompositeMotion.equals(compositeLivingMotion)) {
			if (this.compositeLivingAnimations.containsKey(this.currentCompositeMotion)) {
				this.getCompositeLayer(this.getCompositeLivingMotion(this.currentCompositeMotion).get().getPriority()).off(this.entitypatch);
			}
			
			if (this.compositeLivingAnimations.containsKey(compositeLivingMotion)) {
				this.playAnimation(this.getCompositeLivingMotion(compositeLivingMotion), 0.0F);
			}
		}
		
		this.currentCompositeMotion = LivingMotions.NONE;
		this.entitypatch.currentCompositeMotion = LivingMotions.NONE;
	}
	
	public void resetMotion(boolean resetPrevMotion) {
		if (resetPrevMotion) this.currentMotion = LivingMotions.IDLE;
		this.entitypatch.currentLivingMotion = LivingMotions.IDLE;
	}
	
	public void resetCompositeMotion() {
		if (this.currentCompositeMotion != LivingMotions.IDLE && this.compositeLivingAnimations.containsKey(this.currentCompositeMotion)) {
			AssetAccessor<? extends StaticAnimation> currentPlaying = this.getCompositeLivingMotion(this.currentCompositeMotion);
			AssetAccessor<? extends StaticAnimation> resetPlaying = this.getCompositeLivingMotion(LivingMotions.IDLE);
			
			if (resetPlaying != null && currentPlaying != resetPlaying) {
				this.playAnimation(resetPlaying, 0.0F);
			} else if (currentPlaying != null) {
				this.getCompositeLayer(currentPlaying.get().getPriority()).off(this.entitypatch);
			}
		}
		
		this.currentCompositeMotion = LivingMotions.NONE;
		this.entitypatch.currentCompositeMotion = LivingMotions.NONE;
	}
	
	public void offAllLayers() {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			layer.off(this.entitypatch);
		}
	}
	
	public boolean isAiming() {
		return this.currentCompositeMotion == LivingMotions.AIM;
	}
	
	@Override
	public void playShootingAnimation() {
		if (this.compositeLivingAnimations.containsKey(LivingMotions.SHOT)) {
			this.playAnimation(this.compositeLivingAnimations.get(LivingMotions.SHOT), 0.0F);
			this.entitypatch.currentCompositeMotion = LivingMotions.NONE;
			this.currentCompositeMotion = LivingMotions.NONE;
		}
	}
	
	@Override
	public boolean isPlaying(AssetAccessor<? extends DynamicAnimation> animation) {
		for (Layer layer : this.getAllLayers()) {
			if (layer.animationPlayer.getRealAnimation().equals(animation)) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public AnimationPlayer getPlayerFor(AssetAccessor<? extends DynamicAnimation> playingAnimation) {
		if (playingAnimation == null) {
			return this.baseLayer.animationPlayer;
		}
		
		DynamicAnimation animation = playingAnimation.get();
		
		if (animation instanceof StaticAnimation staticAnimation) {
			Layer layer = staticAnimation.getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(staticAnimation.getPriority());
            if (layer.animationPlayer.getAnimation() == playingAnimation) return layer.animationPlayer;
		}

        for (Layer layer : this.baseLayer.compositeLayers.values()) {
            if (layer.animationPlayer.getRealAnimation().equals(playingAnimation)) {
                return layer.animationPlayer;
            }
        }

		return this.baseLayer.animationPlayer;
	}
	
	@Override
	public Optional<AnimationPlayer> getPlayer(AssetAccessor<? extends DynamicAnimation> playingAnimation) {
        DynamicAnimation animation = playingAnimation.get();

        if (animation instanceof StaticAnimation staticAnimation) {
            Layer layer = staticAnimation.getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(staticAnimation.getPriority());

            if (layer.animationPlayer.getRealAnimation().equals(playingAnimation)) {
                return Optional.of(layer.animationPlayer);
            }
        }

        if (this.baseLayer.animationPlayer.getRealAnimation().equals(playingAnimation.get().getRealAnimation())) {
            return Optional.of(this.baseLayer.animationPlayer);
        }

        for (Layer layer : this.baseLayer.compositeLayers.values()) {
            if (layer.animationPlayer.getRealAnimation().equals(playingAnimation.get().getRealAnimation())) {
                return Optional.of(layer.animationPlayer);
            }
        }

        return Optional.empty();
	}
	
	public Layer.Priority getPriorityFor(AssetAccessor<? extends DynamicAnimation> playingAnimation) {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (layer.animationPlayer.getRealAnimation().equals(playingAnimation)) {
				return layer.priority;
			}
		}
		
		return this.baseLayer.priority;
	}
	
	public LivingMotion currentMotion() {
		return this.currentMotion;
	}
	
	public LivingMotion currentCompositeMotion() {
		return this.currentCompositeMotion;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> Pair<AnimationPlayer, T> findFor(Class<T> animationType) {
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (animationType.isAssignableFrom(layer.animationPlayer.getAnimation().getClass())) {
				return Pair.of(layer.animationPlayer, (T)layer.animationPlayer.getAnimation());
			}
		}
		
		return animationType.isAssignableFrom(this.baseLayer.animationPlayer.getAnimation().getClass()) ? Pair.of(this.baseLayer.animationPlayer, (T)this.baseLayer.animationPlayer.getAnimation()) : null;
	}
	
	public LivingEntityPatch<?> getOwner() {
		return this.entitypatch;
	}
	
	@Override
	public EntityState getEntityState() {
		ParameterizedHashMap<StateFactor<?>> stateMap = new ParameterizedHashMap<> ();

		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			if (this.baseLayer.baseLayerPriority.isHigherThan(layer.priority)) {
				continue;
			}

			if (!layer.isOff()) {
				stateMap.putAll(layer.animationPlayer.getAnimation().get().getStatesMap(this.entitypatch, layer.animationPlayer.getElapsedTime()));
			}

			// put base layer states
			if (layer.priority == this.baseLayer.baseLayerPriority) {
				stateMap.putAll(this.baseLayer.animationPlayer.getAnimation().get().getStatesMap(this.entitypatch, this.baseLayer.animationPlayer.getElapsedTime()));
			}
		}

		return new EntityState(stateMap);
	}
}