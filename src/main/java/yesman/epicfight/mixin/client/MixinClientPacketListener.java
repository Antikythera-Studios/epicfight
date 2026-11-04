package yesman.epicfight.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.client.event.impl.VanillaGeneralClientEventHooks;

@Mixin(value = ClientPacketListener.class)
public abstract class MixinClientPacketListener {
    // Captured at handleRespawn HEAD, used at handleRespawn RETURN
    private LocalPlayer epicfight$oldPlayer;

    @Inject(at = @At(value = "HEAD"), method = "handleRespawn(Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;)V", cancellable = false)
    private void epicfight$handleRespawnHead(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo info) {
        VanillaGeneralClientEventHooks.packet = clientboundRespawnPacket;
        this.epicfight$oldPlayer = Minecraft.getInstance().player;
        EpicFight.LOGGER.info("[EpicFight] MixinClientPacketListener.handleRespawn HEAD fired");
    }

    @Inject(at = @At(value = "RETURN"), method = "handleRespawn(Lnet/minecraft/network/protocol/game/ClientboundRespawnPacket;)V", cancellable = false)
    private void epicfight$handleRespawnReturn(ClientboundRespawnPacket clientboundRespawnPacket, CallbackInfo info) {
        LocalPlayer newPlayer = Minecraft.getInstance().player;
        EpicFight.LOGGER.info("[EpicFight] MixinClientPacketListener.handleRespawn RETURN fired, old={}, new={}", this.epicfight$oldPlayer, newPlayer);
        if (this.epicfight$oldPlayer != null && newPlayer != null) {
            VanillaGeneralClientEventHooks.onClonedInClient(this.epicfight$oldPlayer, newPlayer);
        }
        this.epicfight$oldPlayer = null;
    }

    @Inject(at = @At(value = "RETURN"), method = "handleLogin(Lnet/minecraft/network/protocol/game/ClientboundLoginPacket;)V", cancellable = false)
    private void epicfight$handleLogin(ClientboundLoginPacket clientboundLoginPacket, CallbackInfo info) {
        LocalPlayer player = Minecraft.getInstance().player;
        EpicFight.LOGGER.info("[EpicFight] MixinClientPacketListener.handleLogin RETURN fired, player={}", player);
        if (player != null) {
            VanillaGeneralClientEventHooks.onPlayerLoggedIn(player);
        }
    }

    @Inject(at = @At(value = "HEAD"), method = "close()V", cancellable = false)
    private void epicfight$close(CallbackInfo info) {
        LocalPlayer player = Minecraft.getInstance().player;
        EpicFight.LOGGER.info("[EpicFight] MixinClientPacketListener.close fired, player={}", player);
        if (player != null) {
            VanillaGeneralClientEventHooks.onPlayerLoggedOut(player);
        }
    }
}
