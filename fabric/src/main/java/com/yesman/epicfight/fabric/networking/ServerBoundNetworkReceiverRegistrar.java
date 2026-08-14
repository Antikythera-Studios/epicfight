package com.yesman.epicfight.fabric.networking;

import com.yesman.akythera.core.networking.PayloadContext;
import com.yesman.akythera.core.networking.payload.AkytheraPayloadTypes;
import com.yesman.epicfight.networking.payload.EpicFightPayloadTypes;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jspecify.annotations.NonNull;

import java.util.function.BiConsumer;
import java.util.stream.Stream;

@SuppressWarnings("unchecked")
public final class ServerBoundNetworkReceiverRegistrar {
    /// Maps a payload handler to use [PayloadContext] instead [ServerPlayNetworking.Context]
    private static <T extends CustomPacketPayload> ServerPlayNetworking.PlayPayloadHandler<@NonNull T> mapContext(BiConsumer<T, PayloadContext> original) {
        return (payload, context) -> {
            original.accept(payload, PayloadContext.createContext(context.player()));
        };
    }

    // Register stream codecs
    public static void registerTypes() {
        Stream.concat(EpicFightPayloadTypes.SERVER_BOUND.stream(), EpicFightPayloadTypes.BI_DIRECTIONAL.stream()).forEach(
            payloadChannel ->
                PayloadTypeRegistry.serverboundPlay().register(
                    (CustomPacketPayload.Type<CustomPacketPayload>)payloadChannel.type(),
                    (StreamCodec<? super RegistryFriendlyByteBuf, CustomPacketPayload>) payloadChannel.streamCodec()
                )
        );
    }

    // Register receivers
    public static void registerReceivers() {
        Stream.concat(EpicFightPayloadTypes.SERVER_BOUND.stream(), EpicFightPayloadTypes.BI_DIRECTIONAL.stream()).forEach(
            payloadChannel ->
                ServerPlayNetworking.registerGlobalReceiver(
                    (CustomPacketPayload.Type<CustomPacketPayload>) payloadChannel.type(),
                    mapContext((BiConsumer<CustomPacketPayload, PayloadContext>) payloadChannel.handler())
                )
        );
    }

    private ServerBoundNetworkReceiverRegistrar() {}
}
