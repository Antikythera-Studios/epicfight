package yesman.epicfight.network;

import net.minecraft.network.protocol.PacketFlow;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import yesman.epicfight.EpicFight;
import yesman.epicfight.network.common.BiDirectionalAnimationVariable;
import yesman.epicfight.network.common.BiDirectionalSyncAnimationPositionPacket;
import yesman.epicfight.network.common.BiDirectionalSyncEmoteSlots;
import yesman.epicfight.network.server.SPAbsorption;
import yesman.epicfight.network.server.SPAddLearnedSkill;
import yesman.epicfight.network.server.SPAnimatorControl;
import yesman.epicfight.network.server.SPChangeGamerule;
import yesman.epicfight.network.server.SPChangeLivingMotion;
import yesman.epicfight.network.server.SPChangePlayerMode;
import yesman.epicfight.network.server.SPChangeSkill;
import yesman.epicfight.network.server.SPClearSkills;
import yesman.epicfight.network.server.SPCreateTerrainFracture;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.network.server.SPHandleSkillData;
import yesman.epicfight.network.server.SPInitSkills;
import yesman.epicfight.network.server.SPMobEffectControl;
import yesman.epicfight.network.server.SPModifyExpandedEntityData;
import yesman.epicfight.network.server.SPModifyPlayerData;
import yesman.epicfight.network.server.SPPlayUISound;
import yesman.epicfight.network.server.SPRemoveSkillAndLearn;
import yesman.epicfight.network.server.SPSetAttackTarget;
import yesman.epicfight.network.server.SPSetRemotePlayerSkill;
import yesman.epicfight.network.server.SPSetSkillContainerValue;
import yesman.epicfight.network.server.SPSkillFeedback;
import yesman.epicfight.network.server.SPUpdatePlayerInput;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Registers all client-bound payload handlers with {@link ClientPlayNetworking}.
 *
 * <p>Must be called from {@code onInitializeClient} only — references client-only classes.</p>
 */
public final class EpicFightClientPayloadRegistration {

    private EpicFightClientPayloadRegistration() {}

    /**
     * Registers all client-bound payload handlers with {@link ClientPlayNetworking}.
     *
     * <p>The handlers delegate to {@link EpicFightClientBoundPayloadHandler}.</p>
     */
    public static void registerClientHandlers() {
        // SP* payloads (server → client)
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_ABSORPTION,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleAbsorption(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_ADD_LEARNED_SKILL,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleAddLearnedSkill(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_HANDLE_SKILL_DATA,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSkillData(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_ANIMATOR_CONTROL,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleAnimatorControl(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_GAMERULE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleChangeGameRule(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_LIVING_MOTION,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleChangeLivingMotion(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_PLAYER_MODE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleChangePlayerMode(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_SKILL,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleChangeSkill(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CLEAR_SKILLS,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleClearSkills(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_DATAPACK_SYNC,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleDataPack(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_ENTITY_PAIRING,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleEntityPairing(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_CREATE_FRACTURE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleFracture(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_MOB_EFFECT,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleMobEffect(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_MODIFY_EXPANDED_ENTITY_DATA,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleModifyExpandedEntityData(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_DISABLE_Y_ROT,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleDisableModelYRot(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_GRAPPLING_TARGET,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetGrapplingTarget(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_LAST_ATTACK_RESULT,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetLastAttackResult(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_PLAYER_MODE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetPlayerMode(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_PLAYER_Y_ROT,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleModelYRot(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_PLAY_UI_SOUND,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handlePlayUiSound(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_REMOVE_SKILL,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleRemoveSkill(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_ATTACK_TARGET,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetAttackTarget(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_REMOTE_PLAYER_SKILL,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetRemotePlayerSkill(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SET_SKILL_CONTAINER_VALUE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSetSkillContainerValue(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_SKILL_FEEDBACK,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSkillFeedback(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_UPDATE_PLAYER_INPUT,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleUpdatePlayerInput(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.CLIENT_BOUND_INIT_SKILLS,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleInitSkills(payload, toClientContext(ctx)));

        // Bi-directional payloads (server → client side)
        registerClientHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_MODIFY_ANIMATION_VARIABLE,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleAnimationVariablePacket(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_ANIMATION_POSITION,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSyncAnimationPosition(payload, toClientContext(ctx)));
        registerClientHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_EMOTE_SLOTS,
            (payload, ctx) -> EpicFightClientBoundPayloadHandler.handleSyncEmoteSlot(payload, toClientContext(ctx)));

        EpicFight.LOGGER.info("EpicFight client payload handlers registered (30)");
    }

    private static <T extends CustomPacketPayload> void registerClientHandler(
            CustomPacketPayload.Type<T> type,
            ClientPlayNetworking.PlayPayloadHandler<T> handler) {
        try {
            ClientPlayNetworking.registerGlobalReceiver(type, handler);
        } catch (IllegalStateException e) {
            EpicFight.LOGGER.debug("Client handler already registered: {} ({})", type.id(), e.getMessage());
        }
    }

    private static EpicFightPayloadContext toClientContext(ClientPlayNetworking.Context ctx) {
        return new EpicFightPayloadContext(ctx.player(), null, PacketFlow.CLIENTBOUND);
    }
}
