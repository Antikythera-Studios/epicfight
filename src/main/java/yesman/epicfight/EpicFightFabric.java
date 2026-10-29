package yesman.epicfight;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.impl.VanillaEntityEventHooks;
import yesman.epicfight.api.event.impl.VanillaWorldEventHooks;
import yesman.epicfight.api.ex_cap.listeners.ConditionalReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ItemPresetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.MovesetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.WeaponModifierReloadListener;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.compat.MinecraftMod;
import yesman.epicfight.compat.mcreator.MCreatorPlayerAnimationsCompat;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.config.CommonConfig;
import yesman.epicfight.config.ServerConfig;
import yesman.epicfight.data.loot.EpicFightLootTables;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.main.EpicFightExtensibleEnums;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.network.EpicFightPayloadRegistration;
import yesman.epicfight.network.EntityPairingPacketType;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.platform.fabric.FabricModPlatform;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.entries.EpicFightCreativeTabs;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.registry.entries.*;
import yesman.epicfight.server.commands.AnimatorCommand;
import yesman.epicfight.server.commands.PlayerModeCommand;
import yesman.epicfight.server.commands.PlayerSkillCommand;
import yesman.epicfight.server.commands.PlayerStaminaCommand;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.skill.SkillSlots;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;
import yesman.epicfight.world.capabilities.provider.CommonEntityPatchProvider;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.item.SkillBookItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.nio.file.Files;
import java.nio.file.Path;

public class EpicFightFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        EpicFight.initialize(new FabricModPlatform());

        EpicFightPayloadRegistration.registerCodecs();
        EpicFightPayloadRegistration.registerServerHandlers();

        // Load COMMON and SERVER configs
        java.nio.file.Path configDir = FabricLoader.getInstance().getConfigDir();
        yesman.epicfight.platform.neoforged.fml.config.ModConfig commonCfg = new yesman.epicfight.platform.neoforged.fml.config.ModConfig(
            yesman.epicfight.platform.neoforged.fml.config.ModConfig.Type.COMMON, CommonConfig.SPEC, configDir, EpicFight.MODID);
        CommonConfig.onLoad(commonCfg);
        yesman.epicfight.platform.neoforged.fml.config.ModConfig serverCfg = new yesman.epicfight.platform.neoforged.fml.config.ModConfig(
            yesman.epicfight.platform.neoforged.fml.config.ModConfig.Type.SERVER, ServerConfig.SPEC, configDir, EpicFight.MODID);
        ServerConfig.onLoad(serverCfg);

        // Register extensible enums
        LivingMotion.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, LivingMotions.class);
        SkillCategory.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, SkillCategories.class);
        SkillSlot.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, SkillSlots.class);
        CapabilityItem.Styles.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, CapabilityItem.Styles.class);
        CapabilityItem.WeaponCategories.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, CapabilityItem.WeaponCategories.class);
        Faction.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, Factions.class);
        EntityPairingPacketType.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, EntityPairingPacketTypes.class);

        LivingMotion.ENUM_MANAGER.loadEnum();
        SkillCategory.ENUM_MANAGER.loadEnum();
        SkillSlot.ENUM_MANAGER.loadEnum();
        CapabilityItem.Styles.ENUM_MANAGER.loadEnum();
        CapabilityItem.WeaponCategories.ENUM_MANAGER.loadEnum();
        Faction.ENUM_MANAGER.loadEnum();
        EntityPairingPacketType.ENUM_MANAGER.loadEnum();

        EpicFightExtensibleEnums.initExtensibleEnums();

        // Register animations before deferred registries — skills reference animation constants in their constructors
        AnimationManager.addNoWarningModId(EpicFight.EPICSKINS_MODID);
        AnimationManager.AnimationRegistryEvent animationRegistryEvent = new AnimationManager.AnimationRegistryEvent();
        Animations.registerAnimations(animationRegistryEvent);
        try {
            yesman.epicfight.epicskins.animation.EpicSkinsAnimations.registerAnimations(animationRegistryEvent);
        } catch (Throwable ignored) {}
        animationRegistryEvent.getBuilders().stream()
            .sorted(java.util.Comparator.comparing(AnimationManager.AnimationBuilder::namespace))
            .forEach(builder -> builder.task().accept(builder));

        // Register all deferred registries
        EpicFightBlocks.REGISTRY.accept();
        EpicFightItems.REGISTRY.accept();
        for (var dr : EpicFightRegistries.DEFERRED_REGISTRIES) {
            dr.accept();
        }

        EpicFightCommandArgumentTypes.registerArgumentTypes();
        EpicFightRegistries.registerDynamicCallbacks();
        EpicFightRegistries.bakeRegistries();
        EpicFightCapabilities.ENTITY_PATCH_PROVIDER.registerVanillaEntityPatches();

        // Wire EntityJoinLevelEvent equivalent via Fabric API
        // LivingEntityPatch.onJoinWorld sets the WEIGHT attribute; without it knockback divides by zero.
        ServerEntityEvents.ENTITY_LOAD.register((entity, serverLevel) -> {
            try {
                VanillaEntityEventHooks.onJoinLevel(entity, serverLevel, false);

                EpicFightCapabilities.getUnparameterizedEntityPatch(entity, EntityPatch.class).ifPresent(entitypatch -> {
                    try { entitypatch.onAddedToLevel(); } catch (Throwable ignored) {}
                });

                if (entity.getType() == net.minecraft.world.entity.EntityType.ENDERMAN) {
                    if (VanillaEntityEventHooks.onEnderManSapwns((net.minecraft.world.entity.monster.EnderMan) entity)) {
                        entity.discard();
                    }
                }
            } catch (Throwable e) {
                EpicFight.LOGGER.error("ServerEntityEvents.ENTITY_LOAD exception for entity {}", entity, e);
            }
        });

        // Register entity attributes
        yesman.epicfight.platform.fabric.event.EntityAttributeCreationEvent creationEvent = new yesman.epicfight.platform.fabric.event.EntityAttributeCreationEvent();
        EpicFightAttributes.EventBus.entityAttributeCreationEvent(creationEvent);
        yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent modificationEvent = new yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent();
        EpicFightAttributes.EventBus.entityAttributeModificationEvent(modificationEvent);

        // Register spawn placements via mixin accessor (SpawnPlacements.register is package-private)
        yesman.epicfight.platform.fabric.mixin.SpawnPlacementsAccessor.epicfight$register(
            EpicFightEntityTypes.WITHER_SKELETON_MINION.get(),
            net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            net.minecraft.world.entity.monster.Monster::checkAnyLightMonsterSpawnRules
        );

        EpicFightCapabilities.ITEM_CAPABILITY_PROVIDER.registerWeaponTypesByClass();
        Armatures.registerEntityTypes();
        WeaponTypeReloadListener.registerDefaultWeaponTypes();

        // Add skill books to creative tab
        ItemGroupEvents.modifyEntriesEvent(EpicFightCreativeTabs.ITEMS.getKey()).register(entries -> {
            EpicFightRegistries.SKILL.holders()
                .filter(skill -> skill.value().getCategory().learnable() && skill.value().getCreativeTab() == null)
                .forEach(holder -> {
                    ItemStack stack = new ItemStack(EpicFightItems.SKILLBOOK.get());
                    SkillBookItem.setContainingSkill(holder, stack);
                    entries.accept(stack);
                });
        });

        EpicFightGameRules.registerGameRules();
        EpicFightPotions.addRecipes();
        EpicFightMobEffects.addOffhandModifier();

        EpicFightEventHooks.Registry.SKILLBOOK_LOOT_TABLE.registerEvent(event -> {
            EpicFightLootTables.createSkillLootTable(event);
        });
        EpicFightLootTables.registerLootTableEvents();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PlayerModeCommand.register(dispatcher);
            PlayerSkillCommand.register(dispatcher);
            PlayerStaminaCommand.register(dispatcher);
            AnimatorCommand.register(dispatcher);
        });

        yesman.epicfight.network.EpicFightReloadListeners.register();

        // Wire datapack sync — per-player on join, all players on /reload
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            try {
                VanillaWorldEventHooks.onDatapackSync(handler.getPlayer());
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("Failed to sync datapack data to player: {}", handler.getPlayer().getName().getString(), e);
            }
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResources, success) -> {
            if (success) {
                try {
                    VanillaWorldEventHooks.onDatapackSyncAll(server);
                } catch (Throwable e) {
                    EpicFight.LOGGER.warn("Failed to sync datapack data to all players", e);
                }
            }
        });

        // Load compat modules
        boolean isClientSide = EpicFightSharedConstants.isPhysicalClient();
        for (MinecraftMod mod : MinecraftMod.values()) {
            if (FabricLoader.getInstance().isModLoaded(mod.getModId())) {
                if (isClientSide || !mod.isClientOnly()) {
                    ICompatModule.loadCompatModule(mod.getCompatibilityModule());
                }
            }
        }

        // MCreator compat — loaded if any mod provides bedrock_animations data
        if (isClientSide) {
            try {
                boolean hasBedrockAnimations = FabricLoader.getInstance().getAllMods().stream().anyMatch(modContainer -> {
                    try {
                        return modContainer.getOrigin().getPaths().stream().anyMatch(path -> {
                            try {
                                Path dataPath = path.resolve("data");
                                if (!Files.exists(dataPath)) return false;
                                try (var stream = Files.list(dataPath)) {
                                    return stream.anyMatch(namespace -> Files.exists(namespace.resolve("bedrock_animations")));
                                }
                            } catch (Exception e) {
                                return false;
                            }
                        });
                    } catch (Exception e) {
                        return false;
                    }
                });
                if (hasBedrockAnimations) {
                    ICompatModule.loadCompatModule(MCreatorPlayerAnimationsCompat.class);
                }
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("Failed to check MCreator compat", e);
            }
        }
    }
}
