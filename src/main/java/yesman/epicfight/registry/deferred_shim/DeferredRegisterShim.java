package yesman.epicfight.registry.deferred_shim;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.EpicFight;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/// Fabric-compatible replacement for NeoForge's [DeferredRegister].
///
/// Queues registrations and processes them when [#accept()] is called
/// (typically from the mod initializer).
///
/// @param <T> the registry element type
public class DeferredRegisterShim<T> {
    private final ResourceKey<?> registryKey;
    private final String modId;
    private final List<DeferredHolderShim<T, ?>> entries = new ArrayList<>();
    private boolean accepted = false;

    public DeferredRegisterShim(ResourceKey<?> registryKey, String modId) {
        this.registryKey = registryKey;
        this.modId = modId;
    }

    public String getNamespace() {
        return modId;
    }

    @SuppressWarnings("unchecked")
    public <R> ResourceKey<Registry<R>> getRegistryKey() {
        return (ResourceKey<Registry<R>>) registryKey;
    }

    public <I extends T> DeferredHolderShim<T, I> register(String name, Supplier<I> supplier) {
        DeferredHolderShim<T, I> holder = new DeferredHolderShim<>(registryKey, ResourceLocation.fromNamespaceAndPath(modId, name), supplier);
        addOrBind(holder);
        return holder;
    }

    /// Adds a pre-created DeferredHolderShim subclass to the entries list.
    /// Used by ItemPresetRegister to register DeferredWeapon/DeferredPreset instances
    /// directly, so that accept() binds the same instance that is returned to the caller.
    protected <I extends T> DeferredHolderShim<T, I> addEntry(DeferredHolderShim<T, I> holder) {
        addOrBind(holder);
        return holder;
    }

    /// Overload that accepts a Function<ResourceLocation, I> like NeoForge's DeferredRegister.
    public <I extends T> DeferredHolderShim<T, I> register(String name, java.util.function.Function<ResourceLocation, I> factory) {
        ResourceLocation location = ResourceLocation.fromNamespaceAndPath(modId, name);
        DeferredHolderShim<T, I> holder = new DeferredHolderShim<>(registryKey, location, () -> factory.apply(location));
        addOrBind(holder);
        return holder;
    }

    /// On NeoForge every mod constructor runs before the registry events drain, so
    /// registering from a late @Mod ctor is legal. Our drain can happen before some
    /// addon constructors — and before all entrypoints' animations exist — so
    /// post-accept registrations are queued and bound by [#drainLate] at the start
    /// of datapack load, when every registration is guaranteed present.
    private static final java.util.List<Runnable> LATE_BINDS = new java.util.concurrent.CopyOnWriteArrayList<>();

    private void addOrBind(DeferredHolderShim<T, ?> holder) {
        entries.add(holder);
        if (accepted) {
            LATE_BINDS.add(() -> bindLate(holder));
        }
    }

    /// Vanilla freezes every registry at the end of the first datapack load;
    /// addon registrations can arrive after that, so reopen the registry just
    /// for the bind and refreeze afterwards.
    private void bindLate(DeferredHolderShim<T, ?> holder) {
        var registry = getRegistry();
        if (registry == null) {
            org.slf4j.LoggerFactory.getLogger("epicfight")
                .warn("Registry {} still absent at late bind — skipped {}", registryKey.location(), holder.getId());
            return;
        }
        if (registry instanceof yesman.epicfight.mixin.common.MappedRegistryAccessor accessor
                && accessor.epicfight$isFrozen()) {
            accessor.epicfight$setFrozen(false);
            try {
                holder.bind(registry);
            } finally {
                ((net.minecraft.core.MappedRegistry<?>) registry).freeze();
            }
        } else {
            holder.bind(registry);
        }
    }

    /// Binds every queued late registration. Called once at the head of
    /// [net.minecraft.server.WorldLoader#load] (all entrypoints have run by then);
    /// idempotent and safe to call again on later datapack loads.
    public static void drainLate() {
        for (Runnable bind : LATE_BINDS) {
            try {
                bind.run();
            } catch (Throwable t) {
                org.slf4j.LoggerFactory.getLogger("epicfight")
                    .warn("Late registry bind failed", t);
            }
        }
        LATE_BINDS.clear();
    }

    public java.util.Set<DeferredHolderShim<T, ?>> getEntries() {
        return new java.util.LinkedHashSet<>(entries);
    }
    public void accept() {
        if (accepted) {
            return;
        }
        accepted = true;

        // The target registry may not be class-loaded yet if this accept fires
        // before EpicFightRegistries initializes — defer those entries to
        // drainLate instead of exploding the caller's @Mod constructor.
        Registry<T> registry = getRegistry();
        if (registry == null) {
            for (DeferredHolderShim<T, ?> entry : entries) {
                LATE_BINDS.add(() -> bindLate(entry));
            }
            return;
        }
        for (DeferredHolderShim<T, ?> entry : entries) {
            entry.bind(registry);
        }
    }

    @SuppressWarnings("unchecked")
    private Registry<T> getRegistry() {
        return (Registry<T>) BuiltInRegistries.REGISTRY.get(registryKey.location());
    }
}
