package yesman.epicfight;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientWorldEvents;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.Minecraft;
import yesman.epicfight.api.client.input.action.EpicFightInputAction;
import yesman.epicfight.api.client.input.action.InputAction;
import yesman.epicfight.api.client.input.action.MinecraftInputAction;
import yesman.epicfight.client.events.engine.IEventBasedEngine;
import yesman.epicfight.client.gui.screen.SkillBookScreen;
import yesman.epicfight.client.gui.widgets.AnchoredButton;
import yesman.epicfight.client.gui.widgets.ColorDeterminator;
import yesman.epicfight.client.gui.widgets.common.WidgetTheme;
import yesman.epicfight.client.input.EpicFightKeyMappings;
import yesman.epicfight.client.model.SeparateTransformsModelLoadingPlugin;
import yesman.epicfight.client.model.SkillBookModelLoadingPlugin;
import yesman.epicfight.client.particle.AirBurstParticle;
import yesman.epicfight.client.particle.AnimationTrailParticle;
import yesman.epicfight.client.particle.AshDirectionalParticle;
import yesman.epicfight.client.particle.BloodParticle;
import yesman.epicfight.client.particle.BladeRushParticle;
import yesman.epicfight.client.particle.CatharsisParticle;
import yesman.epicfight.client.particle.CutParticle;
import yesman.epicfight.client.particle.DustParticle;
import yesman.epicfight.client.particle.EnderParticle;
import yesman.epicfight.client.particle.EntityAfterimageParticle;
import yesman.epicfight.client.particle.EviscerateParticle;
import yesman.epicfight.client.particle.FeatherParticle;
import yesman.epicfight.client.particle.ForceFieldEndParticle;
import yesman.epicfight.client.particle.ForceFieldParticle;
import yesman.epicfight.client.particle.GroundSlamParticle;
import yesman.epicfight.client.particle.HitBluntParticle;
import yesman.epicfight.client.particle.HitCutParticle;
import yesman.epicfight.client.particle.LaserParticle;
import yesman.epicfight.client.particle.ProjectileTrailParticle;
import yesman.epicfight.client.particle.TsunamiSplashParticle;
import yesman.epicfight.client.renderer.EpicFightShaders;
import yesman.epicfight.client.renderer.blockentity.FractureBlockRenderer;
import yesman.epicfight.client.renderer.entity.DroppedNetherStarRenderer;
import yesman.epicfight.client.renderer.entity.WitherGhostRenderer;
import yesman.epicfight.client.renderer.entity.WitherSkeletonMinionRenderer;
import yesman.epicfight.client.renderer.patched.item.EpicFightItemProperties;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.client.renderer.shader.compute.loader.ComputeShaderProvider;
import yesman.epicfight.network.EpicFightClientPayloadRegistration;
import yesman.epicfight.network.EpicFightReloadListeners;
import yesman.epicfight.platform.fabric.client.FabricClientModPlatform;
import yesman.epicfight.registry.entries.EpicFightBlockEntities;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.world.capabilities.provider.CommonEntityPatchProvider;

public class EpicFightFabricClient implements ClientModInitializer {

    private static boolean computeShaderChecked = false;

    @Override
    public void onInitializeClient() {
        EpicFightClient.initialize(new FabricClientModPlatform());

        // Register core shaders — fires during resource reload on the render thread
        CoreShaderRegistrationCallback.EVENT.register(context -> {
            context.register(
                    EpicFight.identifier("solid_model"),
                    DefaultVertexFormat.POSITION_COLOR_NORMAL,
                    shader -> EpicFightShaders.positionColorNormalShader = shader);

            // Register compute shaders if hardware supports them
            try {
                ComputeShaderProvider.checkIfSupports();
                if (ComputeShaderProvider.supportComputeShader()) {
                    yesman.epicfight.platform.neoforged.client.event.RegisterShadersEvent evt =
                            new yesman.epicfight.platform.neoforged.client.event.RegisterShadersEvent(
                                    Minecraft.getInstance().getResourceManager());
                    ComputeShaderProvider.epicfight$registerComputeShaders(evt);
                }
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("Compute shader registration failed", e);
            }
        });

        // Register entity renderers
        EntityRendererRegistry.register(EpicFightEntityTypes.AREA_EFFECT_BREATH.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(EpicFightEntityTypes.DROPPED_NETHER_STAR.get(), DroppedNetherStarRenderer::new);
        EntityRendererRegistry.register(EpicFightEntityTypes.DEATH_HARVEST_ORB.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(EpicFightEntityTypes.DODGE_LOCATION_INDICATOR.get(), net.minecraft.client.renderer.entity.NoopRenderer::new);
        EntityRendererRegistry.register(EpicFightEntityTypes.WITHER_GHOST_CLONE.get(), WitherGhostRenderer::new);
        EntityRendererRegistry.register(EpicFightEntityTypes.WITHER_SKELETON_MINION.get(), WitherSkeletonMinionRenderer::new);
        BlockEntityRendererRegistry.register(EpicFightBlockEntities.FRACTURE.get(), FractureBlockRenderer::new);

        // Register particle providers
        ParticleFactoryRegistry registry = ParticleFactoryRegistry.getInstance();
        registry.register(EpicFightParticles.ENDERMAN_DEATH_EMIT.get(), EnderParticle.EndermanDeathEmitProvider::new);
        registry.register(EpicFightParticles.HIT_BLUNT.get(), HitBluntParticle.Provider::new);
        registry.register(EpicFightParticles.CUT.get(), CutParticle.Provider::new);
        registry.register(EpicFightParticles.NORMAL_DUST.get(), DustParticle.NormalDustProvider::new);
        registry.register(EpicFightParticles.DUST_EXPANSIVE.get(), DustParticle.ExpansiveDustProvider::new);
        registry.register(EpicFightParticles.DUST_CONTRACTIVE.get(), DustParticle.ContractiveDustProvider::new);
        registry.register(EpicFightParticles.BLOOD.get(), BloodParticle.Provider::new);
        registry.register(EpicFightParticles.BLADE_RUSH_SKILL.get(), BladeRushParticle.Provider::new);
        registry.register(EpicFightParticles.BREATH_FLAME.get(), EnderParticle.BreathFlameProvider::new);
        registry.register(EpicFightParticles.TSUNAMI_SPLASH.get(), TsunamiSplashParticle.Provider::new);
        registry.register(EpicFightParticles.FEATHER.get(), FeatherParticle.Provider::new);
        registry.register(EpicFightParticles.ASH_DIRECTIONAL.get(), AshDirectionalParticle.Provider::new);
        registry.register(EpicFightParticles.CATHARSIS.get(), CatharsisParticle.Provider::new);
        registry.register(EpicFightParticles.HIT_BLADE.get(), new HitCutParticle.Provider());
        registry.register(EpicFightParticles.EVISCERATE.get(), new EviscerateParticle.Provider());
        registry.register(EpicFightParticles.GROUND_SLAM.get(), new GroundSlamParticle.Provider());
        registry.register(EpicFightParticles.GROUND_FRACTURE.get(), new GroundSlamParticle.BlockParticleProvider());
        registry.register(EpicFightParticles.FORCE_FIELD.get(), new ForceFieldParticle.Provider());
        registry.register(EpicFightParticles.FORCE_FIELD_END.get(), new ForceFieldEndParticle.Provider());
        registry.register(EpicFightParticles.ADRENALINE_PLAYER_BEATING.get(), new EntityAfterimageParticle.AdrenalineParticleProvider());
        registry.register(EpicFightParticles.WHITE_AFTERIMAGE.get(), new EntityAfterimageParticle.WhiteAfterimageProvider());
        registry.register(EpicFightParticles.LASER.get(), new LaserParticle.Provider());
        registry.register(EpicFightParticles.NEUTRALIZE.get(), new DustParticle.ExpansiveMetaParticle.Provider());
        registry.register(EpicFightParticles.BOSS_CASTING.get(), new DustParticle.ContractiveMetaParticle.Provider());
        registry.register(EpicFightParticles.SWING_TRAIL.get(), new AnimationTrailParticle.Provider());
        registry.register(EpicFightParticles.PROJECTILE_TRAIL.get(), new ProjectileTrailParticle.Provider());
        registry.register(EpicFightParticles.AIR_BURST.get(), new AirBurstParticle.Provider());

        IEventBasedEngine.init(null, null);
        EpicFightReloadListeners.registerClient();
        ModelLoadingPlugin.register(new SeparateTransformsModelLoadingPlugin());
        ModelLoadingPlugin.register(new SkillBookModelLoadingPlugin());

        // Register built-in resource pack
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.registerBuiltinResourcePack(
            EpicFight.identifier("epicfight_legacy"),
            FabricLoader.getInstance().getModContainer(EpicFight.MODID).orElseThrow(),
            net.fabricmc.fabric.api.resource.ResourcePackActivationType.NORMAL
        );

        // Load CLIENT config
        java.nio.file.Path configDir = FabricLoader.getInstance().getConfigDir();
        yesman.epicfight.platform.neoforged.fml.config.ModConfig clientCfg = new yesman.epicfight.platform.neoforged.fml.config.ModConfig(
            yesman.epicfight.platform.neoforged.fml.config.ModConfig.Type.CLIENT, yesman.epicfight.config.ClientConfig.SPEC, configDir, EpicFight.MODID);
        yesman.epicfight.config.ClientConfig.epicfight$modConfigLoading(clientCfg);

        EpicFightClientPayloadRegistration.registerClientHandlers();

        // Register client-side extensible enums
        InputAction.ENUM_MANAGER.registerEnumCls(EpicFight.MODID, EpicFightInputAction.class);
        InputAction.ENUM_MANAGER.registerEnumCls("minecraft", MinecraftInputAction.class);
        InputAction.ENUM_MANAGER.loadEnum();
        WidgetTheme.ENUM_MANAGER.registerEnumCls(EpicFight.MODID + ":color_determinator_theme", ColorDeterminator.Theme.class);
        WidgetTheme.ENUM_MANAGER.registerEnumCls(EpicFight.MODID + ":anchored_button_built_in_theme", AnchoredButton.BuiltInTheme.class);
        WidgetTheme.ENUM_MANAGER.loadEnum();

        EpicFightKeyMappings.registerKeys();
        CommonEntityPatchProvider.ClientModule.registerClientPlayerPatches();
        SkillBookScreen.registerIconItems();
        EpicFightItemProperties.registerItemProperties();

        // Defer compute shader check and item renderer init to first client tick (OpenGL context not ready during init)
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!computeShaderChecked) {
                computeShaderChecked = true;
                try {
                    RenderItemBase.initItemRenderers(client);
                    ComputeShaderProvider.checkIfSupports();
                    if (yesman.epicfight.config.ClientConfig.combatCategorizedItems.isEmpty() && yesman.epicfight.config.ClientConfig.miningCategorizedItems.isEmpty()) {
                        yesman.epicfight.client.gui.screen.config.ItemsPreferenceScreen.resetItems();
                    }
                    if (ComputeShaderProvider.supportComputeShader() && ComputeShaderProvider.meshComputeVanilla == null) {
                        yesman.epicfight.platform.neoforged.client.event.RegisterShadersEvent evt =
                                new yesman.epicfight.platform.neoforged.client.event.RegisterShadersEvent(
                                        Minecraft.getInstance().getResourceManager());
                        ComputeShaderProvider.epicfight$registerComputeShaders(evt);
                    }
                } catch (Throwable e) {
                    EpicFight.LOGGER.warn("First-tick client setup failed", e);
                }
            }
        });

        // Wire client level load/unload for FakeLevel
        ClientWorldEvents.AFTER_CLIENT_WORLD_CHANGE.register((client, newLevel) -> {
            try {
                if (newLevel instanceof yesman.epicfight.client.world.util.FakeLevel) return;
                if (newLevel instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
                    yesman.epicfight.client.world.util.FakeLevel.getFakeLevel(clientLevel);
                }
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("FakeLevel load failed", e);
            }
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            try {
                yesman.epicfight.client.world.util.FakeLevel.unloadFakeLevel();
            } catch (Throwable e) {
                EpicFight.LOGGER.warn("FakeLevel unload failed", e);
            }
        });

        // Wire screen event hooks
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            ScreenMouseEvents.allowMouseClick(screen).register((s, mouseX, mouseY, button) ->
                !yesman.epicfight.api.client.event.impl.VanillaGUIEventHooks.onMouseButtonPressedInScreen(screen));
            ScreenMouseEvents.allowMouseRelease(screen).register((s, mouseX, mouseY, button) ->
                !yesman.epicfight.api.client.event.impl.VanillaGUIEventHooks.onMouseButtonReleasedInScreen(screen));
            ScreenKeyboardEvents.allowKeyPress(screen).register((s, key, scancode, modifiers) ->
                !yesman.epicfight.api.client.event.impl.VanillaGUIEventHooks.onKeyboardPressedInScreen(screen, key));
        });
    }
}
