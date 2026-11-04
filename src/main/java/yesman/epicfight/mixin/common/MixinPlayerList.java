package yesman.epicfight.mixin.common;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.api.event.impl.VanillaPlayerEventHooks;

import java.io.File;

/// Mixin for [PlayerList] to wire [PlayerEvent.LoadFromFile].
///
/// In NeoForge, this event fires when a player's data is loaded from disk.
/// In Fabric, we intercept [PlayerList.load] which is called when a player logs in.
///
/// Note: Entity join level is handled by ServerEntityEvents.ENTITY_LOAD registered in
/// EpicFightFabric, not by a mixin here. This is the standard Fabric API approach.
@Mixin(value = PlayerList.class)
public abstract class MixinPlayerList {

    @Shadow
    private PlayerDataStorage playerIo;

    @Inject(
        at = @At(value = "RETURN"),
        method = "load(Lnet/minecraft/server/level/ServerPlayer;)Ljava/util/Optional;"
    )
    private void epicfight$load(ServerPlayer serverPlayer, CallbackInfoReturnable<java.util.Optional<net.minecraft.nbt.CompoundTag>> info) {
        // Get the player data directory from the player data storage
        File playerDirectory = getPlayerDir();
        VanillaPlayerEventHooks.onLoad(serverPlayer, playerDirectory, serverPlayer.getStringUUID());
    }

    private File getPlayerDir() {
        // Access the playerDir field via reflection since it's private in PlayerDataStorage
        try {
            java.lang.reflect.Field field = PlayerDataStorage.class.getDeclaredField("playerDir");
            field.setAccessible(true);
            return (File) field.get(this.playerIo);
        } catch (Exception e) {
            return new File(".");
        }
    }
}
