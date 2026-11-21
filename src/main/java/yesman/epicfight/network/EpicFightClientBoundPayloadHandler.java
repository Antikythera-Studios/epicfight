package yesman.epicfight.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.client.animation.ClientAnimator;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ConditionalReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ItemPresetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.MovesetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.WeaponModifierReloadListener;
import yesman.epicfight.api.exception.DatapackException;
import yesman.epicfight.api.utils.LevelUtil;
import yesman.epicfight.client.ClientEngine;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.AbstractClientPlayerPatch;
import yesman.epicfight.network.common.AbstractAnimatorControl;
import yesman.epicfight.network.common.BiDirectionalAnimationVariable;
import yesman.epicfight.network.common.BiDirectionalSyncAnimationPositionPacket;
import yesman.epicfight.network.common.BiDirectionalSyncEmoteSlots;
import yesman.epicfight.network.server.*;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.skill.modules.HoldableSkill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;

import java.util.function.BiConsumer;

public interface EpicFightClientBoundPayloadHandler {
	static void handleAbsorption(final SPAbsorption data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		if (entity instanceof LivingEntity livingentity && !(entity instanceof Player)) {
			livingentity.setAbsorptionAmount(data.amount());
		}
	}
	
	static void handleAddLearnedSkill(final SPAddLearnedSkill data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), PlayerPatch.class).ifPresent(playerpatch -> {
			PlayerSkills skillCapability = playerpatch.getPlayerSkills();
			data.skills().stream().map(Holder::value).forEach(skillCapability::addLearnedSkill);
		});
	}
	
	static void handleSkillData(final SPHandleSkillData data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> {
			SkillDataManager dataManager = playerpatch.getSkill(data.skillSlot()).getDataManager();
			
			switch (data.workType()) {
			case REGISTER -> {
				Object value = data.skillDataKey().value().decode(data.buffer());
				dataManager.registerData(data.skillDataKey());
				dataManager.setDataRawtype(data.skillDataKey(), value);
			}
			case REMOVE -> dataManager.removeData(data.skillDataKey());
			case MODIFY -> {
				Object value = data.skillDataKey().value().decode(data.buffer());
				dataManager.setDataRawtype(data.skillDataKey(), value);
			}
			}
		});
	}
	
	static void handleAnimationVariablePacket(final BiDirectionalAnimationVariable data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player().level().getEntity(data.entityId()), LivingEntityPatch.class).ifPresent(data::commonProcess);
	}
	
	static void handleAnimatorControl(final SPAnimatorControl data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player().level().getEntity(data.entityId()), LivingEntityPatch.class).ifPresent(entitypatch -> {
			data.animationVariables().forEach(animationVariable -> handleAnimationVariablePacket(animationVariable, context));
			
			if (data.action() == AbstractAnimatorControl.Action.PLAY_CLIENT && data.layer() != AbstractAnimatorControl.Layer.ANIMATION && data.priority() != AbstractAnimatorControl.Priority.ANIMATION) {
				entitypatch.getClientAnimator().playAnimationAt(data.animation(), data.transitionTimeModifier(), data.layer(), data.priority());
			} else {
				data.commonProcess(entitypatch);
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	static void handleChangeGameRule(final SPChangeGamerule data, final IPayloadContext context) {
		GameRules.Value<?> ruleValue = context.player().level().getGameRules().getRule(data.keyValuePair().gamerule().getRuleKey());
		((BiConsumer<GameRules.Value<?>, Object>)data.keyValuePair().gamerule().getRuleType().setRule()).accept(ruleValue, data.keyValuePair().value());
	}
	
	static void handleChangeLivingMotion(final SPChangeLivingMotion data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());

		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, LivingEntityPatch.class).ifPresent(entitypatch -> {
			ClientAnimator animator = entitypatch.getClientAnimator();
			animator.resetLivingAnimations();
			animator.offAllLayers();
			animator.resetMotion(false);
			animator.resetCompositeMotion();
			
			for (int i = 0; i < data.livingMotions().size(); i++) {
				entitypatch.getClientAnimator().addLivingAnimation(data.livingMotions().get(i), data.animations().get(i));
			}
			
			if (data.setChangesAsDefault()) {
				animator.setCurrentMotionsAsDefault();
			}
		});
	}
	
	static void handleChangePlayerMode(final SPChangePlayerMode data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.toMode(data.mode(), false);
		});
	}
	
	static void handleChangeSkill(final SPChangeSkill data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player().level().getEntity(data.entityId()), PlayerPatch.class).ifPresent(playerpatch -> {
			Skill skill = Skill.skillOrNull(data.skill());
			playerpatch.getSkill(data.skillSlot()).setSkill(skill);
			
			if (skill != null && data.skillSlot().category().learnable()) {
				playerpatch.getPlayerSkills().addLearnedSkill(skill);
			}
			
			playerpatch.getSkill(data.skillSlot()).setDisabled(false);
		});
	}
	
	static void handleClearSkills(final SPClearSkills data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());

        EpicFightCapabilities.getPlayerPatchAsOptional(entity).ifPresent(playerpatch -> {
			playerpatch.getPlayerSkills().clearContainersAndLearnedSkills(playerpatch.getOriginal().isLocalPlayer());
		});
	}
	
	static void handleDataPack(final SPDatapackSync data, final IPayloadContext context) {
		try {
			switch (data.packetType()) {
                case MOB -> MobPatchReloadListener.processServerPacket(data);
                case SKILL_PARAMS -> SkillReloadListener.processServerPacket(data);
                case WEAPON -> ItemCapabilityReloadListener.processServerPacket(data);
                case ITEM_PRESET ->  ItemPresetReloadListener.processServerPacket(data);
                case PROVIDER_CONDITIONAL -> ConditionalReloadListener.processServerPacket(data);
                case MOVESET -> MovesetReloadListener.processServerPacket(data);
				case MODIFIER -> WeaponModifierReloadListener.processServerPacket(data);
                case ARMOR -> ItemCapabilityReloadListener.processServerPacket(data);
                case WEAPON_TYPE -> WeaponTypeReloadListener.processServerPacket(data);
                case ITEM_KEYWORD -> ItemKeywordReloadListener.handleClientBoundSyncPacket(data);
                case MANDATORY_RESOURCE_PACK_ANIMATION, RESOURCE_PACK_ANIMATION -> AnimationManager.getInstance().processServerPacket(data, data.packetType() == SPDatapackSync.PacketType.MANDATORY_RESOURCE_PACK_ANIMATION);
			}
		} catch (Exception e) {
			e.printStackTrace();
			throw new DatapackException(e.getMessage());
		}
	}
	
	static void handleEntityPairing(final SPEntityPairingPacket data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> entitypatch.fireEntityPairingEvent(data));
	}
	
	static void handleFracture(final SPCreateTerrainFracture data, final IPayloadContext context) {
		LevelUtil.circleSlamFracture(null, context.player().level(), data.location(), data.radius(), data.noSound(), data.noParticle());
	}
	
	static void handleModelYRot(final SPModifyPlayerData.SetPlayerYRot data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> playerpatch.setModelYRot(data.yRot(), false));
	}
	
	static void handleDisableModelYRot(final SPModifyPlayerData.DisablePlayerYRot data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> playerpatch.disableModelYRot(false));
	}
	
	static void handleSetLastAttackResult(final SPModifyPlayerData.SetLastAttackResult data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> playerpatch.setLastAttackSuccess(data.lastAttackSuccess()));
	}
	
	static void handleSetPlayerMode(final SPModifyPlayerData.SetPlayerMode data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> playerpatch.toMode(data.mode(), false));
	}
	
	static void handleSetGrapplingTarget(final SPModifyPlayerData.SetGrapplingTarget data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, PlayerPatch.class).ifPresent(playerpatch -> {
			Entity grapplingTarget = context.player().level().getEntity(data.grapplingTargetEntityId());
			
			if (grapplingTarget instanceof LivingEntity) {
				playerpatch.setGrapplingTarget((LivingEntity)grapplingTarget);
			} else {
				playerpatch.setGrapplingTarget(null);
			}
		});
	}
	
	static void handleMobEffect(final SPMobEffectControl data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		if (entity != null && entity instanceof LivingEntity livingEntity) {
			switch (data.action()) {
			case ACTIVATE -> livingEntity.addEffect(new MobEffectInstance(data.mobEffect()));
			case REMOVE -> livingEntity.removeEffect(data.mobEffect());
			}
		}
	}
	
	static void handleModifyExpandedEntityData(final SPModifyExpandedEntityData data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, LivingEntityPatch.class).ifPresent(entitypatch -> {
			Object value = data.expandedEntityDataAccessor().value().streamCodec().decode(data.buffer());
			entitypatch.getExpandedSynchedData().setRaw(data.expandedEntityDataAccessor(), value);
		});
	}
	
	static void handlePlayUiSound(final SPPlayUISound data, final IPayloadContext context) {
		ClientEngine.getInstance().playUISound(data);
	}
	
	static void handleRemoveSkill(final SPRemoveSkillAndLearn data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), PlayerPatch.class).ifPresent(playerpatch -> {
			Skill skill = data.skill().value();
			playerpatch.getPlayerSkills().removeLearnedSkill(skill);
			SkillContainer skillContainer = playerpatch.getSkill(data.skillSlot());
			
			if (skillContainer.getSkill() == skill) {
				skillContainer.setSkill(null);
			}
		});
	}
	
	static void handleSetAttackTarget(final SPSetAttackTarget data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		Entity targetEntity = context.player().level().getEntity(data.targetEntityId());
		
		if (entity != null && entity instanceof Mob mob) {
			if (targetEntity == null || !(targetEntity instanceof LivingEntity)) {
				mob.setTarget(null);
			} else {
				mob.setTarget((LivingEntity)targetEntity);
			}
		}
	}
	
	static void handleSetRemotePlayerSkill(final SPSetRemotePlayerSkill data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(entity, AbstractClientPlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.getSkill(data.skillSlot()).setSkillRemote(Skill.skillOrNull(data.skill()));
		});
	}
	
	static void handleSetSkillContainerValue(final SPSetSkillContainerValue data, final IPayloadContext context) {
		EpicFight.LOGGER.debug("SPSetSkillContainerValue: {}", data.entityId());
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player().level().getEntity(data.entityId()), PlayerPatch.class).ifPresent(playerpatch -> {
			SkillContainer container = playerpatch.getSkill(data.skillSlot());
			switch (data.target()) {
			case ENABLE -> container.setDisabled(data.boolVal());
			case ACTIVATE -> { if (data.boolVal()) container.activate(); else container.deactivate(); }
			case RESOURCE -> container.setResource(data.floatVal());
			case DURATION -> container.setDuration((int)data.floatVal());
			case MAX_DURATION -> container.setMaxDuration((int)data.floatVal());
			case STACKS -> container.setStack((int)data.floatVal());
			case MAX_RESOURCE -> container.setMaxResource(data.floatVal());
			case REPLACE_COOLDOWN -> container.setReplaceCooldown((int)data.floatVal());
			}
		});
	}
	
	static void handleSkillFeedback(final SPSkillFeedback data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), PlayerPatch.class).ifPresent(playerpatch -> {
			switch(data.feedbackType()) {
			case EXECUTED -> {
				SkillContainer skillContainer = playerpatch.getSkill(data.skillSlot());
				skillContainer.getSkill().executeOnClient(skillContainer, data.arguments());
			}
			case HOLDING_START -> {
				SkillContainer container = playerpatch.getSkill(data.skillSlot());
				
				if (container.getSkill() instanceof HoldableSkill holdableSkill) {
					playerpatch.startSkillHolding(holdableSkill);
					ControlEngine.getInstance().setHoldingKey(container.getSlot(), holdableSkill.getKeyMapping());
				}
			}
			case EXPIRED -> {
				SkillContainer skillContainer = playerpatch.getSkill(data.skillSlot());
				skillContainer.getSkill().cancelOnClient(skillContainer, data.arguments());
			}
			default -> {}
			}
		});
	}
	
	static void handleUpdatePlayerInput(final SPUpdatePlayerInput data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(Minecraft.getInstance().player.level().getEntity(data.entityId()), PlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.dx = data.strafe();
			playerpatch.dz = data.forward();
		});
	}
	
	static void handleSyncAnimationPosition(final BiDirectionalSyncAnimationPositionPacket data, final IPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		if (entity instanceof LivingEntity livingentity) {
			livingentity.lerpX = data.position().x;
			livingentity.lerpY = data.position().y;
			livingentity.lerpZ = data.position().z;
			livingentity.lerpSteps = data.lerpSteps();
		}
	}
	
	static void handleInitSkills(final SPInitSkills data, final IPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), PlayerPatch.class).ifPresent(playerpatch -> playerpatch.getPlayerSkills().read(data.serializedSkill()));
	}

    static void handleSyncEmoteSlot(final BiDirectionalSyncEmoteSlots data, final IPayloadContext context) {
        EpicFightCapabilities.getLocalPlayerPatchAsOptional(context.player().level().getEntity(data.playerId())).ifPresent(playerpatch -> playerpatch.getEmoteSlots().deserialize(data.compoundTag(), playerpatch.getOriginal().registryAccess()));
    }
}