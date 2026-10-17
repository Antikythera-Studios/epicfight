package yesman.epicfight.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import yesman.epicfight.EpicFight;
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

/**
 * Registers all EpicFight custom payload codecs and server-side handlers with Fabric's networking API.
 *
 * <p>Codec registration is required for Fabric to know how to serialize/deserialize custom payload types.
 * Without it, Fabric falls back to {@code DiscardedPayload}, causing {@code ClassCastException} when
 * sending custom payload types.</p>
 *
 * <p>Handler registration wires each payload type to its handler method in
 * {@link EpicFightServerBoundPayloadHandler}.</p>
 */
public final class EpicFightPayloadRegistration {

    private EpicFightPayloadRegistration() {}

    // ── Codec registration ──────────────────────────────────────────────────

    /**
     * Registers all payload codecs with Fabric's {@link PayloadTypeRegistry}.
     *
     * <p>Must be called on both client and server (from {@code onInitialize}).
     * Bi-directional payloads are registered in both C2S and S2C registries.</p>
     */
    @SuppressWarnings("unchecked")
    public static void registerCodecs() {
        PayloadTypeRegistry<RegistryFriendlyByteBuf> c2s = PayloadTypeRegistry.playC2S();
        PayloadTypeRegistry<RegistryFriendlyByteBuf> s2c = PayloadTypeRegistry.playS2C();

        // ── Server-bound (C2S) payloads ──
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_ANIMATOR_CONTROL, CPAnimatorControl.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_CHANGE_PLAYER_MODE, CPChangePlayerMode.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_CHANGE_SKILL, CPChangeSkill.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_EXECUTE_SKILL, CPSkillRequest.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_MODIFY_ENTITY_PATCH_Y_ROT, CPModifyEntityModelYRot.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_MODIFY_SKILL_DATA, CPHandleSkillData.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_PAIRING_ANIMATION_REGISTRY, CPPairingAnimationRegistry.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_SET_PLAYER_TARGET, CPSetPlayerTarget.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_SET_STAMINA, CPSetStamina.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.SERVER_BOUND_UPDATE_PLAYER_INPUT, CPUpdatePlayerInput.STREAM_CODEC);

        // ── Bi-directional payloads (registered in both C2S and S2C) ──
        registerCodec(c2s, ManagedCustomPacketPayload.BI_DIRECTIONAL_MODIFY_ANIMATION_VARIABLE, BiDirectionalAnimationVariable.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_ANIMATION_POSITION, BiDirectionalSyncAnimationPositionPacket.STREAM_CODEC);
        registerCodec(c2s, ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_EMOTE_SLOTS, BiDirectionalSyncEmoteSlots.STREAM_CODEC);

        registerCodec(s2c, ManagedCustomPacketPayload.BI_DIRECTIONAL_MODIFY_ANIMATION_VARIABLE, BiDirectionalAnimationVariable.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_ANIMATION_POSITION, BiDirectionalSyncAnimationPositionPacket.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_EMOTE_SLOTS, BiDirectionalSyncEmoteSlots.STREAM_CODEC);

        // ── Client-bound (S2C) payloads ──
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_ABSORPTION, SPAbsorption.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_ADD_LEARNED_SKILL, SPAddLearnedSkill.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_HANDLE_SKILL_DATA, SPHandleSkillData.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_ANIMATOR_CONTROL, SPAnimatorControl.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_GAMERULE, SPChangeGamerule.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_LIVING_MOTION, SPChangeLivingMotion.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_PLAYER_MODE, SPChangePlayerMode.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CHANGE_SKILL, SPChangeSkill.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CLEAR_SKILLS, SPClearSkills.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_DATAPACK_SYNC, SPDatapackSync.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_ENTITY_PAIRING, SPEntityPairingPacket.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_CREATE_FRACTURE, SPCreateTerrainFracture.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_MOB_EFFECT, SPMobEffectControl.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_MODIFY_EXPANDED_ENTITY_DATA, SPModifyExpandedEntityData.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_DISABLE_Y_ROT, SPModifyPlayerData.DisablePlayerYRot.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_GRAPPLING_TARGET, SPModifyPlayerData.SetGrapplingTarget.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_LAST_ATTACK_RESULT, SPModifyPlayerData.SetLastAttackResult.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_PLAYER_MODE, SPModifyPlayerData.SetPlayerMode.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_PLAYER_Y_ROT, SPModifyPlayerData.SetPlayerYRot.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_PLAY_UI_SOUND, SPPlayUISound.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_REMOVE_SKILL, SPRemoveSkillAndLearn.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_ATTACK_TARGET, SPSetAttackTarget.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_REMOTE_PLAYER_SKILL, SPSetRemotePlayerSkill.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SET_SKILL_CONTAINER_VALUE, SPSetSkillContainerValue.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_SKILL_FEEDBACK, SPSkillFeedback.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_UPDATE_PLAYER_INPUT, SPUpdatePlayerInput.STREAM_CODEC);
        registerCodec(s2c, ManagedCustomPacketPayload.CLIENT_BOUND_INIT_SKILLS, SPInitSkills.STREAM_CODEC);

        EpicFight.LOGGER.info("EpicFight payload codecs registered (C2S: 13, S2C: 30)");
    }

    private static <T extends CustomPacketPayload> void registerCodec(
            PayloadTypeRegistry<RegistryFriendlyByteBuf> registry,
            CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        try {
            registry.register(type, codec);
        } catch (IllegalStateException e) {
            EpicFight.LOGGER.debug("Payload codec already registered: {} ({})", type.id(), e.getMessage());
        }
    }

    // ── Server-side handler registration ────────────────────────────────────

    /**
     * Registers all server-bound payload handlers with {@link ServerPlayNetworking}.
     *
     * <p>Must be called from {@code onInitialize} (runs on both client and server).
     * The handlers delegate to {@link EpicFightServerBoundPayloadHandler}.</p>
     */
    public static void registerServerHandlers() {
        // CP* payloads (client → server)
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_ANIMATOR_CONTROL,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleAnimatorControl(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_CHANGE_PLAYER_MODE,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleChangePlayerMode(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_CHANGE_SKILL,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleChangeSkill(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_EXECUTE_SKILL,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleExecuteSkill(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_MODIFY_ENTITY_PATCH_Y_ROT,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleModifyPlayerModelYRot(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_MODIFY_SKILL_DATA,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleSkillData(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_PAIRING_ANIMATION_REGISTRY,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handlePairingAnimationRegistry(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_SET_PLAYER_TARGET,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleSetPlayerTarget(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_SET_STAMINA,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleSetStamina(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.SERVER_BOUND_UPDATE_PLAYER_INPUT,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleUpdatePlayerInput(payload, toServerContext(ctx)));

        // Bi-directional payloads (client → server side)
        registerServerHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_MODIFY_ANIMATION_VARIABLE,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleAnimationVariablePacket(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_ANIMATION_POSITION,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleSyncAnimationPosition(payload, toServerContext(ctx)));
        registerServerHandler(ManagedCustomPacketPayload.BI_DIRECTIONAL_SYNC_EMOTE_SLOTS,
            (payload, ctx) -> EpicFightServerBoundPayloadHandler.handleSyncEmoteSlot(payload, toServerContext(ctx)));

        EpicFight.LOGGER.info("EpicFight server payload handlers registered (13)");
    }

    private static <T extends CustomPacketPayload> void registerServerHandler(
            CustomPacketPayload.Type<T> type,
            ServerPlayNetworking.PlayPayloadHandler<T> handler) {
        try {
            ServerPlayNetworking.registerGlobalReceiver(type, handler);
        } catch (IllegalStateException e) {
            EpicFight.LOGGER.debug("Server handler already registered: {} ({})", type.id(), e.getMessage());
        }
    }

    private static EpicFightPayloadContext toServerContext(ServerPlayNetworking.Context ctx) {
        return new EpicFightPayloadContext(ctx.player(), ctx.server(), PacketFlow.SERVERBOUND);
    }
}
