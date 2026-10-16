package yesman.epicfight.main;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLConstructModEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddPackFindersEvent;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.sweenus.simplytooltips.api.TooltipProviderRegistry;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.EpicFight;
import yesman.epicfight.EpicFightClient;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationRegistryEvent;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.client.animation.property.JointMaskReloadListener;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.api.client.model.ItemSkinsReloadListener;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.ex_cap.listeners.ConditionalReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ItemPresetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.MovesetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.WeaponModifierReloadListener;
import yesman.epicfight.client.events.engine.IEventBasedEngine;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.screen.config.EpicFightSettingScreen;
import yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen;
import yesman.epicfight.client.gui.widgets.AnchoredButton;
import yesman.epicfight.client.gui.widgets.ColorDeterminator;
import yesman.epicfight.client.gui.widgets.common.WidgetTheme;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.online.cosmetics.Emote;
import yesman.epicfight.client.renderer.patched.item.EpicFightItemProperties;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.compat.ICompatModule;
import yesman.epicfight.compat.MinecraftMod;
import yesman.epicfight.compat.mcreator.MCreatorPlayerAnimationsCompat;
import yesman.epicfight.compat.simplytooltips.EpicFightTooltipProvider;
import yesman.epicfight.config.ClientConfig;
import yesman.epicfight.config.CommonConfig;
import yesman.epicfight.config.ServerConfig;
import yesman.epicfight.data.loot.EpicFightLootTables;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.generated.LangKeys;
import yesman.epicfight.network.EntityPairingPacketType;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.platform.ModPlatformProvider;
import yesman.epicfight.platform.neoforge.NeoForgeModPlatform;
import yesman.epicfight.platform.neoforge.client.NeoForgeClientModPlatform;
import yesman.epicfight.registry.EpicFightRegistries;
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
import yesman.epicfight.world.capabilities.entitypatch.Faction;
import yesman.epicfight.world.capabilities.entitypatch.Factions;
import yesman.epicfight.world.capabilities.item.*;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.provider.CommonEntityPatchProvider;
import yesman.epicfight.world.gamerule.EpicFightGameRules;
import yesman.epicfight.world.item.SkillBookItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 *  --- Future list ---
 *  Update language files (always)
 *  AddEntity an alert function when an entity targeting the player tries grappling or execution attack
 *  AddEntity UI for execution resistance
 *  AddEntity functionality to blooming effect (resists wither effect)
 *  AddEntity a screen for setting animation properties in datapack editor
 *  Enhance the stun system (maybe remove or barely leave knockback)
 *
 *  @author yesman
 */
@Mod(EpicFightMod.MODID)
public class EpicFightMod {

	// TODO: Rename class to EpicFightNeoForge
	// TODO: Avoid using the deprecated fields in neoforge Gradle project, and migrate to the new shared ones in EpicFight via IDE structural replacement

	/// @deprecated Use [yesman.epicfight.EpicFight#MODID] instead
	@Deprecated(forRemoval = true)
	public static final String MODID = EpicFight.MODID;

	/// @deprecated Use [yesman.epicfight.EpicFight#EPICSKINS_MODID] instead
	@Deprecated(forRemoval = true)
	public static final String EPICSKINS_MODID = EpicFight.EPICSKINS_MODID;

	/// @deprecated Use [yesman.epicfight.EpicFight#LOGGER] instead
	@Deprecated(forRemoval = true, since = "26.1")
	public static final Logger LOGGER = EpicFight.LOGGER;

	public static String prefix(String s) {
		return String.format("%s:%s", MODID, s);
	}

    /// @deprecated Consider using the generated object [LangKeys],
    /// which is type-safe and not error-prone to runtime bugs or crashes.
    @Deprecated(forRemoval = true)
	public static String format(String s) {
		return String.format(s, MODID);
	}



	public static void logAndStacktraceIfDevSide(BiConsumer<Logger, String> logFunction, String message, Function<String, Throwable> exceptionProvider) {
		logAndStacktraceIfDevSide(logFunction, message, exceptionProvider, message);
	}

	public static void logAndStacktraceIfDevSide(BiConsumer<Logger, String> logFunction, String message, Function<String, Throwable> exceptionProvider, String stackTraceMessage) {
		logFunction.accept(LOGGER, message);
		stacktraceIfDevSide(message, exceptionProvider, stackTraceMessage);
	}

	public static void stacktraceIfDevSide(String message, Function<String, Throwable> exceptionProvider) {
		stacktraceIfDevSide(message, exceptionProvider, message);
	}

	public static void stacktraceIfDevSide(String message, Function<String, Throwable> exceptionProvider, String stackTraceMessage) {
		if (exceptionProvider != null && EpicFightSharedConstants.IS_DEV_ENV) {
			exceptionProvider.apply(stackTraceMessage).printStackTrace();
		}
	}

    public EpicFightMod(IEventBus modEventBus, ModContainer modContainer) {
		ModPlatformProvider.initialize(new NeoForgeModPlatform());
		TooltipProviderRegistry.register(new EpicFightTooltipProvider(), 2);
    	if (EpicFightSharedConstants.isPhysicalClient()) {
			EpicFightClient.initialize(new NeoForgeClientModPlatform(modEventBus));
			// TODO: (MULTI_LOADER) EpicFightKeyMappings must be in common and not neoforge,
			//  and is temporarily kept here since CombatKeyMapping depends on ClientEngine,
			//  which is not in common yet. https://github.com/Epic-Fight/epicfight/pull/2365
			//  When ClientEngine#isEpicFightMode is migrated to common:
			//  1. Move EpicFightKeyMappings and CombatKeyMapping to common (same package: yesman.epicfight.client.input)
			//  2. Remove "EpicFightKeyMappings.registerKeys()" statement from here
			//  3. Add it at the very end of "EpicFightClient.initialize()" method
			EpicFightKeyMappings.registerKeys();

    		modContainer.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
    		modContainer.registerExtensionPoint(IConfigScreenFactory.class, EpicFightSettingScreen::new);
    		IEventBasedEngine.init(NeoForge.EVENT_BUS, modEventBus);
    	} else {
    		modContainer.registerConfig(ModConfig.Type.SERVER, ServerConfig.SPEC);
    	}

    	modContainer.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
    	modContainer.registerExtensionPoint(EpicFightExtensions.class, (Supplier<EpicFightExtensions>)() -> new EpicFightExtensions(EpicFightCreativeTabs.ITEMS));

		modEventBus.addListener(this::constructMod);
		modEventBus.addListener(this::doCommonStuff);
		modEventBus.addListener(this::addPackFindersEvent);
		modEventBus.addListener(this::buildCreativeTabWithSkillBooks);
        modEventBus.addListener(this::addDatapackRegistryEvent);
		modEventBus.addListener(this::registerCapabilities);

    	NeoForge.EVENT_BUS.addListener(this::command);
        NeoForge.EVENT_BUS.addListener(this::addReloadListnerEvent);

    	LivingMotion.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, LivingMotions.class);
    	SkillCategory.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, SkillCategories.class);
    	SkillSlot.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, SkillSlots.class);
    	Style.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, Styles.class);
    	WeaponCategory.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, WeaponCategories.class);
    	Faction.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, Factions.class);
    	EntityPairingPacketType.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, EntityPairingPacketTypes.class);

    	if (EpicFightSharedConstants.isPhysicalClient()) {
            InputAction.ENUM_MANAGER.registerEnumCls(EpicFightMod.MODID, EpicFightInputAction.class);
            InputAction.ENUM_MANAGER.registerEnumCls("minecraft", MinecraftInputAction.class);
            WidgetTheme.ENUM_MANAGER.registerEnumCls(EpicFightMod.prefix("color_determinator_theme"), ColorDeterminator.Theme.class);
            WidgetTheme.ENUM_MANAGER.registerEnumCls(EpicFightMod.prefix("anchored_button_built_in_theme"), AnchoredButton.BuiltInTheme.class);
        }
    	EpicFightRegistries.DEFERRED_REGISTRIES.forEach(deferredRegistry -> deferredRegistry.register(modEventBus));

        if (EpicFightSharedConstants.isPhysicalClient()) {
            modEventBus.addListener(ComputeShaderProvider::epicfight$registerComputeShaders);
        }
        loadModCompatibilityModules(modEventBus);

	}

    private List<? extends Class<? extends ICompatModule>> getCompatibilityModules(final boolean isClientSide) {
        return Arrays.stream(MinecraftMod.values())
                .filter(mod -> ModPlatformProvider.get().isModLoaded(mod.getModId()))
                // Includes all mods on client. Skips client-only mods on server
                .filter(mod -> isClientSide || !mod.isClientOnly())
                .map(MinecraftMod::getCompatibilityModule)
                .toList();
    }

    private void loadModCompatibilityModules(@NotNull IEventBus modEventBus) {
        final boolean isClientSide = EpicFightSharedConstants.isPhysicalClient();

        for (final Class<? extends ICompatModule> module : getCompatibilityModules(isClientSide)) {
            ICompatModule.loadCompatModule(modEventBus, module);
        }

        if (isClientSide) {
            if (ModList.get().getModFiles().stream().anyMatch(modFile -> {
                try {
                    Path dataPath = modFile.getFile().findResource("data");
                    return Files.exists(dataPath) && Files.list(dataPath).anyMatch(namespace -> Files.exists(namespace.resolve("bedrock_animations")));
                } catch (Exception e) {
                    return false;
                }
            })) {
                ICompatModule.loadCompatModule(modEventBus, MCreatorPlayerAnimationsCompat.class);
            }
        }
    }

    /**
     * FML Lifecycle Events
     */
    private void constructMod(final FMLConstructModEvent event) {
    	event.enqueueWork(LivingMotion.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(SkillCategory.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(SkillSlot.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(Style.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(WeaponCategory.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(Faction.ENUM_MANAGER::loadEnum);
    	event.enqueueWork(EntityPairingPacketType.ENUM_MANAGER::loadEnum);
        event.enqueueWork(WidgetTheme.ENUM_MANAGER::loadEnum);

    	if (EpicFightSharedConstants.isPhysicalClient()) {
            event.enqueueWork(InputAction.ENUM_MANAGER::loadEnum);
        }
    	event.enqueueWork(() -> {
    		AnimationManager.addNoWarningModId(EPICSKINS_MODID);
			AnimationRegistryEvent animationregistryevent = new AnimationRegistryEvent();
    		ModLoader.postEvent(animationregistryevent);
    		animationregistryevent.getBuilders().stream().sorted(Comparator.comparing(AnimationManager.AnimationBuilder::namespace)).forEach(builder -> builder.task().accept(builder));
    	});
    }

	private void doCommonStuff(final FMLCommonSetupEvent event) {
		event.enqueueWork(Armatures::registerEntityTypes);
		event.enqueueWork(EpicFightCommandArgumentTypes::registerArgumentTypes);
		event.enqueueWork(EpicFightPotions::addRecipes);
		event.enqueueWork(EpicFightCapabilities.ITEM_CAPABILITY_PROVIDER::registerWeaponTypesByClass);
		event.enqueueWork(EpicFightCapabilities.ENTITY_PATCH_PROVIDER::registerVanillaEntityPatches);
		event.enqueueWork(EpicFightGameRules::registerGameRules);
		event.enqueueWork(WeaponTypeReloadListener::registerDefaultWeaponTypes);
		event.enqueueWork(EpicFightMobEffects::addOffhandModifier);
		event.enqueueWork(EpicFightExtensibleEnums::initExtensibleEnums);
		event.enqueueWork(this::addRegistries);
	}

    private void addRegistries()
    {
        EpicFightEventHooks.Registry.SKILLBOOK_LOOT_TABLE.registerEvent(EpicFightLootTables::createSkillLootTable);
    }

	/**
	 * Register Commands
	 */
	private void command(final RegisterCommandsEvent event) {
		PlayerModeCommand.register(event.getDispatcher());
		PlayerSkillCommand.register(event.getDispatcher());
		PlayerStaminaCommand.register(event.getDispatcher());
		AnimatorCommand.register(event.getDispatcher());
    }

	public void addPackFindersEvent(AddPackFindersEvent event) {
		if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            Path resourcePath = ModList.get().getModFileById(EpicFightMod.MODID).getFile().findResource("packs/epicfight_legacy");

            PackLocationInfo packLocation = new PackLocationInfo("epicfight_legacy", Component.translatable("pack.epicfight_legacy.title"), PackSource.BUILT_IN, Optional.empty());
            Pack.ResourcesSupplier resourcesSupplier = new PathPackResources.PathResourcesSupplier(resourcePath);

            Pack pack = Pack.readMetaAndCreate(packLocation, resourcesSupplier, PackType.CLIENT_RESOURCES, new PackSelectionConfig(false, Pack.Position.TOP, false));

            if (pack != null) {
                event.addRepositorySource(source -> source.accept(pack));
            }
        }
    }

	private void addReloadListnerEvent(final AddReloadListenerEvent event) {
		event.addListener(new ColliderPreset());
		event.addListener(new SkillReloadListener());
		//ExCap ------------------------------------
		event.addListener(new ItemPresetReloadListener());
		event.addListener(new WeaponModifierReloadListener());
		event.addListener(new ConditionalReloadListener());
        event.addListener(new MovesetReloadListener());
		// -----------------------------------------
		event.addListener(new WeaponTypeReloadListener());
		event.addListener(new ItemKeywordReloadListener());
		event.addListener(new ItemCapabilityReloadListener());
		event.addListener(new MobPatchReloadListener());
	}

    private void addDatapackRegistryEvent(DataPackRegistryEvent.NewRegistry event) {
        event.dataPackRegistry(EpicFightRegistries.Keys.EMOTE, Emote.CODEC, Emote.CODEC);
    }

    public void registerCapabilities(RegisterCapabilitiesEvent event) {
        BuiltInRegistries.ITEM.forEach(item -> {
            event.registerItem(EpicFightCapabilities.CAPABILITY_ITEM, EpicFightCapabilities.ITEM_CAPABILITY_PROVIDER, item);
        });
    }

	@EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        	event.enqueueWork(ComputeShaderProvider::checkIfSupports);
    		event.enqueueWork(CommonEntityPatchProvider.ClientModule::registerClientPlayerPatches);
    		event.enqueueWork(SkillBookScreen::registerIconItems);
    		event.enqueueWork(EpicFightItemProperties::registerItemProperties);
    		event.enqueueWork(() -> {
    			if (ClientConfig.combatCategorizedItems.isEmpty() && ClientConfig.miningCategorizedItems.isEmpty()) {
    				ItemsPreferenceScreen.resetItems();
    			}
    		});
        }

        @SubscribeEvent
        public static void registerResourcepackReloadListnerEvent(final RegisterClientReloadListenersEvent event) {
    		event.registerReloadListener(new JointMaskReloadListener());
    		event.registerReloadListener(Meshes.INSTANCE);
    		event.registerReloadListener(AnimationManager.getInstance());
    		event.registerReloadListener(ItemSkinsReloadListener.INSTANCE);
    	}
    }

	@EventBusSubscriber(modid = EpicFightMod.MODID, value = Dist.DEDICATED_SERVER)
    public static class ServerForgeEvents {
		@SubscribeEvent(priority = EventPriority.HIGHEST)
		public static void addReloadListnerEvent(final AddReloadListenerEvent event) {
			event.addListener(AnimationManager.getInstance());
		}
    }

	private void buildCreativeTabWithSkillBooks(final BuildCreativeModeTabContentsEvent event) {
        //Accept learnable skills for each mod by [EpicFightExtensions#skillBookCreativeTab].
		// If the extension doesn't exist, add them to [EpicFightCreativeTabs.ITEMS] tab.
		EpicFightRegistries.SKILL.keySet().stream().map(id -> id.getNamespace()).distinct().forEach((modid) -> {
			ModList.get().getModContainerById(modid).flatMap(modcontainer -> modcontainer.getCustomExtension(EpicFightExtensions.class)).ifPresentOrElse(extension -> {
				if (extension.skillBookCreativeTab().get() == event.getTab()) {
					EpicFightRegistries.SKILL.holders()
						.filter(skill ->
							skill.value().getCategory().learnable() &&
							skill.value().getCreativeTab() == null &&
							skill.value().getRegistryName().getNamespace() == modid
						).forEach(holder -> {
							ItemStack stack = new ItemStack(EpicFightItems.SKILLBOOK.get());
							SkillBookItem.setContainingSkill(holder, stack);
							event.accept(stack);
						});
				}
			}, () -> {
				if (event.getTab() == EpicFightCreativeTabs.ITEMS.get()) {
					EpicFightRegistries.SKILL.holders()
						.filter(skill ->
							skill.value().getCategory().learnable() &&
							skill.value().getCreativeTab() == null &&
							skill.value().getRegistryName().getNamespace() == modid
						).forEach(holder -> {
							ItemStack stack = new ItemStack(EpicFightItems.SKILLBOOK.get());
							SkillBookItem.setContainingSkill(holder, stack);
							event.accept(stack);
						});
				}
			});
		});

		EpicFightRegistries.SKILL.holders()
			.filter(skill ->
				skill.value().getCategory().learnable() &&
				skill.value().getCreativeTab() == event.getTab()
			).forEach(holder -> {
				ItemStack stack = new ItemStack(EpicFightItems.SKILLBOOK.get());
				SkillBookItem.setContainingSkill(holder, stack);
				event.accept(stack);
			});
	}

	/// @deprecated Use [yesman.epicfight.EpicFight#identifier(String)] instead
	@Deprecated(forRemoval = true)
	public static @NotNull ResourceLocation identifier(@NotNull String path) {
        return EpicFight.identifier(path);
	}

	/// @deprecated Use [#identifier(String)] instead. [Mojang renamed `ResourceLocation` to `Identifier` in 1.21.11](https://neoforged.net/news/21.11release/#renaming-of-resourcelocation-to-identifier).
	@Deprecated(forRemoval = true)
	public static @NotNull ResourceLocation rl(@NotNull String path) {
		return identifier(path);
	}
}