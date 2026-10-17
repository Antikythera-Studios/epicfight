package yesman.epicfight.api.animation;

import yesman.epicfight.EpicFight;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.InitAnimatorEvent;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public abstract class Animator {
	protected final Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> livingAnimations = Maps.newHashMap();
	protected final AnimationVariables animationVariables = new AnimationVariables(this);
	protected final LivingEntityPatch<?> entitypatch;
	
	public Animator(LivingEntityPatch<?> entitypatch) {
		this.entitypatch = entitypatch;
	}
	
	/**
	 * Play an animation
	 * @param nextAnimation the animation that is meant to be played.
	 * @param transitionTimeModifier extends the transition time if positive value provided, or starts in time as an amount of time (e.g. -0.1F starts in 0.1F frame time)
	 */
	public abstract void playAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation, float transitionTimeModifier);
	
	public final void playAnimation(int id, float transitionTimeModifier) {
		this.playAnimation(AnimationManager.byId(id), transitionTimeModifier);
	}
	
	/**
	 * Play a given animation without transition animation.
	 */
	public abstract void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> nextAnimation);
	
	public final void playAnimationInstantly(int id) {
		this.playAnimationInstantly(AnimationManager.byId(id));
	}
	
	/**
	 * Reserve a given animation until the current animation ends.
	 * If the given animation has a higher priority than current animation, it terminates the current animation by force and play the next animation
	 */
	public abstract void reserveAnimation(AssetAccessor<? extends StaticAnimation> nextAnimation);
	
	public final void reserveAnimation(int id) {
		this.reserveAnimation(AnimationManager.byId(id));
	}
	
	/**
	 * Stop playing given animation if exist
	 * @return true when found and successfully stop the target animation
	 */
	public abstract boolean stopPlaying(AssetAccessor<? extends StaticAnimation> targetAnimation);
	
	/**
	 * Play an shooting animation to end aiming pose
	 */
	public abstract void playShootingAnimation();
	
	public final boolean stopPlaying(int id) {
		return this.stopPlaying(AnimationManager.byId(id));
	}
	
	public abstract void setSoftPause(boolean paused);
	public abstract void setHardPause(boolean paused);
	public abstract void tick();
	public abstract boolean isPlaying(AssetAccessor<? extends DynamicAnimation> animation);
	
	public abstract EntityState getEntityState();
	
	/**
	 * Searches an animation player playing the given animation parameter or return base layer if it's null
	 * Secure non-null but returned animation player won't match with a given animation
	 */
	@Nullable
	public abstract AnimationPlayer getPlayerFor(@Nullable AssetAccessor<? extends DynamicAnimation> playingAnimation);
	
	/**
	 * Searches an animation player playing the given animation parameter
	 */
	@Nullable
	public abstract Optional<AnimationPlayer> getPlayer(AssetAccessor<? extends DynamicAnimation> playingAnimation);
	
	public abstract <T> Pair<AnimationPlayer, T> findFor(Class<T> animationType);
	public abstract Pose getPose(float partialTicks);
	
	public void postInit() {
		InitAnimatorEvent initAnimatorEvent = new InitAnimatorEvent(this.entitypatch, this);
        EpicFightEventHooks.Animation.INIT_ANIMATOR.post(initAnimatorEvent);
	}
	
	public void playDeathAnimation() {
		this.playAnimation(this.livingAnimations.getOrDefault(LivingMotions.DEATH, Animations.BIPED_DEATH), 0);
	}
	
	public void addLivingAnimation(LivingMotion livingMotion, AssetAccessor<? extends StaticAnimation> animation) {
		if (AnimationManager.checkNull(animation)) {
			EpicFight.LOGGER.warn("Unable to put an empty animation for " + livingMotion);
			return;
		}
		
		this.livingAnimations.put(livingMotion, animation);
	}
	
	public AssetAccessor<? extends StaticAnimation> getLivingAnimation(LivingMotion livingMotion, AssetAccessor<? extends StaticAnimation> defaultGetter) {
		return this.livingAnimations.getOrDefault(livingMotion, defaultGetter);
	}
	
	public Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> getLivingAnimations() {
		return ImmutableMap.copyOf(this.livingAnimations);
	}
	
	public AnimationVariables getVariables() {
		return this.animationVariables;
	}
	
	public LivingEntityPatch<?> getEntityPatch() {
		return this.entitypatch;
	}
	
	public void resetLivingAnimations() {
		this.livingAnimations.clear();
	}
}