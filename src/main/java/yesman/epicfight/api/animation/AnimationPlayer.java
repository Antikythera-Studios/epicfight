package yesman.epicfight.api.animation;

import com.mojang.datafixers.util.Pair;

import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackSpeedModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.PlaybackTimeModifier;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

public class AnimationPlayer {
	protected float elapsedTime;
	protected float prevElapsedTime;
	protected boolean isEnd;
	protected boolean doNotResetTime;
	protected boolean reversed;
	protected AssetAccessor<? extends DynamicAnimation> play;
	
	public AnimationPlayer() {
		this.setPlayAnimation(Animations.EMPTY_ANIMATION);
	}
	
	public void tick(LivingEntityPatch<?> entitypatch) {
		DynamicAnimation currentPlay = this.getAnimation().get();
		DynamicAnimation currentPlayStatic = currentPlay.getRealAnimation().get();
		this.prevElapsedTime = this.elapsedTime;
		
		float playbackSpeed = currentPlay.getPlaySpeed(entitypatch, currentPlay);
		PlaybackSpeedModifier playSpeedModifier = currentPlayStatic.getProperty(StaticAnimationProperty.PLAY_SPEED_MODIFIER).orElse(null);
		
		if (playSpeedModifier != null) {
			playbackSpeed = playSpeedModifier.modify(currentPlay, entitypatch, playbackSpeed, this.prevElapsedTime, this.elapsedTime);
		}
		
		this.elapsedTime += EpicFightSharedConstants.A_TICK * playbackSpeed * (this.isReversed() && currentPlay.canBePlayedReverse() ? -1.0F : 1.0F);
		PlaybackTimeModifier playTimeModifier = currentPlayStatic.getProperty(StaticAnimationProperty.ELAPSED_TIME_MODIFIER).orElse(null);
		
		if (playTimeModifier != null) {
			Pair<Float, Float> time = playTimeModifier.modify(currentPlay, entitypatch, playbackSpeed, this.prevElapsedTime, this.elapsedTime);
			this.prevElapsedTime = time.getFirst();
			this.elapsedTime = time.getSecond();
		}
		
		float totalTime = currentPlay.getTotalTime();
		
		if (currentPlay.isRepeat() && (!Float.isFinite(totalTime) || totalTime <= 0.0F)) {
			// A zero/nonfinite-duration repeat clip cannot advance; hold both clocks at zero
			// rather than modulo-dividing by zero.
			this.prevElapsedTime = 0.0F;
			this.elapsedTime = 0.0F;
		} else if (this.elapsedTime > totalTime) {
			if (currentPlay.isRepeat()) {
				this.prevElapsedTime = this.prevElapsedTime - totalTime;
				this.elapsedTime %= totalTime;
			} else {
				this.elapsedTime = totalTime;
				currentPlay.end(entitypatch, null, true);
				this.isEnd = true;
			}
		} else if (this.elapsedTime < 0) {
			if (currentPlay.isRepeat()) {
				this.prevElapsedTime = totalTime - this.elapsedTime;
				this.elapsedTime = totalTime + this.elapsedTime;
			} else {
				this.elapsedTime = 0.0F;
				currentPlay.end(entitypatch, null, true);
				this.isEnd = true;
			}
		}
		
		if (!Float.isFinite(this.elapsedTime) || !Float.isFinite(this.prevElapsedTime)) {
			this.prevElapsedTime = 0.0F;
			this.elapsedTime = 0.0F;
		}
	}
	
	public void reset() {
		this.elapsedTime = 0;
		this.prevElapsedTime = 0;
		this.isEnd = false;
	}
	
	public void setPlayAnimation(AssetAccessor<? extends DynamicAnimation> animation) {
		if (this.doNotResetTime) {
			this.doNotResetTime = false;
			this.isEnd = false;
		} else {
			this.reset();
		}

		this.play = animation;
	}
	
	public Pose getCurrentPose(LivingEntityPatch<?> entitypatch, float partialTicks) {
		return this.play.get().getPoseByTime(entitypatch, this.prevElapsedTime + (this.elapsedTime - this.prevElapsedTime) * partialTicks, partialTicks);
	}
	
	public float getElapsedTime() {
		return this.elapsedTime;
	}
	
	public float getPrevElapsedTime() {
		return this.prevElapsedTime;
	}
	
	public void setElapsedTimeCurrent(float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.isEnd = false;
	}
	
	public void setElapsedTime(float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.prevElapsedTime = elapsedTime;
		this.isEnd = false;
	}
	
	public void setElapsedTime(float prevElapsedTime, float elapsedTime) {
		this.elapsedTime = elapsedTime;
		this.prevElapsedTime = prevElapsedTime;
		this.isEnd = false;
	}
	
	public void begin(AssetAccessor<? extends DynamicAnimation> animation, LivingEntityPatch<?> entitypatch) {
		animation.get().tick(entitypatch);
	}
	
	public AssetAccessor<? extends DynamicAnimation> getAnimation() {
		return this.play;
	}
	
	public AssetAccessor<? extends StaticAnimation> getRealAnimation() {
		return this.play.get().getRealAnimation();
	}
	
	public void markDoNotResetTime() {
		this.doNotResetTime = true;
	}

	public boolean isEnd() {
		return this.isEnd;
	}
	
	public void terminate(LivingEntityPatch<?> entitypatch) {
		this.play.get().end(entitypatch, this.play, true);
		this.isEnd = true;
	}
	
	public boolean isReversed() {
		return this.reversed;
	}
	
	public void setReversed(boolean reversed) {
		this.reversed = reversed;
	}
	
	public boolean isEmpty() {
		return this.play == Animations.EMPTY_ANIMATION;
	}
	
	@Override
	public String toString() {
		return this.getAnimation() + " " + this.prevElapsedTime + " " + this.elapsedTime;
	}
}