package yesman.epicfight.mixin.common;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.impl.VanillaPlayerEventHooks;

/// Mixin for [ServerPlayer] and [PlayerList] to wire NeoForge player events.
///
/// Replaces:
/// - PlayerEvent.StartTracking / StopTracking
/// - PlayerEvent.LoadFromFile
/// - PlayerEvent.Clone
/// - PlayerEvent.PlayerChangedDimensionEvent
/// - ArrowLooseEvent
@Mixin(value = ServerPlayer.class)
public abstract class MixinServerPlayerEvents {

    /// PlayerEvent.PlayerChangedDimensionEvent — fires when a player changes dimension
    @Inject(at = @At(value = "TAIL"), method = "teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FF)Z")
    private void epicfight$changeDimension(CallbackInfoReturnable<Boolean> info) {
        ServerPlayer self = (ServerPlayer)(Object)this;
        VanillaPlayerEventHooks.onChagneDimension(self);
    }

    /// PlayerEvent.Clone — fires when a player is cloned during respawn
    @Inject(at = @At(value = "TAIL"), method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V")
    private void epicfight$restoreFrom(ServerPlayer oldPlayer, boolean wasDeath, CallbackInfo info) {
        ServerPlayer self = (ServerPlayer)(Object)this;
        VanillaPlayerEventHooks.onCloned(oldPlayer, self, wasDeath);
    }
}
