package yesman.epicfight.registry;

import java.util.concurrent.atomic.AtomicBoolean;

import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.internal.fabric.NeoForgeCompat;
import net.neoforged.neoforge.registries.RegisterEvent;
import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;

/**
 * Runs the NeoForge-equivalent "all mod constructors done" phase once, at the
 * head of {@link net.minecraft.server.WorldLoader#load} — the first moment every
 * Fabric entrypoint (and therefore every animation/skill provider) is guaranteed
 * to have run.
 *
 * Order mirrors upstream NeoForge:
 *   1. {@link AnimationManager.AnimationRegistryEvent} — addon builders (EDP,
 *      WOM, EFN...) contribute animations; EF's own animations were already
 *      registered directly by {@code EpicFightFabric}.
 *   2. {@link RegisterEvent} for the SKILL registry — EDP's
 *      {@code AnimationRegistryOperations.onVirtualizeAnimations} clones real
 *      animations into virtual ones (all real animations now resolvable).
 *   3. {@link DeferredRegisterShim#drainLate()} — binds every registration that
 *      arrived after the main drain (mod constructors that ran late under our
 *      Fabric bootstrap), so generated skills resolve their animation keys.
 */
public final class DeferredInitPhase {
    private DeferredInitPhase() {}

    private static final AtomicBoolean ANIMATION_EVENT_FIRED = new AtomicBoolean(false);

    public static void run() {
        if (ANIMATION_EVENT_FIRED.compareAndSet(false, true)) {
            AnimationManager.AnimationRegistryEvent event = new AnimationManager.AnimationRegistryEvent();
            try {
                NeoForgeCompat.MOD_BUS.post(event);
            } catch (Throwable t) {
                EpicFight.LOGGER.warn("AnimationRegistryEvent listeners failed", t);
            }
            EpicFight.LOGGER.info(
                "AnimationRegistryEvent collected builders: {}",
                event.getBuilders().stream().map(AnimationManager.AnimationBuilder::namespace).toList()
            );
            event.getBuilders().forEach(b -> {
                try {
                    b.task().accept(b);
                } catch (Throwable t) {
                    EpicFight.LOGGER.warn("Animation builder task failed", t);
                }
            });
        }

        // RegisterEvent(SKILL) is safe to re-fire: virtualization helpers are
        // idempotent on already-created virtual ids.
        try {
            NeoForgeCompat.MOD_BUS.post(new RegisterEvent(EpicFightRegistries.SKILL));
        } catch (Throwable t) {
            EpicFight.LOGGER.warn("Deferred RegisterEvent(SKILL) failed", t);
        }

        DeferredRegisterShim.drainLate();

        // Entity patches: at this point every mod's DeferredRegister has drained,
        // so addon entity holders resolve to their real types (previously a holder
        // accessed pre-bind resolved to the defaulted-registry fallback — pig).
        try {
            yesman.epicfight.world.capabilities.EpicFightCapabilities.ENTITY_PATCH_PROVIDER.fireEntityPatchRegistryEvent();
        } catch (Throwable t) {
            EpicFight.LOGGER.warn("Deferred EntityPatchRegistryEvent failed", t);
        }

        // Re-fire load-complete once: animation modifiers/virtual-config passes
        // (EDP's onModifierAnimations, virtualization config defs) skipped any
        // animations that didn't exist at bootstrap — now everything resolves.
        if (LOAD_COMPLETE_REFIRED.compareAndSet(false, true)) {
            FMLLoadCompleteEvent complete = new FMLLoadCompleteEvent();
            try {
                NeoForgeCompat.MOD_BUS.post(complete);
            } catch (Throwable t) {
                EpicFight.LOGGER.warn("Deferred FMLLoadCompleteEvent listeners failed", t);
            }
            complete.drainWork();
        }
    }

    private static final AtomicBoolean LOAD_COMPLETE_REFIRED = new AtomicBoolean(false);
}
