package com.yesman.epicfight.neoforged.networking;

import com.yesman.akythera.core.networking.NetworkingHelper;
import com.yesman.akythera.core.networking.PayloadContext;
import com.yesman.akythera.core.networking.payload.AkytheraPayloadTypes;
import com.yesman.akythera.core.util.ClientOnly;
import com.yesman.epicfight.networking.payload.EpicFightPayloadTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;

public final class NeoForgedNetworkingHelper extends NetworkingHelper {
    @ClientOnly
    @Override
    public void sendToServer(CustomPacketPayload payload, CustomPacketPayload... others) {
        ClientPacketDistributor.sendToServer(payload, others);
    }

    @Override
    public void sendToClient(ServerPlayer serverPlayer, CustomPacketPayload payload, CustomPacketPayload... others) {
        PacketDistributor.sendToPlayer(serverPlayer, payload, others);
    }

    @Override
    public void sendToAll(CustomPacketPayload payload, CustomPacketPayload... others) {
        PacketDistributor.sendToAllPlayers(payload, others);
    }

    @Override
    public void sendToPlayersTrackingEntity(Entity entity, CustomPacketPayload payload, CustomPacketPayload... others) {
        PacketDistributor.sendToPlayersTrackingEntity(entity, payload, others);
    }

    @Override
    public void sendToPlayersTrackingChunk(ServerLevel serverLevel, ChunkPos chunkPos, CustomPacketPayload payload, CustomPacketPayload... others) {
        PacketDistributor.sendToPlayersTrackingChunk(serverLevel, chunkPos, payload, others);
    }

    private static final String PROTOCOL_VERSION = "1";

    /// Maps a payload handler to use [PayloadContext] instead [IPayloadContext]
    private static <T extends CustomPacketPayload> IPayloadHandler<@NonNull T> mapContext(BiConsumer<T, PayloadContext> original) {
        return (payload, context) -> original.accept(payload, PayloadContext.createContext(context.player()));
    }

    /// Register payload types and their handlers
    ///
    /// We do not use `bidirectional` packets since Fabric doesn't support it, and it makes
    /// multiloader integration harder.
    @SuppressWarnings("unchecked")
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar payloadRegistrar = event.registrar(PROTOCOL_VERSION);

        EpicFightPayloadTypes.CLIENT_BOUND.forEach(payloadChannel ->
            payloadRegistrar.playToClient(
                (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>)payloadChannel.streamCodec(),
                mapContext((BiConsumer<CustomPacketPayload, PayloadContext>)payloadChannel.handler())
            )
        );

        EpicFightPayloadTypes.SERVER_BOUND.forEach(payloadChannel ->
            payloadRegistrar.playToServer(
                (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>)payloadChannel.streamCodec(),
                mapContext((BiConsumer<CustomPacketPayload, PayloadContext>)payloadChannel.handler())
            )
        );

        EpicFightPayloadTypes.BI_DIRECTIONAL.forEach(payloadChannel ->
            payloadRegistrar.playBidirectional(
                (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>)payloadChannel.streamCodec(),
                mapContext((BiConsumer<CustomPacketPayload, PayloadContext>)payloadChannel.handler())
            )
        );
    }
}
