package yesman.epicfight.api.client.event.impl;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import yesman.epicfight.api.client.camera.EpicFightCameraAPI;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.player.StartUsingItemEvent;
import yesman.epicfight.client.events.engine.ControlEngine;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.world.capabilites.entitypatch.player.LocalPlayerPatch;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.level.block.FractureBlockState;

public final class VanillaGeneralClientEventHooks {
    /// Called when the player starts using an item in hand
    ///
    /// @see MultiPlayerGameMode#useItem
    ///
    /// @return whether cancel the event
    public static boolean onUseItemInClientSide(Player player, ItemStack itemStack, InteractionHand hand) {
        // Server item use event is fired in {@link PlayerEvents#rightClickItemServerEvent}
        if (!player.level().isClientSide()) {
            return false;
        }

        LocalPlayerPatch playerpatch = EpicFightCapabilities.ClientModule.getLocalPlayerPatch((LocalPlayer)player);

        if (playerpatch != null) {
            if (!playerpatch.getEntityState().canUseItem()) {
                return true;
            } else if (playerpatch.getOriginal().getOffhandItem().getUseAnimation() == UseAnim.NONE) {
                StartUsingItemEvent startUsingItemEvent = new StartUsingItemEvent(playerpatch);
                EpicFightEventHooks.Player.USE_ITEM.postWithListener(startUsingItemEvent, playerpatch.getEventListener());

                if (playerpatch.getEntityState().movementLocked()) {
                    startUsingItemEvent.cancel();
                }

                if (!startUsingItemEvent.isCanceled()) {
                    EpicFightCameraAPI.getInstance().onItemUseEvent(player, playerpatch, itemStack, hand);
                }

                return startUsingItemEvent.isCanceled();
            }
        }

        return false;
    }

    /// Called when the player
    ///
    /// @see ClientPacketListener#handleLogin
    public static void onPlayerLoggedIn(LocalPlayer player) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, LocalPlayerPatch.class).ifPresent(entitypatch -> {
            ControlEngine.getInstance().reloadPlayerPatch(entitypatch);
            RenderEngine.getInstance().initHUD(entitypatch);
        });
    }

    /// Bad code: should be fixed after Forge provides any parameters that can figure out if respawning caused by dimension changes
    public static ClientboundRespawnPacket packet;

    /// Called when the player is cloned in the client side
    ///
    /// @see ClientPacketListener#handleRespawn
    @SuppressWarnings("unchecked")
    public static void onClonedInClient(LocalPlayer oldPlayer, LocalPlayer newPlayer) {
        LocalPlayerPatch oldCap = EpicFightCapabilities.getEntityPatch(oldPlayer, LocalPlayerPatch.class);
        LocalPlayerPatch newCap = EpicFightCapabilities.getEntityPatch(newPlayer, LocalPlayerPatch.class);

        // when a player revives after it disappears, oldCap == null
        if (oldCap != null && newCap != null) {
            if (packet != null && packet.shouldKeep((byte)3)) {
                newPlayer.tickCount = oldPlayer.tickCount;
                newCap.copyOldData(oldCap, false);
            }

            packet = null;
            newCap.onRespawnLocalPlayer(newPlayer);
            newCap.toMode(oldCap.getPlayerMode(), false);
        }

        EpicFightGameRules.GAME_RULES.values().forEach(gamerule -> {
            Object val = gamerule.getRuleValue(oldPlayer.level());
            ((EpicFightGameRules.ConfigurableGameRule<Object, ?, ?>)gamerule).setRuleValue(newPlayer.level(), val);
        });

        ControlEngine.getInstance().reloadPlayerPatch(newCap);
        RenderEngine.getInstance().initHUD(newCap);
    }

    public static void onPlayerLoggedOut(LocalPlayer player) {
        ItemCapabilityReloadListener.reset();
        EpicFightCapabilities.ITEM_CAPABILITY_PROVIDER.clear();
        EpicFightCapabilities.ENTITY_PATCH_PROVIDER.clearDatapackEntities();
        WeaponTypeReloadListener.clear();
        RenderEngine.getInstance().clear();
        FractureBlockState.reset();
    }

    private VanillaGeneralClientEventHooks() {
    }
}
