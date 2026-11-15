package yesman.epicfight.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import yesman.epicfight.network.EpicFightPayloadContext;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.network.client.CPAnimatorControl;
import yesman.epicfight.network.client.CPChangePlayerMode;
import yesman.epicfight.network.client.CPChangeSkill;
import yesman.epicfight.network.client.CPHandleSkillData;
import yesman.epicfight.network.client.CPModifyEntityModelYRot;
import yesman.epicfight.network.client.CPPairingAnimationRegistry;
import yesman.epicfight.network.client.CPSetPlayerTarget;
import yesman.epicfight.network.client.CPSetStamina;
import yesman.epicfight.network.client.CPSkillRequest;
import yesman.epicfight.network.client.CPUpdatePlayerInput;
import yesman.epicfight.network.common.BiDirectionalAnimationVariable;
import yesman.epicfight.network.common.BiDirectionalSyncAnimationPositionPacket;
import yesman.epicfight.network.common.BiDirectionalSyncEmoteSlots;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.network.server.SPModifyPlayerData;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.network.server.SPUpdatePlayerInput;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillDataManager;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

public interface EpicFightServerBoundPayloadHandler {
	static <T> void handleAnimationVariablePacket(final BiDirectionalAnimationVariable data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			data.commonProcess(playerpatch);
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(data, playerpatch.getOriginal());
		});
	}
	
	static void handleAnimatorControl(final CPAnimatorControl data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			if (!data.isClientOnly()) {
				data.animationVariables().forEach(animationVariable -> handleAnimationVariablePacket(animationVariable, context));
				data.commonProcess(playerpatch);
			}
			
			SPAnimatorControl payload = new SPAnimatorControl(data.action(), data.animation(), playerpatch.getOriginal().getId(), data.transitionTimeModifier(), data.pause());
			payload.animationVariables().addAll(data.animationVariables());
			
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(payload, playerpatch.getOriginal());
			
			if (data.responseToSender()) {
				payload.animationVariables().clear();
				EpicFightNetworkManager.sendToPlayer(payload, playerpatch.getOriginal());
			}
		});
	}
	
	static void handleChangePlayerMode(final CPChangePlayerMode data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.toMode(data.mode(), false);
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(SPModifyPlayerData.setPlayerMode(playerpatch.getOriginal().getId(), playerpatch.getPlayerMode()), playerpatch.getOriginal());
		});
	}
	
	static void handleChangeSkill(final CPChangeSkill data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			Skill skill = Skill.skillOrNull(data.skill());
			SkillContainer skillContainer = playerpatch.getSkill(data.skillSlot());
			boolean skillEquipped = (!skillContainer.onReplaceCooldown() || data.skillBookSlotIndex() >= 0) && skillContainer.setSkill(skill);
			
			if (skill != null) {
				if (data.skill().value().getCategory().learnable()) {
					playerpatch.getPlayerSkills().addLearnedSkill(skill);
				}
				
				if (skillEquipped && data.skillBookSlotIndex() >= 0) {
					if (!playerpatch.getOriginal().isCreative()) playerpatch.getOriginal().getInventory().removeItem(playerpatch.getOriginal().getInventory().getItem(data.skillBookSlotIndex()));
				}
			}
			
			if (skillEquipped) {
				skillContainer.setReplaceCooldown(EpicFightGameRules.SKILL_REPLACE_COOLDOWN.getRuleValue(playerpatch.getOriginal().level()));
				EpicFightNetworkManager.sendToPlayer(SPSetSkillContainerValue.replaceCooldown(skillContainer.getSlot(), skillContainer.getReplaceCooldown(), playerpatch.getOriginal().getId()), playerpatch.getOriginal());
				EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPSetRemotePlayerSkill(data.skillSlot(), playerpatch.getOriginal().getId(), data.skill()), playerpatch.getOriginal());
			}
		});
	}
	
	static void handlePairingAnimationRegistry(final CPPairingAnimationRegistry data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		AnimationManager.getInstance().validateClientAnimationRegistry(data, ((ServerPlayer)context.player()).connection);
	}
	
	static void handleExecuteSkill(final CPSkillRequest data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			SkillContainer skillContainer = playerpatch.getSkill(data.skillSlot());
			
			switch (data.workType()) {
				case CAST -> skillContainer.requestCasting(playerpatch, data.arguments());
				case CANCEL -> skillContainer.requestCancel(playerpatch, data.arguments());
				case HOLD_START -> skillContainer.requestHold(playerpatch, data.arguments());
			}
		});
	}
	
	static void handleModifyPlayerModelYRot(final CPModifyEntityModelYRot data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			if (data.disable()) {
				playerpatch.disableModelYRot(false);
				EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(SPModifyPlayerData.disablePlayerYRot(playerpatch.getOriginal().getId()), playerpatch.getOriginal());
			} else {
				playerpatch.setModelYRot(data.modelYRot(), false);
				EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(SPModifyPlayerData.setPlayerYRot(playerpatch.getOriginal().getId(), data.modelYRot()), playerpatch.getOriginal());
			}
		});
	}
	
	static void handleSkillData(final CPHandleSkillData data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		Player player = context.player();
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(player, PlayerPatch.class).ifPresent(playerpatch -> {
			SkillDataManager dataManager = playerpatch.getSkill(data.skillSlot()).getDataManager();
			Object value = data.skillDataKey().value().decode(data.buffer());
			dataManager.setDataRawtype(data.skillDataKey(), value);
		});
	}
	
	static void handleSetPlayerTarget(final CPSetPlayerTarget data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(entitypatch -> {
			Entity entity = entitypatch.getOriginal().level().getEntity(data.targetEntityId());
			
			if (entity instanceof LivingEntity livingEntity) {
				entitypatch.setAttackTarget(livingEntity);
			} else if (entity == null) {
				entitypatch.setAttackTarget(null);
			}
		});
	}
	
	static void handleSetStamina(final CPSetStamina data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.setStamina(data.consumption());
			
			if (data.resetActionTick()) {
				playerpatch.resetActionTick();
			}
		});
	}
	
	static void handleUpdatePlayerInput(final CPUpdatePlayerInput data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		EpicFightCapabilities.getUnparameterizedEntityPatch(context.player(), ServerPlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.dx = data.strafe();
			playerpatch.dz = data.forward();
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new SPUpdatePlayerInput(data.entityId(), data.forward(), data.strafe()), playerpatch.getOriginal());
		});
	}
	
	static void handleSyncAnimationPosition(final BiDirectionalSyncAnimationPositionPacket data, final yesman.epicfight.network.EpicFightPayloadContext context) {
		Entity entity = context.player().level().getEntity(data.entityId());
		
		if (entity instanceof Player player) {
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(new BiDirectionalSyncAnimationPositionPacket(entity.getId(), data.elapsedTime(), data.position(), data.lerpSteps()), player);
		}
	}

    static void handleSyncEmoteSlot(final BiDirectionalSyncEmoteSlots data, final yesman.epicfight.network.EpicFightPayloadContext context) {
        EpicFightCapabilities.getPlayerPatchAsOptional(context.player().level().getEntity(data.playerId())).ifPresent(playerpatch -> {
            playerpatch.getEmoteSlots().deserialize(data.compoundTag(), playerpatch.getOriginal().registryAccess());
        });
    }
}
