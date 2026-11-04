package yesman.epicfight.registry;

import yesman.epicfight.EpicFight;
import yesman.epicfight.api.animation.AnimationManager;

/// NeoForge-style addon bridge — only invoked when the `neoforge_compat`
/// mod is present (checked by the caller). Every `net.neoforged.*` touch
/// goes through reflection so this mod compiles and runs on a plain Fabric
/// classpath with no neoforge_compat dependency anywhere in the build.
final class NeoForgeCompatBridge {
    private NeoForgeCompatBridge() {}

    private static final String COMPAT = "net.neoforged.neoforge.internal.fabric.NeoForgeCompat";

    private static Object modBus() throws Exception {
        return Class.forName(COMPAT).getField("MOD_BUS").get(null);
    }

    private static void post(Object bus, Object event) throws Exception {
        bus.getClass().getMethod("postAny", Object.class).invoke(bus, event);
    }

    static void postAnimationRegistryEvent() {
        try {
            AnimationManager.AnimationRegistryEvent event = new AnimationManager.AnimationRegistryEvent();
            post(modBus(), event);
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
        } catch (Throwable t) {
            EpicFight.LOGGER.warn("AnimationRegistryEvent listeners failed", t);
        }
    }

    static void postSkillRegisterEvent() {
        try {
            Class<?> registerEvent = Class.forName("net.neoforged.neoforge.registries.RegisterEvent");
            Object event = registerEvent.getConstructor(net.minecraft.core.Registry.class).newInstance(EpicFightRegistries.SKILL);
            post(modBus(), event);
        } catch (Throwable t) {
            EpicFight.LOGGER.warn("Deferred RegisterEvent(SKILL) failed", t);
        }
    }

    static void postLoadComplete() {
        try {
            Object complete = Class.forName("net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent").getDeclaredConstructor().newInstance();
            post(modBus(), complete);
            complete.getClass().getMethod("drainWork").invoke(complete);
        } catch (Throwable t) {
            EpicFight.LOGGER.warn("Deferred FMLLoadCompleteEvent listeners failed", t);
        }
    }
}
