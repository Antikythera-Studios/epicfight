package yesman.epicfight.client.world.capabilites.entitypatch.player;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Keyframe;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.TransformSheet;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.DirectStaticAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.AnimationSubFileReader;
import yesman.epicfight.api.client.animation.AnimationSubFileReader.PovSettings;
import yesman.epicfight.api.client.animation.AnimationSubFileReader.PovSettings.ViewLimit;
import yesman.epicfight.api.client.animation.Layer;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.client.input.InputManager;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.client.CPAnimatorControl;
import yesman.epicfight.network.client.CPChangePlayerMode;
import yesman.epicfight.network.client.CPModifyEntityModelYRot;
import yesman.epicfight.network.client.CPSetStamina;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.registry.entries.EpicFightDataComponentTypes;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

import java.util.List;
import java.util.Optional;

public class LocalPlayerPatch extends AbstractClientPlayerPatch<LocalPlayer> {
	private final Minecraft minecraft;
    private final FirstPersonLayer firstPersonLayer = new FirstPersonLayer();

	private int chargingTicksO;
	private AnimationSubFileReader.PovSettings povSettings;
	
	public LocalPlayerPatch(LocalPlayer entity) {
		super(entity);
		
		this.minecraft = Minecraft.getInstance();
	}
	
	public void onRespawnLocalPlayer(LocalPlayer newPlayer) {
		this.onJoinWorld(newPlayer, newPlayer.level(), false);
	}

	@Override
	public void preTick() {
		if (this.isHoldingAny()) {
			this.chargingTicksO = this.getChargingTicks();
		} else {
			this.chargingTicksO = 0;
		}
		
		super.preTick();
	}

	@Override
	public void postTickClient() {
		// Handle first person animation
		final AssetAccessor<? extends StaticAnimation> currentPlaying = this.firstPersonLayer.animationPlayer.getRealAnimation();

		boolean noPovAnimation = this.getClientAnimator().iterVisibleLayersUntilFalse(layer -> {
			if (layer.isOff()) {
				return true;
			}

			StaticAnimation anim = layer.animationPlayer.getRealAnimation().get();
			Optional<DirectStaticAnimation> optPovAnimation = anim.getProperty(ClientAnimationProperties.POV_ANIMATION);
			Optional<PovSettings> optPovSettings = anim.getProperty(ClientAnimationProperties.POV_SETTINGS);

            if (optPovAnimation.isPresent() && optPovSettings.isPresent()) {
                DirectStaticAnimation povAnimation = optPovAnimation.get();

                if (!povAnimation.equals(currentPlaying.get())) {
                    this.firstPersonLayer.playAnimation(povAnimation, layer.animationPlayer.getRealAnimation(), this, 0.0F);
                    this.povSettings = optPovSettings.get();
                }
            }

			return optPovAnimation.isEmpty();
		});

		if (noPovAnimation && !currentPlaying.equals(Animations.EMPTY_ANIMATION)) {
			this.firstPersonLayer.off();
		}

		this.firstPersonLayer.update(this);

		if (this.firstPersonLayer.animationPlayer.getAnimation().equals(Animations.EMPTY_ANIMATION)) {
			this.povSettings = null;
		}
	}
	
	@Override
	public boolean overrideRender() {
		// Disable rendering the player when animated first person model disabled
		if (this.original.is(this.minecraft.player)) {
			if (this.minecraft.options.getCameraType().isFirstPerson() && !ClientConfig.enableAnimatedFirstPersonModel) {
				return false;
			}
		}
		
		return super.overrideRender();
	}
	
	@Override
	public LivingEntity getTarget() {
		return EpicFightCameraAPI.getInstance().getFocusingEntity();
	}
	
	@Override
	public void toVanillaMode(boolean synchronize) {
		RenderEngine.getInstance().battleModeHUD.slideDown();
		
		if (this.playerMode != PlayerMode.VANILLA) {
			if (ClientConfig.autoPerspectiveSwithing) {
				this.minecraft.options.setCameraType(CameraType.FIRST_PERSON);
			}
			
			if (synchronize) {
				EpicFightNetworkManager.sendToServer(new CPChangePlayerMode(PlayerMode.VANILLA));
			}
		}
		
		super.toVanillaMode(synchronize);
	}
	
	@Override
	public void toEpicFightMode(boolean synchronize) {
		RenderEngine.getInstance().battleModeHUD.slideUp();
		
		if (this.playerMode != PlayerMode.EPICFIGHT) {
			if (ClientConfig.autoPerspectiveSwithing) {
				this.minecraft.options.setCameraType(CameraType.THIRD_PERSON_BACK);
			}
			
			if (synchronize) {
				EpicFightNetworkManager.sendToServer(new CPChangePlayerMode(PlayerMode.EPICFIGHT));
			}
		}
		
		super.toEpicFightMode(synchronize);
	}
	
	@Override
	public boolean isFirstPerson() {
		return this.minecraft.options.getCameraType() == CameraType.FIRST_PERSON;
	}
	
	@Override
	public boolean shouldBlockMoving() {
		return InputManager.isActionActive(MinecraftInputAction.MOVE_BACKWARD) || InputManager.isActionActive(MinecraftInputAction.SNEAK);
	}
	
	@Override
	public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
		if (!this.isLogicalClient()) {
			return false;
		}
		
		return actionAnimation.shouldPlayerMove(this);
	}
	
	public int getChargingTicksO() {
		return this.chargingTicksO;
	}

	public FirstPersonLayer getFirstPersonLayer() {
		return this.firstPersonLayer;
	}
	
	public AnimationSubFileReader.PovSettings getPovSettings() {
		return this.povSettings;
	}
	
	public boolean hasCameraAnimation() {
		return this.povSettings != null && this.povSettings.cameraTransform() != null;
	}
	
	@Override
	public void setStamina(float value) {
		EpicFightNetworkManager.sendToServer(new CPSetStamina(value, true));
	}

	@Override
	public void setModelYRot(float amount, boolean sendPacket) {
		super.setModelYRot(amount, sendPacket);
		
		if (sendPacket) {
			EpicFightNetworkManager.sendToServer(new CPModifyEntityModelYRot(amount));
		}
	}
	
	public float getModelYRot() {
		return this.modelYRot;
	}
	
	public void setModelYRotInGui(float rotDeg) {
		this.useModelYRot = true;
		this.modelYRot = rotDeg;
	}
	
	public void disableModelYRotInGui(float originalDeg) {
		this.useModelYRot = false;
		this.modelYRot = originalDeg;
	}

	@Override
	public void disableModelYRot(boolean sendPacket) {
		super.disableModelYRot(sendPacket);
		
		if (sendPacket) {
			EpicFightNetworkManager.sendToServer(new CPModifyEntityModelYRot(0.0F, true));
		}
	}
	
	@Override
	public double checkXTurn(double xRot) {
		if (xRot == 0.0D) {
			return xRot;
		}
		
		if (ClientConfig.enableFirstPersonCameraMove && this.minecraft.options.getCameraType().isFirstPerson() && this.isEpicFightMode() && !this.getFirstPersonLayer().isOff()) {
			ViewLimit viewLimit = this.getPovSettings().viewLimit();
			
			if (viewLimit != null) {
				float xRotDest = this.original.getXRot() + (float)xRot * 0.15F;
				
				if (xRotDest <= viewLimit.xRotMin() || xRotDest >= viewLimit.xRotMax()) {
					return 0.0D;
				}
			}
		}
		
		return xRot;
	}
	
	@Override
	public double checkYTurn(double yRot) {
		if (yRot == 0.0D) {
			return yRot;
		}
		
		if (ClientConfig.enableFirstPersonCameraMove && this.minecraft.options.getCameraType().isFirstPerson() && this.isEpicFightMode() && !this.getFirstPersonLayer().isOff()) {
			ViewLimit viewLimit = this.getPovSettings().viewLimit();
			
			if (viewLimit != null) {
				float yCamera = Mth.wrapDegrees(this.original.getYRot());
				float yBody = MathUtils.findNearestRotation(yCamera, this.getYRot());
				float yRotDest = yCamera + (float)yRot * 0.15F;
				float yRotClamped = Mth.clamp(yRotDest, yBody + viewLimit.yRotMin(), yBody + viewLimit.yRotMax());
				
				if (yRotDest != yRotClamped) {
					return 0.0D;
				}
			}
		}
		
		return yRot;
	}
	
	@Override
	public void beginAction(ActionAnimation animation) {
        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();

        if (cameraApi.isTPSMode()) {
            if (cameraApi.getFocusingEntity() != null && animation instanceof AttackAnimation) {
                cameraApi.alignPlayerLookToCrosshair(false, true, true);
            } else {
                cameraApi.alignPlayerLookToCameraRotation(false, true, true);
            }
        }

        if (!this.useModelYRot || animation.getProperty(ActionAnimationProperty.SYNC_CAMERA).orElse(false)) {
            this.modelYRot = this.original.getYRot();
        }

        if (cameraApi.getFocusingEntity() != null && cameraApi.isLockingOnTarget() && !cameraApi.getFocusingEntity().isRemoved()) {
            Vec3 playerPosition = this.original.position();
            Vec3 targetPosition = cameraApi.getFocusingEntity().position();
            Vec3 toTarget = targetPosition.subtract(playerPosition);
            this.original.setYRot((float)MathUtils.getYRotOfVector(toTarget));
        }
	}
	
	/**
	 * Play an animation after the current animation is finished
	 */
	@Override
	public void reserveAnimation(AssetAccessor<? extends StaticAnimation> animation) {
		this.animator.reserveAnimation(animation);
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.RESERVE, animation, 0.0F, false, false, false));
	}
	
	/**
	 * Play an animation without convert time
	 */
	@Override
	public void playAnimationInstantly(AssetAccessor<? extends StaticAnimation> animation) {
		this.animator.playAnimationInstantly(animation);
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.PLAY_INSTANTLY, animation, 0.0F, false, false, false));
	}
	
	/**
	 * Play a shooting animation to end aim pose
	 * This method doesn't send packet from client to server
	 */
	@Override
	public void playShootingAnimation() {
		this.animator.playShootingAnimation();
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.SHOT, Animations.EMPTY_ANIMATION, 0.0F, false, true, false));
	}
	
	/**
	 * Stop playing an animation
	 */
	@Override
	public void stopPlaying(AssetAccessor<? extends StaticAnimation> animation) {
		this.animator.stopPlaying(animation);
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.STOP, animation, -1.0F, false, false, false));
	}
	
	/**
	 * Play an animation ensuring synchronization between client-server
	 * Plays animation when getting response from server if it called in client side.
	 * Do not call this in client side for non-player entities.
	 */
	@Override
	public void playAnimationSynchronized(AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier) {
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.PLAY, animation, transitionTimeModifier, false, false, true));
	}
	
	/**
	 * Play an animation only in client side, including all clients tracking this entity
	 */
	@Override
	public void playAnimationInClientSide(AssetAccessor<? extends StaticAnimation> animation, float transitionTimeModifier) {
		this.animator.playAnimation(animation, transitionTimeModifier);
		this.handleAnimationPayloadSend(new CPAnimatorControl(AbstractAnimatorControl.Action.PLAY, animation, transitionTimeModifier, false, true, false));
	}
	
	/**
	 * Pause an animator until it receives a proper order
	 * @param action SOFT_PAUSE: resume when next animation plays
	 * 				 HARD_PAUSE: resume when hard pause is set false
	 **/
	@Override
	public void pauseAnimator(AbstractAnimatorControl.Action action, boolean pause) {
		super.pauseAnimator(action, pause);
		this.handleAnimationPayloadSend(new CPAnimatorControl(action, Animations.EMPTY_ANIMATION, 0.0F, pause, false, false));
	}
	
	private void handleAnimationPayloadSend(CPAnimatorControl payload) {
		if (payload.action().syncVariables()) {
			payload.animationVariables().addAll(this.getAnimator().getVariables().createPendingVariablesPayloads(payload.animation()));
		}
		
		EpicFightNetworkManager.sendToServer(payload);
	}
	
	@Override
	public void openSkillBook(ItemStack itemstack, InteractionHand hand) {
		if (itemstack.has(EpicFightDataComponentTypes.SKILL.get())) {
            Holder<Skill> skill = itemstack.get(EpicFightDataComponentTypes.SKILL.get());

            if (skill == null || !skill.isBound()) {
                EpicFight.LOGGER.warn("Skill book contains an unbound skill holder, not opening screen: {}", skill);
                return;
            }

			Minecraft.getInstance().setScreen(new SkillBookScreen(this.original, skill.value(), hand, null));
		}
	}
	
	@Override
	public void resetHolding() {
		if (this.holdingSkill != null) {
			ControlEngine.getInstance().releaseAllServedKeys();
		}
		
		super.resetHolding();
	}
	
	@Override
	public void updateHeldItem(CapabilityItem mainHandCap, CapabilityItem offHandCap) {
		super.updateHeldItem(mainHandCap, offHandCap);

		if (ClientConfig.playerBehaviorStrategy == ClientConfig.PlayerBehaviorStrategy.SWITCHING_MODE) {

			if (ClientConfig.combatCategorizedItems.contains(this.original.getMainHandItem().getItem())
					|| ClientConfig.combatCategorizedItems.contains(this.original.getOffhandItem().getItem())) {
				this.toEpicFightMode(true);
			} else if (ClientConfig.miningCategorizedItems.contains(this.original.getMainHandItem().getItem())) {
				this.toVanillaMode(true);
			}
		}
	}

    @Override
    public void playLocalSound(Holder<SoundEvent> sound) {
        // Playing sound twice fixes volume issue...
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.value(), 1.0F, 1.0F));
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound.value(), 1.0F, 1.0F));
    }

	/**
	 * Judge the next behavior depending on player's item preference and where he's looking at
	 * @return true if the next action is swing a weapon, false if the next action is breaking a block
	 */
    public boolean canPlayAttackAnimation() {
        if (this.isVanillaMode()) {
            return false;
        }

        EpicFightCameraAPI cameraApi = EpicFightCameraAPI.getInstance();

        HitResult hitResult =
            (EpicFightCameraAPI.getInstance().isTPSMode() && cameraApi.getCrosshairHitResult() != null && cameraApi.getCrosshairHitResult().getLocation().distanceToSqr(this.original.getEyePosition()) < this.original.blockInteractionRange() * this.original.blockInteractionRange())
                ? cameraApi.getCrosshairHitResult() : this.minecraft.hitResult;

        if (hitResult == null) {
            return true;
        }

        EntityHitResult entityHitResult = RenderEngine.asEntityHitResult(this.minecraft.hitResult);

        if (entityHitResult != null) {
            Entity hitEntity = entityHitResult.getEntity();

            if (!(hitEntity instanceof LivingEntity) && !(hitEntity instanceof net.minecraft.world.entity.boss.EnderDragonPart) && !(hitEntity instanceof Interaction)) {
                return false;
            }
        }

        if (EpicFightCameraAPI.getInstance().isLockingOnTarget()) {
            return true;
        }

        if (ClientConfig.playerBehaviorStrategy.checkHitResult()) {
            // Resolve the combat item against the active hand: in offhand-only mirror mode the
            // weapon lives in the offhand, so a mainhand-only check would treat the player as
            // unarmed and let block-break run against blocks that they're actually trying to
            // attack with the offhand weapon.
            ItemStack combatItem;
            if (ClientConfig.combatCategorizedItems.contains(this.original.getMainHandItem().getItem())) {
                combatItem = this.original.getMainHandItem();
            } else if (ClientConfig.combatCategorizedItems.contains(this.original.getOffhandItem().getItem())) {
                combatItem = this.original.getOffhandItem();
            } else {
                combatItem = ItemStack.EMPTY;
            }

            if (!combatItem.isEmpty()) {
                BlockHitResult blockHitResult = RenderEngine.asBlockHitResult(this.minecraft.hitResult);

                if (blockHitResult != null && this.minecraft.level != null) {
                    BlockPos bp = blockHitResult.getBlockPos();
                    BlockState bs = this.minecraft.level.getBlockState(bp);
                    return !combatItem.getItem().canAttackBlock(bs, this.original.level(), bp, this.original) || !combatItem.isCorrectToolForDrops(bs);
                }
            } else {
                return RenderEngine.hitResultNotEquals(this.minecraft.hitResult, HitResult.Type.BLOCK);
            }

            return true;
        } else {
            return this.getPlayerMode() == PlayerPatch.PlayerMode.EPICFIGHT;
        }
    }

	public class FirstPersonLayer extends Layer {
		private final TransformSheet linkCameraTransform = new TransformSheet(List.of(new Keyframe(0.0F, JointTransform.empty()), new Keyframe(Float.MAX_VALUE, JointTransform.empty())));
		
		public FirstPersonLayer() {
			super(null);
		}
		
		public void playAnimation(AssetAccessor<? extends StaticAnimation> nextFirstPersonAnimation, AssetAccessor<? extends StaticAnimation> originalAnimation, LivingEntityPatch<?> entitypatch, float transitionTimeModifier) {
			Optional<PovSettings> povSettings = originalAnimation.get().getProperty(ClientAnimationProperties.POV_SETTINGS);
			
			boolean hasPrevCameraAnimation = LocalPlayerPatch.this.povSettings != null && LocalPlayerPatch.this.povSettings.cameraTransform() != null;
			boolean hasNextCameraAnimation = povSettings.isPresent() && povSettings.get().cameraTransform() != null;
			
			// Activate pov animation
			if (hasPrevCameraAnimation || hasNextCameraAnimation) {
				if (hasPrevCameraAnimation) {
					this.linkCameraTransform.getKeyframes()[0].transform().copyFrom(LocalPlayerPatch.this.povSettings.cameraTransform().getInterpolatedTransform(this.animationPlayer.getElapsedTime()));
				} else {
					this.linkCameraTransform.getKeyframes()[0].transform().copyFrom(JointTransform.empty());
				}
				
				if (hasNextCameraAnimation) {
					this.linkCameraTransform.getKeyframes()[1].transform().copyFrom(povSettings.get().cameraTransform().getKeyframes()[0].transform());
				} else {
					this.linkCameraTransform.getKeyframes()[1].transform().clearTransform();
				}
				
				this.linkCameraTransform.getKeyframes()[1].setTime(nextFirstPersonAnimation.get().getTransitionTime());
			}
			
			super.playAnimation(nextFirstPersonAnimation, entitypatch, transitionTimeModifier);
		}
		
		public void off() {
			// Off camera animation
			if (LocalPlayerPatch.this.povSettings != null && LocalPlayerPatch.this.povSettings.cameraTransform() != null) {
				this.linkCameraTransform.getKeyframes()[0].transform().copyFrom(LocalPlayerPatch.this.povSettings.cameraTransform().getInterpolatedTransform(this.animationPlayer.getElapsedTime()));
				this.linkCameraTransform.getKeyframes()[1].transform().copyFrom(JointTransform.empty());
				this.linkCameraTransform.getKeyframes()[1].setTime(EpicFightSharedConstants.GENERAL_ANIMATION_TRANSITION_TIME);
			}
			
			super.off(LocalPlayerPatch.this);
		}
		
		@Override
		protected Pose getCurrentPose(LivingEntityPatch<?> entitypatch) {
			return this.animationPlayer.isEmpty() ? super.getCurrentPose(entitypatch) : this.animationPlayer.getCurrentPose(entitypatch, 0.0F);
		}
		
		public TransformSheet getLinkCameraTransform() {
			return this.linkCameraTransform;
		}
	}

}