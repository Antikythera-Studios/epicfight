package yesman.epicfight.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.server.packs.resources.ResourceManager;

import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import yesman.epicfight.EpicFight;
import yesman.epicfight.gameasset.ColliderPreset;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.client.animation.property.JointMaskReloadListener;
import yesman.epicfight.api.client.model.ItemSkinsReloadListener;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.data.reloader.ItemCapabilityReloadListener;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.data.reloader.SkillReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ConditionalReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ItemPresetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.MovesetReloadListener;
import yesman.epicfight.api.ex_cap.listeners.WeaponModifierReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ExCapDataCreationReloadListener;
import yesman.epicfight.api.ex_cap.listeners.ExCapDataReloadListener;
import yesman.epicfight.world.capabilities.item.ItemKeywordReloadListener;
import yesman.epicfight.world.capabilities.item.WeaponTypeReloadListener;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Registers all EpicFight server-side reload listeners with Fabric's {@link ResourceManagerHelper}.
 *
 * <p>Fabric requires reload listeners to implement {@link IdentifiableResourceReloadListener},
 * which adds a {@code getFabricId()} method. This class wraps the existing
 * {@link SimpleJsonResourceReloadListener} instances in adapter wrappers.</p>
 */
public final class EpicFightReloadListeners {

    private EpicFightReloadListeners() {}

    /**
     * Registers all server-side reload listeners with Fabric's resource manager.
     * Must be called from {@code onInitialize} (common side).
     */
    public static void register() {
        ResourceManagerHelper helper = ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA);

        // Collider preset — registered first to match NeoForge upstream ordering
        helper.registerReloadListener(wrap("collider_preset", new ColliderPreset()));

        // ExCap reload listeners — MUST run before core listeners that depend on weapon types
        // Order: item_presets (populates BUILDERS) → weapon_types (exports BUILDERS to PRESETS) → capabilities (uses PRESETS)
        helper.registerReloadListener(wrap("item_presets", new ItemPresetReloadListener()));
        helper.registerReloadListener(wrap("weapon_modifiers", new WeaponModifierReloadListener()));
        helper.registerReloadListener(wrap("movesets", new MovesetReloadListener()));
        helper.registerReloadListener(wrap("conditional", new ConditionalReloadListener()));
        helper.registerReloadListener(wrap("excap_data_creation", new ExCapDataCreationReloadListener()));
        helper.registerReloadListener(wrap("excap_data", new ExCapDataReloadListener()));

        // Core datapack reload listeners — run after ExCap so weapon types are available
        helper.registerReloadListener(wrap("skill_parameters", SkillReloadListener.getInstance()));
        helper.registerReloadListener(wrap("epicfight_mobpatch", new MobPatchReloadListener()));
        ResourceLocation weaponTypesId = ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "weapon_types");
        ResourceLocation animationManagerId = ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "animation_manager");
        helper.registerReloadListener(wrap("animation_manager", AnimationManager.getInstance()));
        helper.registerReloadListener(wrap("weapon_types", new WeaponTypeReloadListener(),
                ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, "item_presets"),
                animationManagerId));
        helper.registerReloadListener(wrap("capabilities", new ItemCapabilityReloadListener(), weaponTypesId, animationManagerId));
        helper.registerReloadListener(wrap("item_keywords", new ItemKeywordReloadListener(), weaponTypesId));
        // Emote is now loaded as a data pack registry via RegistryDataLoader (MixinRegistryDataLoader),
        // matching the NeoForge DataPackRegistryEvent approach. No manual reload listener needed.

        EpicFight.LOGGER.info("EpicFight reload listeners registered (13)");
    }

    /**
     * Registers all client-side reload listeners with Fabric's resource manager.
     * Must be called from {@code onInitializeClient} (client side).
     *
     * <p>These correspond to the NeoForge {@code RegisterClientReloadListenersEvent}
     * registrations in {@code EpicFightMod.registerResourcepackReloadListnerEvent()}.</p>
     */
    public static void registerClient() {
        ResourceManagerHelper helper = ResourceManagerHelper.get(net.minecraft.server.packs.PackType.CLIENT_RESOURCES);

        helper.registerReloadListener(wrap("joint_mask", new JointMaskReloadListener()));
        helper.registerReloadListener(wrap("meshes", Meshes.INSTANCE));
        helper.registerReloadListener(wrap("animation_manager_client", AnimationManager.getInstance()));
        helper.registerReloadListener(wrap("item_skins", ItemSkinsReloadListener.INSTANCE));

        EpicFight.LOGGER.info("EpicFight client reload listeners registered (4)");
    }

    /**
     * Wraps a {@link PreparableReloadListener} in an {@link IdentifiableResourceReloadListener}
     * with the given fabric ID.
     */
    private static IdentifiableResourceReloadListener wrap(String id, PreparableReloadListener delegate, ResourceLocation... after) {
        return new IdentifiableReloadListenerWrapper(ResourceLocation.fromNamespaceAndPath(EpicFight.MODID, id), delegate, List.of(after));
    }

    private static IdentifiableResourceReloadListener wrap(String id, PreparableReloadListener delegate) {
        return wrap(id, delegate, new ResourceLocation[0]);
    }

    private record IdentifiableReloadListenerWrapper(ResourceLocation fabricId, PreparableReloadListener delegate, List<ResourceLocation> deps)
            implements IdentifiableResourceReloadListener {

        @Override
        public ResourceLocation getFabricId() {
            return fabricId;
        }

        @Override
        public Collection<ResourceLocation> getFabricDependencies() {
            return deps;
        }

        @Override
        public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                               ProfilerFiller preparationsProfiler, ProfilerFiller reloadProfiler,
                                               Executor backgroundExecutor, Executor gameExecutor) {
            return delegate.reload(stage, resourceManager, preparationsProfiler, reloadProfiler, backgroundExecutor, gameExecutor)
                    .exceptionally(throwable -> {
                        EpicFight.LOGGER.warn("Reload listener {} failed: {}", fabricId, throwable.getMessage());
                        return null;
                    });
        }
    }
}
