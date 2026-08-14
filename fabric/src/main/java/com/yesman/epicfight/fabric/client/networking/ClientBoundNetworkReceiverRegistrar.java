package com.yesman.epicfight.fabric.client.networking;

import com.yesman.akythera.core.networking.PayloadContext;
import com.yesman.akythera.core.networking.payload.AkytheraPayloadTypes;
import com.yesman.epicfight.networking.payload.EpicFightPayloadTypes;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class ClientBoundNetworkReceiverRegistrar {
    /// Maps a payload handler to use [PayloadContext] instead [ClientPlayNetworking.Context]
    private static <T extends CustomPacketPayload> ClientPlayNetworking.PlayPayloadHandler<@NonNull T> mapContext(BiConsumer<T, PayloadContext> original) {
        return (payload, context) -> original.accept(payload, PayloadContext.createContext(context.player()));
    }

    // Register stream codecs
    public static void registerTypes() {
        Stream.concat(EpicFightPayloadTypes.CLIENT_BOUND.stream(), EpicFightPayloadTypes.BI_DIRECTIONAL.stream()).forEach(
            payloadChannel ->
                PayloadTypeRegistry.clientboundPlay().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                    (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>) payloadChannel.streamCodec()
                )
        );
    }

    // Register receivers
    public static void registerReceivers() {
        Stream.concat(EpicFightPayloadTypes.CLIENT_BOUND.stream(), EpicFightPayloadTypes.BI_DIRECTIONAL.stream()).forEach(
            payloadChannel ->
                ClientPlayNetworking.registerGlobalReceiver(
                    (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                    mapContext((BiConsumer<CustomPacketPayload, PayloadContext>)payloadChannel.handler())
                )
        );
    }

    private ClientBoundNetworkReceiverRegistrar() {}
}
