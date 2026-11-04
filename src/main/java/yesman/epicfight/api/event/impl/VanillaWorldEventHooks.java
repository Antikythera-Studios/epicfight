package yesman.epicfight.api.event.impl;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPDatapackSync;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.skill.PlayerSkills;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.gamerule.EpicFightGameRules.ConfigurableGameRule;

/// World event hooks that must be triggered by either mod loader event or Mixins.
///
/// Replaces NeoForge's [NeoForgeWorldEvent].
public final class VanillaWorldEventHooks {

    /// Called when a player joins or datapacks are reloaded.
    /// Sends gamerule sync packets and datapack data to the player.
    public static void onDatapackSync(ServerPlayer player) {
        if (player == null) return;

        // Send gamerule sync packets
        EpicFightNetworkManager.PayloadBundleBuilder payloadBundleBuilder = EpicFightNetworkManager.PayloadBundleBuilder.create();

        EpicFightGameRules.GAME_RULES.values().stream().filter(ConfigurableGameRule::shouldSync).forEach(gamerule -> {
            payloadBundleBuilder.and(gamerule.getSyncPacket(player));
        });

        payloadBundleBuilder.send((first, others) -> EpicFightNetworkManager.sendToPlayer(first, player, others));

        if (!player.getServer().isSingleplayerOwner(player.getGameProfile())) {
            sendLevelData(player);
        } else {
            EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(serverplayerpatch -> {
                PlayerSkills skillCapability = serverplayerpatch.getPlayerSkills();

                skillCapability.listSkillContainers().forEach(skillContainer -> {
                    if (skillContainer.getSkill() != null) {
                        skillContainer.setSkill(skillContainer.getSkill(), true);
                    }
                });
            });
        }
    }

    /// Called when datapacks are reloaded for all players.
    public static void onDatapackSyncAll(MinecraftServer server) {
        server.getPlayerList().getPlayers().forEach(VanillaWorldEventHooks::sendLevelData);
    }

    /// Sends all EpicFight datapack data to a player.
    /// This is the Fabric equivalent of NeoForgeWorldEvent.sendLevelData.
    public static void sendLevelData(ServerPlayer player) {
        EpicFightCapabilities.getUnparameterizedEntityPatch(player, ServerPlayerPatch.class).ifPresent(serverplayerpatch -> {
            PlayerSkills skillCapability = serverplayerpatch.getPlayerSkills();

            skillCapability.listSkillContainers().forEach(skillContainer -> {
                if (skillContainer.getSkill() != null) {
                    skillContainer.setSkill(skillContainer.getSkill(), true);
                }
            });

            SPDatapackSync skillParamsPacket = new SPDatapackSync(SPDatapackSync.PacketType.SKILL_PARAMS);
            SkillReloadListener.getSkillParams().forEach(skillParamsPacket::addTag);
            EpicFightNetworkManager.sendToPlayer(skillParamsPacket, player);
        });

        SPDatapackSync animationPacket = new SPDatapackSync(player.getServer().isResourcePackRequired() ? SPDatapackSync.PacketType.MANDATORY_RESOURCE_PACK_ANIMATION : SPDatapackSync.PacketType.RESOURCE_PACK_ANIMATION);
        SPDatapackSync armorPacket = new SPDatapackSync(SPDatapackSync.PacketType.ARMOR);
        SPDatapackSync weaponPacket = new SPDatapackSync(SPDatapackSync.PacketType.WEAPON);
        SPDatapackSync mobCapabilityPacket = new SPDatapackSync(SPDatapackSync.PacketType.MOB);
        SPDatapackSync itemPresetPacket = new SPDatapackSync(SPDatapackSync.PacketType.ITEM_PRESET);
        SPDatapackSync conditionalPacket = new SPDatapackSync(SPDatapackSync.PacketType.PROVIDER_CONDITIONAL);
        SPDatapackSync movesetPacket = new SPDatapackSync(SPDatapackSync.PacketType.MOVESET);
        SPDatapackSync weaponModifierPacket = new SPDatapackSync(SPDatapackSync.PacketType.MODIFIER);
        SPDatapackSync weaponTypePacket = new SPDatapackSync(SPDatapackSync.PacketType.WEAPON_TYPE);
        SPDatapackSync itemKeywordPacket = new SPDatapackSync(SPDatapackSync.PacketType.ITEM_KEYWORD);

        AnimationManager.getInstance().getResourcepackAnimationStream().forEach(animationPacket::addTag);
        ItemCapabilityReloadListener.getArmorDataStream().forEach(armorPacket::addTag);
        ItemCapabilityReloadListener.getWeaponDataStream().forEach(weaponPacket::addTag);
        MobPatchReloadListener.getDataStream().forEach(mobCapabilityPacket::addTag);
        WeaponTypeReloadListener.getWeaponTypeDataStream().forEach(weaponTypePacket::addTag);
        ItemKeywordReloadListener.getCompounds().forEach(itemKeywordPacket::addTag);

        EpicFightNetworkManager.PayloadBundleBuilder
            .beginWith(animationPacket)
            .and(itemPresetPacket)
            .and(weaponModifierPacket)
            .and(conditionalPacket)
            .and(movesetPacket)
            .and(weaponTypePacket)
            .and(armorPacket)
            .and(weaponPacket)
            .and(mobCapabilityPacket)
            .and(itemKeywordPacket)
            .send((first, others) -> EpicFightNetworkManager.sendToPlayer(first, player, others));
    }

    private VanillaWorldEventHooks() {}
}
