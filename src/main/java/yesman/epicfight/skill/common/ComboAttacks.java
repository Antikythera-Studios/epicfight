package yesman.epicfight.skill.common;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationVariables;
import yesman.epicfight.api.animation.AnimationVariables.IndependentVariableKey;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.AttackAnimation;
import yesman.epicfight.api.animation.types.EntityState;
import yesman.epicfight.api.animation.types.MainFrameAnimation;
import yesman.epicfight.api.event.EntityEventListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.ComboAttackEvent;
import yesman.epicfight.api.event.types.player.ModifyComboCounter;
import yesman.epicfight.api.event.types.player.SkillConsumeEvent;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.registry.entries.EpicFightSkillDataKeys;
import yesman.epicfight.skill.*;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.List;

public class ComboAttacks extends Skill {
    private static final double MIN_AIR_ATTACK_Y_VELOCITY = -0.05D;

    /// Minimum number of consecutive sprint ticks required before a dash attack can fire.
    /// Below this, a sprint+attack falls through to the normal combo path. Prevents instant-spam
    /// of the dash motion right after entering sprint state.
    public static final int MIN_SPRINT_TICKS_FOR_DASH = 8;

	/// Decides if the animation used for combo attack
	public static final IndependentVariableKey<Boolean> COMBO = AnimationVariables.unsyncIndependent(animator -> false, false);
	
	public static SkillBuilder<?> createComboAttackBuilder() {
		return new SkillBuilder<>(ComboAttacks::new).setCategory(SkillCategories.BASIC_ATTACK).setActivateType(ActivateType.ONE_SHOT).setResource(Resource.NONE);
	}

    public static void setComboCounterWithEvent(ModifyComboCounter.Causal reason, ServerPlayerPatch playerpatch, SkillContainer container, @Nullable AnimationAccessor<? extends MainFrameAnimation> causalAnimation, int counter) {
        if (reason != ModifyComboCounter.Causal.TIME_EXPIRED && causalAnimation != null && !causalAnimation.get().getProperty(ActionAnimationProperty.RESET_PLAYER_COMBO_COUNTER).orElse(true)) {
            return;
        }

        // Source the cap from whichever hand is currently driving combat. The dual-style misfire
        // that used to require an explicit ONE_HAND lookup here is now handled centrally inside
        // WeaponCapability.getCurrentSet (which falls back ONE_HAND -> TWO_HAND -> COMMON for
        // the offhand-only mirror case), so cap.getAutoAttackMotion / cap.handleComboCounter
        // already resolve to the correct single-wield moveset. That covers two-handed weapons
        // (longsword, katana) too, which author their combo under TWO_HAND and would have
        // returned an empty ONE_HAND moveset under the old explicit-pin approach.
        CapabilityItem itemCapability = playerpatch.getPrimaryItemCapability();
        int modifiedCombo = itemCapability.handleComboCounter(reason, playerpatch, causalAnimation, counter);
        List<AnimationAccessor<? extends AttackAnimation>> comboMotions = itemCapability.getAutoAttackMotion(playerpatch);

        int prevValue = container.getDataManager().getDataValue(EpicFightSkillDataKeys.COMBO_COUNTER);
        ModifyComboCounter modifyComboCounterEvent = new ModifyComboCounter(reason, playerpatch, causalAnimation, prevValue, modifiedCombo);
        EpicFightEventHooks.Player.MODIFY_COMBO_COUNTER.postWithListener(modifyComboCounterEvent, playerpatch.getEventListener());

        // Clamped combo counter value from 0 to last combo index
        int comboCounterSafe = Mth.clamp(modifyComboCounterEvent.getNextValue(), 0, comboMotions == null ? 0 : comboMotions.size() - 3);

        container.getDataManager().setData(EpicFightSkillDataKeys.COMBO_COUNTER, comboCounterSafe);
    }

    /// Consumption amount when basic attacks set to use stamina
    private float dashAttackConsumption = 0f;
    private float airAttackConsumption = 0f;

	public ComboAttacks(SkillBuilder<?> builder) {
		super(builder);
	}

    @Override
    public void loadDatapackParameters(CompoundTag parameters) {
        super.loadDatapackParameters(parameters);
        this.dashAttackConsumption = parameters.getFloat("dash_attack_consumption");
        this.airAttackConsumption = parameters.getFloat("air_attack_consumption");
    }

    @Override
    public void onInitiate(SkillContainer skillContainer, EntityEventListener eventListener) {
        super.onInitiate(skillContainer, eventListener);

        skillContainer.runOnServer(playerpatch -> {
            eventListener.registerEvent(
                EpicFightEventHooks.Animation.START_ACTION,
                event -> {
                    int comboCounter = skillContainer.getDataManager().getDataValue(EpicFightSkillDataKeys.COMBO_COUNTER);
                    setComboCounterWithEvent(ModifyComboCounter.Causal.ANOTHER_ACTION_ANIMATION, playerpatch, skillContainer, event.getAnimation(), comboCounter);
                },
                this
            );
        });
    }

	@Override
	public boolean isExecutableState(PlayerPatch<?> executor) {
		EntityState playerState = executor.getEntityState();
		Player player = executor.getOriginal();
		return !(player.isSpectator() || executor.isInAir() || !playerState.canBasicAttack());
	}

	@Override
	public void executeOnServer(SkillContainer skillContainer, CompoundTag args) {
		ServerPlayerPatch executor = skillContainer.getServerExecutor();
		SkillConsumeEvent skillConsumeEvent = new SkillConsumeEvent(executor, this, this.resource, null);
        EpicFightEventHooks.Player.CONSUME_SKILL.postWithListener(skillConsumeEvent, executor.getEventListener());

		if (!skillConsumeEvent.isCanceled()) {
			skillConsumeEvent.getResourceType().consumer.consume(skillContainer, executor, skillConsumeEvent.getAmount());
		}

        ComboAttackEvent comboAttackEvent = new ComboAttackEvent(executor);
        EpicFightEventHooks.Player.COMBO_ATTACK.postWithListener(comboAttackEvent, executor.getEventListener());

		if (comboAttackEvent.isCanceled()) {
			return;
		}
		
		// Use the primary cap so an offhand-only weapon drives the combo. The visual mirror is
		// applied once at the renderer level (ClientAnimator.getPose) and the collider matches
		// it via a MirrorX matrix in Collider.updateAndSelectCollideEntity, so we just play the
		// source attack accessor as-is here -- no wrapper swap required, the attack appears on
		// the correct side automatically. Single-wield moveset selection (ONE_HAND vs TWO_HAND)
		// is handled by WeaponCapability.getCurrentSet's offhand-mirror bypass.
		CapabilityItem cap = executor.getPrimaryItemCapability();

		AnimationAccessor<? extends AttackAnimation> attackMotion = null;
		ServerPlayer player = executor.getOriginal();
		SkillDataManager dataManager = skillContainer.getDataManager();
		int comboCounter = dataManager.getDataValue(EpicFightSkillDataKeys.COMBO_COUNTER);
        boolean dashAttack = player.isSprinting() && executor.getSprintTickCount() >= MIN_SPRINT_TICKS_FOR_DASH;
        boolean airAttack = !skillContainer.getExecutor().getOriginal().onGround() && !skillContainer.getExecutor().getOriginal().isInWater() && skillContainer.getExecutor().getOriginal().getDeltaMovement().y() > MIN_AIR_ATTACK_Y_VELOCITY;

        if (player.isPassenger()) {
			Entity entity = player.getVehicle();

			if ((entity instanceof PlayerRideableJumping rideable && rideable.canJump()) && cap.availableOnHorse(executor) && cap.getMountAttackMotion(executor) != null) {
				comboCounter %= cap.getMountAttackMotion(executor).size();
				attackMotion = cap.getMountAttackMotion(executor).get(comboCounter);
				comboCounter++;
			}
		} else {
            List<AnimationAccessor<? extends AttackAnimation>> combo = cap.getAutoAttackMotion(executor);

            if (combo == null || combo.isEmpty()) {
                return;
            }

            int comboSize = combo.size();

            //Improve array safety by doing a wrap and abs instead.
            if (airAttack) {
                attackMotion = combo.get(Math.abs(comboSize - 1) % comboSize);
            } else if (dashAttack) {
                attackMotion = combo.get(Math.abs(comboSize - 2) % comboSize);
            } else {
                attackMotion = combo.get(Math.abs(comboCounter) % comboSize);

                // Grows the combo counter when doing combo attacks. A moveset with
                // only dash+air entries (size <= 2) has no regular swings — keep
                // the counter at 0 instead of dividing by zero.
                comboCounter = comboSize > 2 ? (comboCounter + 1) % (comboSize - 2) : 0;
            }
		}

        if (!airAttack && !dashAttack) dataManager.setData(EpicFightSkillDataKeys.COMBO_COUNTER, comboCounter);

        if (attackMotion != null && this.checkConsumption(skillContainer, dashAttack, airAttack)) {
            // Remove an existing data
			executor.getAnimator().playAnimation(attackMotion, 0.0F);
            executor.getAnimator().getVariables().put(COMBO, attackMotion, true);

			boolean stiffAttack = EpicFightGameRules.STIFF_COMBO_ATTACKS.getRuleValue(executor.getOriginal().level());
			SPAnimatorControl animatorControlPacket;

			if (stiffAttack) {
				animatorControlPacket = new SPAnimatorControl(AbstractAnimatorControl.Action.PLAY, attackMotion, skillContainer.getExecutor(), 0.0F);
			} else {
				animatorControlPacket = new SPAnimatorControl(AbstractAnimatorControl.Action.PLAY_CLIENT, attackMotion, skillContainer.getExecutor(), 0.0F, AbstractAnimatorControl.Layer.COMPOSITE_LAYER, AbstractAnimatorControl.Priority.HIGHEST);
			}

			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntityWithSelf(animatorControlPacket, player);

			// Drop the sprint state so the player has to re-enter sprint (Ctrl / double-W)
			// before another dash attack can be queued. Movement input is untouched.
			if (dashAttack) {
				player.setSprinting(false);
				executor.resetSprintTickCount();
			}
		}
		
		executor.updateEntityState();
	}

	@Override
	public void updateContainer(SkillContainer container) {
		container.runOnServer(serverplayerpatch -> {
			if (container.getExecutor().getTickSinceLastAction() > 16 && container.getDataManager().getDataValue(EpicFightSkillDataKeys.COMBO_COUNTER) > 0) {
                    setComboCounterWithEvent(ModifyComboCounter.Causal.TIME_EXPIRED, serverplayerpatch, container, null, 0);
			}
		});
	}

    /// Checks the consumption of the skill based on dash, air attack states
    protected boolean checkConsumption(SkillContainer container, boolean dash, boolean air) {
        float finalConsumption = air ? this.airAttackConsumption : this.dashAttackConsumption;

        if (this.resource == Resource.STAMINA) {
            finalConsumption = container.getExecutor().getModifiedStaminaConsume(finalConsumption);
        }

        if (air || dash) {
            return container.getExecutor().consumeForSkill(this, this.resource, finalConsumption);
        } else {
            return container.getExecutor().consumeForSkill(this, this.resource);
        }
    }
}
