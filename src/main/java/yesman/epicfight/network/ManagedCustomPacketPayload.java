package yesman.epicfight.network;

import com.google.common.collect.Maps;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.EpicFight;
import yesman.epicfight.network.client.*;
import yesman.epicfight.network.common.BiDirectionalAnimationVariable;
import yesman.epicfight.network.common.BiDirectionalSyncAnimationPositionPacket;
import yesman.epicfight.network.common.BiDirectionalSyncEmoteSlots;
import yesman.epicfight.network.server.*;

import java.util.Map;
import java.util.NoSuchElementException;

public interface ManagedCustomPacketPayload extends CustomPacketPayload {
	Map<Class<? extends CustomPacketPayload>, CustomPacketPayload.Type<?>> PAYLOAD_TYPES = Maps.newHashMap();
	
	// Client bound payloads
	CustomPacketPayload.Type<SPAbsorption> CLIENT_BOUND_ABSORPTION = registerPayloadType(SPAbsorption.class, EpicFight.MODID, "client_bound_absorption");
	CustomPacketPayload.Type<SPAddLearnedSkill> CLIENT_BOUND_ADD_LEARNED_SKILL = registerPayloadType(SPAddLearnedSkill.class, EpicFight.MODID, "client_bound_add_learned_skill");
	CustomPacketPayload.Type<SPHandleSkillData> CLIENT_BOUND_HANDLE_SKILL_DATA = registerPayloadType(SPHandleSkillData.class, EpicFight.MODID, "client_bound_register_or_remove_skill_data");
	CustomPacketPayload.Type<SPAnimatorControl> CLIENT_BOUND_ANIMATOR_CONTROL = registerPayloadType(SPAnimatorControl.class, EpicFight.MODID, "client_bound_animator_control");
	CustomPacketPayload.Type<SPChangeGamerule> CLIENT_BOUND_CHANGE_GAMERULE = registerPayloadType(SPChangeGamerule.class, EpicFight.MODID, "client_bound_change_gamerule");
	CustomPacketPayload.Type<SPChangeLivingMotion> CLIENT_BOUND_CHANGE_LIVING_MOTION = registerPayloadType(SPChangeLivingMotion.class, EpicFight.MODID, "client_bound_change_living_motion");
	CustomPacketPayload.Type<SPChangePlayerMode> CLIENT_BOUND_CHANGE_PLAYER_MODE = registerPayloadType(SPChangePlayerMode.class, EpicFight.MODID, "client_bound_change_player_mode");
	CustomPacketPayload.Type<SPChangeSkill> CLIENT_BOUND_CHANGE_SKILL = registerPayloadType(SPChangeSkill.class, EpicFight.MODID, "client_bound_change_skill");
	CustomPacketPayload.Type<SPClearSkills> CLIENT_BOUND_CLEAR_SKILLS = registerPayloadType(SPClearSkills.class, EpicFight.MODID, "client_bound_clear_skills");
	CustomPacketPayload.Type<SPDatapackSync> CLIENT_BOUND_DATAPACK_SYNC = registerPayloadType(SPDatapackSync.class, EpicFight.MODID, "client_bound_sycn_datapack");
	CustomPacketPayload.Type<SPEntityPairingPacket> CLIENT_BOUND_ENTITY_PAIRING = registerPayloadType(SPEntityPairingPacket.class, EpicFight.MODID, "client_bound_entity_pairing");
	CustomPacketPayload.Type<SPCreateTerrainFracture> CLIENT_BOUND_CREATE_FRACTURE = registerPayloadType(SPCreateTerrainFracture.class, EpicFight.MODID, "client_bound_create_terrain_fracture");
	CustomPacketPayload.Type<SPMobEffectControl> CLIENT_BOUND_MOB_EFFECT = registerPayloadType(SPMobEffectControl.class, EpicFight.MODID, "client_bound_share_mob_effect_to_tracking_players");
	CustomPacketPayload.Type<SPModifyExpandedEntityData> CLIENT_BOUND_MODIFY_EXPANDED_ENTITY_DATA = registerPayloadType(SPModifyExpandedEntityData.class, EpicFight.MODID, "client_bound_modify_expanded_entity_data");
	CustomPacketPayload.Type<SPModifyPlayerData.DisablePlayerYRot> CLIENT_BOUND_DISABLE_Y_ROT = registerPayloadType(SPModifyPlayerData.DisablePlayerYRot.class, EpicFight.MODID, "client_bound_disable_player_y_rot");
	CustomPacketPayload.Type<SPModifyPlayerData.SetGrapplingTarget> CLIENT_BOUND_SET_GRAPPLING_TARGET = registerPayloadType(SPModifyPlayerData.SetGrapplingTarget.class, EpicFight.MODID, "client_bound_set_grappling_target");
	CustomPacketPayload.Type<SPModifyPlayerData.SetLastAttackResult> CLIENT_BOUND_SET_LAST_ATTACK_RESULT = registerPayloadType(SPModifyPlayerData.SetLastAttackResult.class, EpicFight.MODID, "client_bound_set_lasst_attack_result");
	CustomPacketPayload.Type<SPModifyPlayerData.SetPlayerMode> CLIENT_BOUND_SET_PLAYER_MODE = registerPayloadType(SPModifyPlayerData.SetPlayerMode.class, EpicFight.MODID, "client_bound_set_player_mode");
	CustomPacketPayload.Type<SPModifyPlayerData.SetPlayerYRot> CLIENT_BOUND_SET_PLAYER_Y_ROT = registerPayloadType(SPModifyPlayerData.SetPlayerYRot.class, EpicFight.MODID, "client_bound_set_player_y_rot");
	CustomPacketPayload.Type<SPPlayUISound> CLIENT_BOUND_PLAY_UI_SOUND = registerPayloadType(SPPlayUISound.class, EpicFight.MODID, "client_bound_play_ui_sound");
	CustomPacketPayload.Type<SPRemoveSkillAndLearn> CLIENT_BOUND_REMOVE_SKILL = registerPayloadType(SPRemoveSkillAndLearn.class, EpicFight.MODID, "client_bound_remove_skill");
	CustomPacketPayload.Type<SPSetAttackTarget> CLIENT_BOUND_SET_ATTACK_TARGET = registerPayloadType(SPSetAttackTarget.class, EpicFight.MODID, "client_bound_set_target");
	CustomPacketPayload.Type<SPSetRemotePlayerSkill> CLIENT_BOUND_SET_REMOTE_PLAYER_SKILL = registerPayloadType(SPSetRemotePlayerSkill.class, EpicFight.MODID, "client_bound_set_remote_player_skill");
	CustomPacketPayload.Type<SPSetSkillContainerValue> CLIENT_BOUND_SET_SKILL_CONTAINER_VALUE = registerPayloadType(SPSetSkillContainerValue.class, EpicFight.MODID, "client_bound_set_skill_container_values");
	CustomPacketPayload.Type<SPSkillFeedback> CLIENT_BOUND_SKILL_FEEDBACK = registerPayloadType(SPSkillFeedback.class, EpicFight.MODID, "client_bound_skill_feedback");
	CustomPacketPayload.Type<SPUpdatePlayerInput> CLIENT_BOUND_UPDATE_PLAYER_INPUT = registerPayloadType(SPUpdatePlayerInput.class, EpicFight.MODID, "client_bound_update_player_input");
	CustomPacketPayload.Type<SPInitSkills> CLIENT_BOUND_INIT_SKILLS = registerPayloadType(SPInitSkills.class, EpicFight.MODID, "client_bound_init_skills");
	
	// Server bound payloads
	CustomPacketPayload.Type<CPAnimatorControl> SERVER_BOUND_ANIMATOR_CONTROL = registerPayloadType(CPAnimatorControl.class, EpicFight.MODID, "server_bound_animator_control");
	CustomPacketPayload.Type<CPChangePlayerMode> SERVER_BOUND_CHANGE_PLAYER_MODE = registerPayloadType(CPChangePlayerMode.class, EpicFight.MODID, "server_bound_change_player_mode");
	CustomPacketPayload.Type<CPChangeSkill> SERVER_BOUND_CHANGE_SKILL = registerPayloadType(CPChangeSkill.class, EpicFight.MODID, "server_bound_change_skill");
	CustomPacketPayload.Type<CPSkillRequest> SERVER_BOUND_EXECUTE_SKILL = registerPayloadType(CPSkillRequest.class, EpicFight.MODID, "server_bound_skill_request");
	CustomPacketPayload.Type<CPModifyEntityModelYRot> SERVER_BOUND_MODIFY_ENTITY_PATCH_Y_ROT = registerPayloadType(CPModifyEntityModelYRot.class, EpicFight.MODID, "server_bound_modify_entitypatch_y_rot");
	CustomPacketPayload.Type<CPHandleSkillData> SERVER_BOUND_MODIFY_SKILL_DATA = registerPayloadType(CPHandleSkillData.class, EpicFight.MODID, "server_bound_modify_skill_data");
	CustomPacketPayload.Type<CPPairingAnimationRegistry> SERVER_BOUND_PAIRING_ANIMATION_REGISTRY = registerPayloadType(CPPairingAnimationRegistry.class, EpicFight.MODID, "server_bound_pairing_animation_registry");
	CustomPacketPayload.Type<CPSetPlayerTarget> SERVER_BOUND_SET_PLAYER_TARGET = registerPayloadType(CPSetPlayerTarget.class, EpicFight.MODID, "server_bound_set_player_target");
	CustomPacketPayload.Type<CPSetStamina> SERVER_BOUND_SET_STAMINA = registerPayloadType(CPSetStamina.class, EpicFight.MODID, "server_bound_set_stamina");
	CustomPacketPayload.Type<CPUpdatePlayerInput> SERVER_BOUND_UPDATE_PLAYER_INPUT = registerPayloadType(CPUpdatePlayerInput.class, EpicFight.MODID, "server_bound_update_player_input");
	
	// Bi-directional payloads
	CustomPacketPayload.Type<BiDirectionalAnimationVariable> BI_DIRECTIONAL_MODIFY_ANIMATION_VARIABLE = registerPayloadType(BiDirectionalAnimationVariable.class, EpicFight.MODID, "bi_directional_modify_animation_variable");
	CustomPacketPayload.Type<BiDirectionalSyncAnimationPositionPacket> BI_DIRECTIONAL_SYNC_ANIMATION_POSITION = registerPayloadType(BiDirectionalSyncAnimationPositionPacket.class, EpicFight.MODID, "bi_directional_update_player_input");
    CustomPacketPayload.Type<BiDirectionalSyncEmoteSlots> BI_DIRECTIONAL_SYNC_EMOTE_SLOTS = registerPayloadType(BiDirectionalSyncEmoteSlots.class, EpicFight.MODID, "bi_directional_emote_slots");

	static <T extends ManagedCustomPacketPayload> CustomPacketPayload.Type<T> registerPayloadType(Class<T> type, String modid, String payloadId) {
		CustomPacketPayload.Type<T> packet = new CustomPacketPayload.Type<T> (ResourceLocation.fromNamespaceAndPath(modid, payloadId));
		PAYLOAD_TYPES.put(type, packet);
		
		return packet;
	}
	
	@Override
	default CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
		if (!PAYLOAD_TYPES.containsKey(this.getClass())) {
			throw new NoSuchElementException("Unregistered packet: " + this.getClass());
		}
		
		return PAYLOAD_TYPES.get(this.getClass());
	}
}
