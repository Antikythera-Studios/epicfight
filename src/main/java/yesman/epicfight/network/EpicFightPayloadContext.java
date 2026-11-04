package yesman.epicfight.network;

import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/// Wrapper for Fabric networking context.
/// On NeoForge, payload handlers received [IPayloadContext].
/// On Fabric, we wrap the relevant context info in this class.
public class EpicFightPayloadContext {
    private final Player player;
    private final MinecraftServer server;
    private final PacketFlow flow;

    public EpicFightPayloadContext(Player player, MinecraftServer server, PacketFlow flow) {
        this.player = player;
        this.server = server;
        this.flow = flow;
    }

    public Player player() {
        return player;
    }

    public ServerPlayer playerAsServer() {
        return (ServerPlayer) player;
    }

    public MinecraftServer server() {
        return server;
    }

    public PacketFlow flow() {
        return flow;
    }

    public void reply(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        if (this.player instanceof ServerPlayer serverPlayer && serverPlayer.connection != null) {
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(serverPlayer, payload);
        }
    }
}
