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
import yesman.epicfight.api.model.Armature;
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

	/**
	 * Pose-evaluation pooling. {@link #poseCache} memoizes composed poses keyed by
	 * (partialTick, useCurrentMotion/kind) and is invalidated whenever
	 * {@link #poseStateFingerprint()} changes (i.e. once per animator tick plus any
	 * mid-tick state flip). Evicted poses go to {@link #poseFreeList} and are refilled
	 * in place, so their {@link JointTransform} objects keep being reused.
	 * {@link #evalPosePool} holds short-lived poses used only inside a single
	 * evaluation (per-layer poses, mirror intermediates).
	 */
	private final Deque<Pose> evalPosePool = new ArrayDeque<>(8);
	private final Deque<Pose> poseFreeList = new ArrayDeque<>(8);
	private final Map<Long, Pose> poseCache = new HashMap<>();
	private int poseFingerprint;
	/**
	 * Per-evaluation scratch, pooled in a deque rather than stored in fields: pose
	 * evaluation is reentrant ({@code LayerOffAnimation#getPoseByTime} calls
	 * {@link #getComposedLayerPoseBelow} while an outer evaluation is still
	 * gathering layer poses), so each evaluation acquires its own context.
	 */
	private final Deque<EvalScratch> evalScratchPool = new ArrayDeque<>(4);

	private static final class EvalScratch {
		final Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> layerPoses = new LinkedHashMap<>();
		final Set<String> jointUnion = new HashSet<>();
		final List<BindLayerEntry> bindLayerEntries = new ArrayList<>(8);
		int bindLayerCount;
	}

	private static final class BindLayerEntry {
		Layer.Priority priority;
		AssetAccessor<? extends DynamicAnimation> animation;
		JointMaskEntry jointMaskEntry;
		LivingMotion livingMotion;
		JointMaskSet maskSet;
	}

	/** Debug switches: -Depicfight.poseCache=true re-enables {@link #poseCache} (off by
	 *  default while the idle-regression is being isolated),
	 *  -Depicfight.animDebug=true prints base-layer/animator state once per second. */
	private static final boolean POSE_CACHE = Boolean.getBoolean("epicfight.poseCache");
	private static final boolean ANIM_DEBUG = Boolean.getBoolean("epicfight.animDebug");
	private int animDebugTicks;
	
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
		if (AnimationManager.checkNull(nextAnimation)) {
			EpicFight.LOGGER.warn("Skipping playback of an empty animation ({})", nextAnimation == null ? "null" : nextAnimation.registryName());
			return;
		}
		
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
		if (AnimationManager.checkNull(nextAnimation)) {
			EpicFight.LOGGER.warn("Skipping instant playback of an empty animation ({})", nextAnimation == null ? "null" : nextAnimation.registryName());
			return;
		}
		
		Layer layer = nextAnimation.get().getLayerType() == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(nextAnimation.get().getPriority());
		layer.paused  = false;
		layer.playAnimationInstantly(nextAnimation, this.entitypatch);
	}
	
	@Override
	public void reserveAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation) {
		if (AnimationManager.checkNull(nextAnimation)) {
			EpicFight.LOGGER.warn("Skipping reservation of an empty animation ({})", nextAnimation == null ? "null" : nextAnimation.registryName());
			return;
		}
		
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
		if (AnimationManager.checkNull(targetAnimation)) {
			return false;
		}
		
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
			EpicFight.LOGGER.warn("Unable to put an empty animation for {}: {}", livingMotion, animation == null ? "null" : animation.registryName());
			return;
		}
		
		Layer.LayerType layerType = animation.get().getLayerType();
		boolean isBaseLayer = (layerType == Layer.LayerType.BASE_LAYER);
		
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> storage = layerType == Layer.LayerType.BASE_LAYER ? this.livingAnimations : this.compositeLivingAnimations;
		LivingMotion compareMotion = layerType == Layer.LayerType.BASE_LAYER ? this.currentMotion : this.currentCompositeMotion;
		Layer layer = layerType == Layer.LayerType.BASE_LAYER ? this.baseLayer : this.baseLayer.compositeLayers.get(animation.get().getPriority());
		AssetAccessor<? extends StaticAnimation> previousAnimation = storage.get(livingMotion);
		storage.put(livingMotion, animation);
		
		if (livingMotion == compareMotion && previousAnimation != animation) {
			EntityState state = this.getEntityState();
			
			if (!state.inaction()) {
				layer.playLivingAnimation(animation, this.entitypatch);
			}
		}
		
		if (isBaseLayer) {
			animation.get().getProperty(ClientAnimationProperties.MULTILAYER_ANIMATION).ifPresent(multilayerAnimation -> {
				AssetAccessor<? extends StaticAnimation> previousComposite = this.compositeLivingAnimations.get(livingMotion);
				this.compositeLivingAnimations.put(livingMotion, multilayerAnimation);
				
				if (livingMotion == this.currentCompositeMotion && previousComposite != multilayerAnimation) {
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

	public Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> getDefaultLivingAnimations() {
		return this.defaultLivingAnimations;
	}

	public Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> getDefaultCompositeLivingAnimations() {
		return this.defaultCompositeLivingAnimations;
	}

	public Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> getCompositeLivingAnimations() {
		return this.compositeLivingAnimations;
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

		if (ANIM_DEBUG && this.entitypatch.getOriginal() == net.minecraft.client.Minecraft.getInstance().player && ++this.animDebugTicks % 20 == 0) {
			AnimationPlayer p = this.baseLayer.animationPlayer;
			Pose sample = this.baseLayer.getEnabledPose(this.entitypatch, true, 1.0F);
			EpicFight.LOGGER.info(
				"[ANIMDBG] entity={} lm={}/{} cm={}/{} baseAnim={} elapsed={}/{} end={} paused={} next={} living={} compLiving={} basePoseJoints={}",
				this.entitypatch.getOriginal() != null ? this.entitypatch.getOriginal().getName().getString() : "null",
				this.entitypatch.currentLivingMotion, this.currentMotion,
				this.entitypatch.currentCompositeMotion, this.currentCompositeMotion,
				p.getAnimation(), p.getElapsedTime(), p.getAnimation().get().getTotalTime(), p.isEnd(),
				this.baseLayer.paused, this.baseLayer.nextAnimation,
				this.livingAnimations.size(), this.compositeLivingAnimations.size(),
				sample.getJointTransformData().size()
			);
		}
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
		if (!POSE_CACHE) {
			float mirrorBlend = this.entitypatch.getMirrorBlend(partialTicks);
			Pose composedPose = this.buildComposedPose(partialTicks, true);

			if (mirrorBlend <= 0.0F) {
				return composedPose;
			}

			Pose mirrored = this.acquireEvalPose();
			PoseMirror.mirrorPose(composedPose, mirrored, false);
			Pose result = new Pose();
			Pose.interpolatePose(composedPose, mirrored, mirrorBlend, result);
			this.releaseEvalPose(mirrored);

			return result;
		}

		this.ensurePoseCacheFresh();
		long key = ((long)Float.floatToRawIntBits(partialTicks) << 2) | 2L;
		Pose cached = this.poseCache.get(key);

		if (cached != null) {
			return cached;
		}

		float mirrorBlend = this.entitypatch.getMirrorBlend(partialTicks);
		Pose composedPose = this.getPose(partialTicks, true);
		Pose result = composedPose;

		if (mirrorBlend > 0.0F) {
			Pose mirrored = this.acquireEvalPose();
			PoseMirror.mirrorPose(composedPose, mirrored, false);
			result = this.poseFreeList.poll();

			if (result == null) {
				result = new Pose();
			}

			Pose.interpolatePose(composedPose, mirrored, mirrorBlend, result);
			this.releaseEvalPose(mirrored);
		}

		this.poseCache.put(key, result);
		return result;
	}

	public Pose getPose(float partialTicks, boolean useCurrentMotion) {
		if (!POSE_CACHE) {
			return this.buildComposedPose(partialTicks, useCurrentMotion);
		}

		this.ensurePoseCacheFresh();
		long key = ((long)Float.floatToRawIntBits(partialTicks) << 2) | (useCurrentMotion ? 1L : 0L);
		Pose cached = this.poseCache.get(key);

		if (cached != null) {
			return cached;
		}

		Pose composedPose = this.buildComposedPose(partialTicks, useCurrentMotion);
		this.poseCache.put(key, composedPose);

		return composedPose;
	}
	
	/**
	 * Invalidates {@link #poseCache} when animator/entity state moved on since the
	 * last evaluation; evicted poses are recycled into {@link #poseFreeList}.
	 */
	private void ensurePoseCacheFresh() {
		int fingerprint = this.poseStateFingerprint();
		
		if (fingerprint != this.poseFingerprint) {
			this.poseFingerprint = fingerprint;
			
			for (Pose pose : this.poseCache.values()) {
				if (this.poseFreeList.size() < 8 && !this.poseFreeList.contains(pose)) {
					this.poseFreeList.push(pose);
				}
			}
			
			this.poseCache.clear();
		}
	}
	
	/**
	 * A cheap fingerprint of every input that can change the composed pose between
	 * two evaluations at the same partial tick: playing animations and their clocks
	 * on every layer, layer/motion state, and the entity rotations that feed
	 * pose-tick modifiers (head/body tracking, attack pitch, mirror blend).
	 */
	private int poseStateFingerprint() {
		// Entityless patches (ModelPreviewer's FakeEntityPatch) have no original —
		// skip the entity-derived inputs; the layer fingerprints below still
		// produce a valid cache key for the preview animator.
		net.minecraft.world.entity.LivingEntity entity = this.entitypatch.getOriginal();
		int h = entity == null ? 0 :
				Float.floatToRawIntBits(entity.yHeadRot)
				+ 31 * Float.floatToRawIntBits(entity.yHeadRotO)
				+ 31 * Float.floatToRawIntBits(entity.yBodyRot)
				+ 31 * Float.floatToRawIntBits(entity.yBodyRotO)
				+ 31 * Float.floatToRawIntBits(entity.getXRot())
				+ 31 * Float.floatToRawIntBits(entity.xRotO);
		h = 31 * h + Float.floatToRawIntBits(this.entitypatch.getAttackDirectionPitch(1.0F));
		h = 31 * h + Float.floatToRawIntBits(this.entitypatch.getMirrorBlend(1.0F));

		if (entity != null) {
			h = 31 * h + (entity.isUsingItem() ? 1 : 0) + (entity.isSprinting() ? 2 : 0)
					+ (entity.isCrouching() ? 4 : 0) + (entity.isPassenger() ? 8 : 0);
			h = 31 * h + System.identityHashCode(entity.getMainHandItem().getItem());
			h = 31 * h + System.identityHashCode(entity.getOffhandItem().getItem());
		}
		h = 31 * h + this.entitypatch.currentLivingMotion.hashCode();
		h = 31 * h + this.entitypatch.currentCompositeMotion.hashCode();
		h = 31 * h + this.layerFingerprint(this.baseLayer);
		
		for (Layer layer : this.baseLayer.compositeLayers.values()) {
			h = 31 * h + this.layerFingerprint(layer);
		}
		
		return h;
	}
	
	private int layerFingerprint(Layer layer) {
		AnimationPlayer player = layer.animationPlayer;
		int h = System.identityHashCode(player.getAnimation());
		h = 31 * h + Float.floatToRawIntBits(player.getElapsedTime());
		h = 31 * h + Float.floatToRawIntBits(player.getPrevElapsedTime());
		h = 31 * h + (player.isEnd() ? 1 : 0);
		h = 31 * h + (player.isReversed() ? 2 : 0);
		h = 31 * h + (layer.paused ? 4 : 0);
		h = 31 * h + (layer.isDisabled() ? 8 : 0);
		h = 31 * h + System.identityHashCode(layer.nextAnimation);
		h = 31 * h + Float.floatToRawIntBits(player.getAnimation().get().getTotalTime());
		
		return h;
	}
	
	private Pose acquireEvalPose() {
		Pose pose = this.evalPosePool.poll();
		return pose == null ? new Pose() : pose;
	}
	
	private void releaseEvalPose(Pose pose) {
		if (this.evalPosePool.size() < 8) {
			this.evalPosePool.push(pose);
		}
	}
	
	/** Collects the per-layer poses for one evaluation into {@link #layerPosesScratch}
	 *  (values are pooled poses that must be released by the caller afterwards). */
	private void gatherLayerPoses(boolean useCurrentMotion, float partialTicks, Layer.Priority[] priorities, boolean skipEmpty, EvalScratch scratch) {
		for (Layer.Priority priority : priorities) {
			Layer compositeLayer = this.baseLayer.compositeLayers.get(priority);
			
			if (!compositeLayer.isDisabled() && (!skipEmpty || !compositeLayer.animationPlayer.isEmpty())) {
				Pose layerPose = this.acquireEvalPose();
				compositeLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks, layerPose);
				scratch.layerPoses.put(priority, Pair.of(compositeLayer.animationPlayer.getAnimation(), layerPose));
			}
		}
	}

	private EvalScratch acquireEvalScratch() {
		EvalScratch scratch = this.evalScratchPool.poll();
		return scratch == null ? new EvalScratch() : scratch;
	}

	private void releaseEvalScratch(EvalScratch scratch) {
		if (this.evalScratchPool.size() < 4) {
			this.evalScratchPool.push(scratch);
		}
	}
	
	private Pose buildComposedPose(float partialTicks, boolean useCurrentMotion) {
		EvalScratch scratch = this.acquireEvalScratch();
		Pose baseLayerPose = this.acquireEvalPose();
		Pose composedPose = this.poseFreeList.poll();
		
		if (composedPose == null) {
			composedPose = new Pose();
		}
		
		scratch.layerPoses.clear();
		scratch.jointUnion.clear();
		this.baseLayer.getEnabledPose(this.entitypatch, useCurrentMotion, partialTicks, baseLayerPose);
		scratch.jointUnion.addAll(baseLayerPose.getJointTransformData().keySet());
		this.gatherLayerPoses(useCurrentMotion, partialTicks, this.baseLayer.baseLayerPriority.highers(), true, scratch);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			scratch.jointUnion.addAll(pair.getSecond().getJointTransformData().keySet());
		}
		
		composedPose.retainJoints(scratch.jointUnion);
		composedPose.loadDeep(baseLayerPose);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			composedPose.loadDeep(pair.getSecond());
		}
		
		this.applyBindModifier(baseLayerPose, composedPose, this.entitypatch.getArmature().getJointsInDFSOrder(), scratch.layerPoses, useCurrentMotion);
		
		this.releaseEvalPose(baseLayerPose);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			this.releaseEvalPose(pair.getSecond());
		}
		
		this.releaseEvalScratch(scratch);
		return composedPose;
	}
	
	public Pose getComposedLayerPoseBelow(Layer.Priority priorityLimit, float partialTicks) {
		return this.getComposedLayerPoseBelow(priorityLimit, partialTicks, new Pose());
	}
	
	/** {@link #getComposedLayerPoseBelow(Layer.Priority, float)} writing into {@code dest}. */
	public Pose getComposedLayerPoseBelow(Layer.Priority priorityLimit, float partialTicks, Pose dest) {
		EvalScratch scratch = this.acquireEvalScratch();
		Pose baseLayerPose = this.acquireEvalPose();
		
		scratch.layerPoses.clear();
		scratch.jointUnion.clear();
		this.baseLayer.getEnabledPose(this.entitypatch, true, partialTicks, baseLayerPose);
		scratch.jointUnion.addAll(baseLayerPose.getJointTransformData().keySet());
		this.gatherLayerPoses(true, partialTicks, priorityLimit.lowers(), false, scratch);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			scratch.jointUnion.addAll(pair.getSecond().getJointTransformData().keySet());
		}
		
		dest.retainJoints(scratch.jointUnion);
		dest.loadDeep(baseLayerPose);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			dest.loadDeep(pair.getSecond());
		}
		
		if (!scratch.layerPoses.isEmpty()) {
			this.applyBindModifier(baseLayerPose, dest, this.entitypatch.getArmature().getJointsInDFSOrder(), scratch.layerPoses, true);
		}
		
		this.releaseEvalPose(baseLayerPose);
		
		for (Pair<AssetAccessor<? extends DynamicAnimation>, Pose> pair : scratch.layerPoses.values()) {
			this.releaseEvalPose(pair.getSecond());
		}
		
		this.releaseEvalScratch(scratch);
		return dest;
	}
	
	public void applyBindModifier(Pose basePose, Pose result, Joint joint, Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> poses, boolean useCurrentMotion) {
		this.applyBindModifier(basePose, result, this.entitypatch.getArmature().getJointsInDFSOrder(), poses, useCurrentMotion);
	}
	
	/**
	 * Depth-first walk over the armature's fixed joint order (computed once by
	 * {@link Armature#getJointsInDFSOrder()}), applying each joint's bind modifier
	 * from the highest-priority masked layer that provides one. Mask entry, living
	 * motion and mask set are joint-independent, so they are resolved once per
	 * layer instead of once per joint.
	 */
	public void applyBindModifier(Pose basePose, Pose result, List<Joint> joints, Map<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> poses, boolean useCurrentMotion) {
		EvalScratch scratch = this.acquireEvalScratch();
		scratch.bindLayerCount = 0;
		
		for (Map.Entry<Layer.Priority, Pair<AssetAccessor<? extends DynamicAnimation>, Pose>> entry : poses.entrySet()) {
			AssetAccessor<? extends DynamicAnimation> nowPlaying = entry.getValue().getFirst();
			JointMaskEntry jointMaskEntry = nowPlaying.get().getJointMaskEntry(this.entitypatch, useCurrentMotion).orElse(null);
			
			if (jointMaskEntry != null) {
				BindLayerEntry bindLayer = this.bindLayerEntry(scratch, scratch.bindLayerCount++);
				bindLayer.priority = entry.getKey();
				bindLayer.animation = nowPlaying;
				bindLayer.jointMaskEntry = jointMaskEntry;
				bindLayer.livingMotion = this.getCompositeLayer(bindLayer.priority).getLivingMotion(this.entitypatch, useCurrentMotion);
				bindLayer.maskSet = jointMaskEntry.getMask(bindLayer.livingMotion);
			}
		}
		
		for (Joint joint : joints) {
			for (int i = scratch.bindLayerCount - 1; i >= 0; i--) {
				BindLayerEntry bindLayer = scratch.bindLayerEntries.get(i);
				
				if (bindLayer.animation.get().hasTransformFor(joint.getName()) && !bindLayer.jointMaskEntry.isMasked(bindLayer.livingMotion, joint.getName())) {
					BindModifier bindModifier = bindLayer.maskSet.getBindModifier(joint.getName());
					
					if (bindModifier != null) {
						bindModifier.modify(this.entitypatch, basePose, result, bindLayer.livingMotion, bindLayer.jointMaskEntry, bindLayer.priority, joint, poses);
						break;
					}
				}
			}
		}
		
		this.releaseEvalScratch(scratch);
	}
	
	private BindLayerEntry bindLayerEntry(EvalScratch scratch, int index) {
		while (scratch.bindLayerEntries.size() <= index) {
			scratch.bindLayerEntries.add(new BindLayerEntry());
		}
		
		return scratch.bindLayerEntries.get(index);
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
				stateMap.putAll(layer.animationPlayer.getStatesMap(this.entitypatch));
			}

			// put base layer states
			if (layer.priority == this.baseLayer.baseLayerPriority) {
				stateMap.putAll(this.baseLayer.animationPlayer.getStatesMap(this.entitypatch));
			}
		}

		return new EntityState(stateMap);
	}
}