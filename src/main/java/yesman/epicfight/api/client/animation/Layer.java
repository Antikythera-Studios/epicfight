package yesman.epicfight.api.client.animation;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Supplier;

import com.google.common.collect.Maps;

import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.ConcurrentLinkAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.LayerOffAnimation;
import yesman.epicfight.api.animation.types.LinkAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class Layer {
	protected AssetAccessor<? extends StaticAnimation> nextAnimation;
	protected final LinkAnimation linkAnimation;
	protected final ConcurrentLinkAnimation concurrentLinkAnimation;
	protected final LayerOffAnimation layerOffAnimation;
	protected final Layer.Priority priority;
	protected boolean disabled;
	protected boolean paused;
	public final AnimationPlayer animationPlayer;

	public Layer(Priority priority) {
		this(priority, AnimationPlayer::new);
	}
	
	public Layer(Priority priority, Supplier<AnimationPlayer> animationPlayerProvider) {
		this.animationPlayer = animationPlayerProvider.get();
		this.linkAnimation = new LinkAnimation();
		this.concurrentLinkAnimation = new ConcurrentLinkAnimation();
		this.layerOffAnimation = new LayerOffAnimation(priority);
		this.priority = priority;
		this.disabled = true;
	}
	
	private static final boolean PA_DEBUG = Boolean.getBoolean("epicfight.animDebug");

	public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, float transitionTimeModifier) {
		if (PA_DEBUG && entitypatch.getOriginal() == net.minecraft.client.Minecraft.getInstance().player) {
			String animName = String.valueOf(nextAnimation.registryName());
			if (animName.contains("creative_")) {
				StackTraceElement[] st = Thread.currentThread().getStackTrace();
				StringBuilder sb = new StringBuilder();
				for (int i = 2; i < Math.min(st.length, 9); i++) {
					sb.append(" <- ").append(st[i].getClassName()).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
				}
				yesman.epicfight.EpicFight.LOGGER.info("[PA] {}{}", animName, sb);
			}
		}

		// Get pose before StaticAnimation#end is called
		Pose lastPose = this.getCurrentPose(entitypatch);

		if (!this.animationPlayer.isEnd()) {
			this.animationPlayer.getAnimation().get().end(entitypatch, nextAnimation, false);
		}

		this.resume();
		nextAnimation.get().begin(entitypatch);

		if (!nextAnimation.get().isMetaAnimation()) {
			this.setLinkAnimation(nextAnimation, entitypatch, lastPose, transitionTimeModifier);
			this.linkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
			entitypatch.updateEntityState();
			this.nextAnimation = nextAnimation;
		}
	}

	/**
	 * Plays an animation without a link animation
	 */
	public void playAnimationInstantly(AssetAccessor<? extends DynamicAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
		if (!this.animationPlayer.isEnd()) {
			this.animationPlayer.getAnimation().get().end(entitypatch, nextAnimation, false);
		}

		this.resume();

		nextAnimation.get().begin(entitypatch);
		nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
		entitypatch.updateEntityState();
		this.nextAnimation = null;
	}

	protected void playLivingAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
		if (PA_DEBUG && entitypatch.getOriginal() == net.minecraft.client.Minecraft.getInstance().player) {
			String animName = String.valueOf(nextAnimation.registryName());
			if (animName.contains("creative_")) {
				StackTraceElement[] st = Thread.currentThread().getStackTrace();
				StringBuilder sb = new StringBuilder();
				for (int i = 2; i < Math.min(st.length, 9); i++) {
					sb.append(" <- ").append(st[i].getClassName()).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
				}
				yesman.epicfight.EpicFight.LOGGER.info("[PA-L] {}{}", animName, sb);
			}
		}
		if (!this.animationPlayer.isEnd()) {
			this.animationPlayer.getAnimation().get().end(entitypatch, nextAnimation, false);
		}

		this.resume();
		nextAnimation.get().begin(entitypatch);

		if (!nextAnimation.get().isMetaAnimation()) {
			this.concurrentLinkAnimation.acceptFrom(this.animationPlayer.getRealAnimation(), nextAnimation, this.animationPlayer.getElapsedTime());
			this.concurrentLinkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
			entitypatch.updateEntityState();
			this.nextAnimation = nextAnimation;
		}
	}
	
	protected Pose getCurrentPose(LivingEntityPatch<?> entitypatch) {
		return entitypatch.getClientAnimator().getPose(0.0F, false);
	}
	
	protected void setLinkAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, Pose lastPose, float transitionTimeModifier) {
		AssetAccessor<? extends DynamicAnimation> fromAnimation = this.animationPlayer.isEmpty() ? entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation() : this.animationPlayer.getAnimation();
		
		if (fromAnimation.get() instanceof LinkAnimation linkAnimation) {
			fromAnimation = linkAnimation.getFromAnimation();
		}
		
		nextAnimation.get().setLinkAnimation(fromAnimation, lastPose, !this.animationPlayer.isEmpty(), transitionTimeModifier, entitypatch, this.linkAnimation);
		this.linkAnimation.getAnimationClip().setBaked();
	}
	
	public void update(LivingEntityPatch<?> entitypatch) {
		if (this.paused) {
			this.animationPlayer.setElapsedTime(this.animationPlayer.getElapsedTime());
		} else {
			this.animationPlayer.tick(entitypatch);
		}
		
		if (!this.animationPlayer.isEnd()) {
			this.animationPlayer.getAnimation().get().tick(entitypatch);
		} else if (!this.paused) {
			if (this.nextAnimation != null) {
				if (!this.animationPlayer.getAnimation().get().isLinkAnimation() && !this.nextAnimation.get().isLinkAnimation()) {
					this.nextAnimation.get().begin(entitypatch);
				}

				this.nextAnimation.get().putOnPlayer(this.animationPlayer, entitypatch);
				this.nextAnimation = null;
			} else {
				if (this.animationPlayer.getAnimation() instanceof LayerOffAnimation) {
					this.animationPlayer.getAnimation().get().end(entitypatch, Animations.EMPTY_ANIMATION, true);
				} else {
					this.off(entitypatch);
				}
			}
		}
		
		if (this.isBaseLayer()) {
			entitypatch.updateEntityState();
			entitypatch.updateMotion(true);
		}
	}
	
	public void pause() {
		this.paused = true;
	}
	
	public void resume() {
		this.paused = false;
		this.disabled = false;
	}
	
	protected boolean isDisabled() {
		return this.disabled;
	}
	
	public boolean isOff() {
		return this.isDisabled() || this.animationPlayer.isEmpty();
	}
	
	protected boolean isBaseLayer() {
		return false;
	}
	
	public void copyLayerTo(Layer layer, float playbackTime) {
		AssetAccessor<? extends DynamicAnimation> animation;
		
		if (this.animationPlayer.getAnimation() == this.linkAnimation) {
			this.linkAnimation.copyTo(layer.linkAnimation);
			animation = layer.linkAnimation;
		} else {
			animation = this.animationPlayer.getAnimation();
		}
		
		layer.animationPlayer.setPlayAnimation(animation);
		layer.animationPlayer.setElapsedTime(this.animationPlayer.getPrevElapsedTime() + playbackTime, this.animationPlayer.getElapsedTime() + playbackTime);
		layer.nextAnimation = this.nextAnimation;
		layer.resume();
	}
	
	public LivingMotion getLivingMotion(LivingEntityPatch<?> entitypatch, boolean current) {
		return current ? entitypatch.currentLivingMotion : entitypatch.getClientAnimator().currentMotion();
	}
	
	public Pose getEnabledPose(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion, float partialTick) {
		return this.getEnabledPose(entitypatch, useCurrentMotion, partialTick, new Pose());
	}

	/** {@link #getEnabledPose(LivingEntityPatch, boolean, float)} writing into {@code pose},
	 *  reusing its stored joint transforms. */
	public Pose getEnabledPose(LivingEntityPatch<?> entitypatch, boolean useCurrentMotion, float partialTick, Pose pose) {
		this.animationPlayer.getCurrentPose(entitypatch, partialTick, pose);
		this.animationPlayer.getAnimation().get().getJointMaskEntry(entitypatch, useCurrentMotion).ifPresent((jointEntry) -> pose.disableJoint((entry) -> jointEntry.isMasked(this.getLivingMotion(entitypatch, useCurrentMotion), entry.getKey())));

		return pose;
	}

	public void off(LivingEntityPatch<?> entitypatch) {
		if (PA_DEBUG && entitypatch.getOriginal() == net.minecraft.client.Minecraft.getInstance().player) {
			StackTraceElement[] st = Thread.currentThread().getStackTrace();
			StringBuilder sb = new StringBuilder();
			for (int i = 2; i < Math.min(st.length, 10); i++) {
				sb.append(" <- ").append(st[i].getClassName()).append('.').append(st[i].getMethodName()).append(':').append(st[i].getLineNumber());
			}
			yesman.epicfight.EpicFight.LOGGER.info("[OFF] base={} anim={}{}", this.isBaseLayer(), this.animationPlayer.getAnimation(), sb);
		}
		if (!this.isDisabled() && !(this.animationPlayer.getAnimation() instanceof LayerOffAnimation)) {
			if (this.priority == null) {
				this.disableLayer();
			} else {
				float transitionTimeModifier = entitypatch.getClientAnimator().baseLayer.animationPlayer.getAnimation().get().getTransitionTime();
				setLayerOffAnimation(this.animationPlayer.getAnimation(), this.getEnabledPose(entitypatch, false, 1.0F), this.layerOffAnimation, transitionTimeModifier);
				this.playAnimationInstantly(this.layerOffAnimation, entitypatch);
			}
		}
	}

	public void disableLayer() {
		this.disabled = true;
		this.animationPlayer.setPlayAnimation(Animations.EMPTY_ANIMATION);
	}
	
	public static void setLayerOffAnimation(AssetAccessor<? extends DynamicAnimation> currentAnimation, Pose currentPose, LayerOffAnimation offAnimation, float transitionTimeModifier) {
		offAnimation.setLastAnimation(currentAnimation.get().getRealAnimation());
		offAnimation.setLastPose(currentPose);
		offAnimation.setTotalTime(transitionTimeModifier);
	}
	
	public AssetAccessor<? extends DynamicAnimation> getNextAnimation() {
		return this.nextAnimation;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(this.isBaseLayer() ? "Base Layer(" + ((BaseLayer)this).baseLayerPriority + ") : " : " Composite Layer(" + this.priority + ") : ");
		sb.append(this.animationPlayer.getAnimation() + " ");
		sb.append(", prev elapsed time: " + this.animationPlayer.getPrevElapsedTime() + " ");
		sb.append(", elapsed time: " + this.animationPlayer.getElapsedTime() + " ");
		sb.append(", total time: " + this.animationPlayer.getAnimation().get().getTotalTime() + " ");
		
		return sb.toString();
	}
	
	public static class BaseLayer extends Layer {
		protected Map<Layer.Priority, Layer> compositeLayers = Maps.newLinkedHashMap();
		protected Layer.Priority baseLayerPriority;
		
		public BaseLayer() {
			this(AnimationPlayer::new);
		}
		
		public BaseLayer(Supplier<AnimationPlayer> animationPlayerProvider) {
			super(null, animationPlayerProvider);
			
			for (Priority priority : Priority.values()) {
				this.compositeLayers.computeIfAbsent(priority, Layer::new);
			}
			
			this.baseLayerPriority = Priority.LOWEST;
		}
		
		@Override
		public void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch, float transitionTimeModifier) {
			this.offCompositeLayersLowerThan(entitypatch, nextAnimation);
			super.playAnimation(nextAnimation, entitypatch, transitionTimeModifier);
			this.baseLayerPriority = nextAnimation.get().getPriority();
		}
		
		@Override
		protected void playLivingAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, LivingEntityPatch<?> entitypatch) {
			if (!this.animationPlayer.isEnd()) {
				this.animationPlayer.getAnimation().get().end(entitypatch, nextAnimation, false);
			}
			
			this.resume();
			nextAnimation.get().begin(entitypatch);
			
			if (!nextAnimation.get().isMetaAnimation()) {
				this.concurrentLinkAnimation.acceptFrom(this.animationPlayer.getRealAnimation(), nextAnimation, this.animationPlayer.getElapsedTime());
				this.concurrentLinkAnimation.putOnPlayer(this.animationPlayer, entitypatch);
				entitypatch.updateEntityState();
				this.nextAnimation = nextAnimation;
			}
		}
		
		@Override
		public void update(LivingEntityPatch<?> entitypatch) {
			super.update(entitypatch);
			
			for (Layer layer : this.compositeLayers.values()) {
				layer.update(entitypatch);
			}
		}
		
		public void offCompositeLayersLowerThan(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends StaticAnimation> nextAnimation) {
			Priority[] layersToOff = nextAnimation.get().isMainFrameAnimation() ? nextAnimation.get().getPriority().lowersAndEqual() : nextAnimation.get().getPriority().lowers();
			
			for (Priority p : layersToOff) {
				this.compositeLayers.get(p).off(entitypatch);
			}
		}
		
		public void disableLayer(Priority priority) {
			this.compositeLayers.get(priority).disableLayer();
		}
		
		public Layer getLayer(Priority priority) {
			return this.compositeLayers.get(priority);
		}
		
		public Priority getBaseLayerPriority() {
			return this.baseLayerPriority;
		}
		
		@Override
		public void off(LivingEntityPatch<?> entitypatch) {
			
		}
		
		@Override
		protected boolean isDisabled() {
			return false;
		}
		
		@Override
		protected boolean isBaseLayer() {
			return true;
		}
	}
	
	public enum LayerType {
		BASE_LAYER, COMPOSITE_LAYER
	}
	
	public enum Priority {
		/**
		 * The common usage of each layer
		 * 
		 * LOWEST: Most of living cycle animations. Also a default value for animations doesn't inherit {@link MainFrameAnimation.class}
		 * LOW: A few {@link ActionAnimation.class} that allows showing living cycle animations. e.g. step
		 * MIDDLE: Most of composite living cycle animations. e.g. weapon holding animations
		 * HIGH: A few composite animations that doesn't repeat. e.g. Uchigatana sheathing, Shield hit
		 * HIGHEST: Most of {@link MainFrameAnimation.class} and a few living cycle animations. e.g. ladder animation
		 **/
		LOWEST, LOW, MIDDLE, HIGH, HIGHEST;
		
		public Priority[] lowers() {
			return Arrays.copyOfRange(Priority.values(), 0, this.ordinal());
		}
		
		public Priority[] lowersAndEqual() {
			return Arrays.copyOfRange(Priority.values(), 0, this.ordinal() + 1);
		}
		
		public Priority[] highers() {
			return Arrays.copyOfRange(Priority.values(), this.ordinal(), Priority.values().length);
		}
		
		public boolean isHigherThan(Priority priority) {
			return this.ordinal() > priority.ordinal();
		}
		
		public boolean isHigherOrEqual(Priority priority) {
			return this.ordinal() >= priority.ordinal();
		}
	}
}