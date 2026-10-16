package yesman.epicfight.registry.deferred_shim;
import yesman.epicfight.EpicFight;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/// Fabric-compatible replacement for NeoForge's [DeferredHolder].
/// Implements [Holder<T>] so it can be used wherever a Holder is expected.
public class DeferredHolderShim<T, I extends T> implements Holder<T> {
    private final ResourceKey<T> key;
    private final Supplier<I> supplier;
    private I value;
    private Holder<T> holder;

    @SuppressWarnings("unchecked")
    public DeferredHolderShim(Object registryKey, ResourceLocation location, Supplier<I> supplier) {
        this.key = registryKey != null ? ResourceKey.create((ResourceKey<Registry<T>>) (Object) registryKey, location) : null;
        this.supplier = supplier;
    }

    /// Aliases an already-bound registry entry: returns a shim over the existing
    /// holder, or null when `location` is absent or the reference is not yet
    /// bound (e.g. the registry has not been frozen). Never mutates the registry.
    public static <T> DeferredHolderShim<T, T> lookup(Registry<T> registry, ResourceLocation location) {
        Holder.Reference<T> reference = registry.getHolder(ResourceKey.create(registry.key(), location)).orElse(null);
        if (reference == null || !reference.isBound()) {
            return null;
        }
        T value = reference.value();
        DeferredHolderShim<T, T> result = new DeferredHolderShim<>(registry.key(), location, () -> value);
        result.value = value;
        result.holder = reference;
        return result;
    }

    @SuppressWarnings("unchecked")
    void bind(Registry<T> registry) {
        I supplied = supplier.get();
        this.value = supplied;
        Registry.register(registry, key.location(), supplied);
        this.holder = registry.getHolder((net.minecraft.resources.ResourceKey<T>) key).orElse(null);
    }

    public I get() {
        if (value == null) {
            EpicFight.LOGGER.warn("Accessed before registration: " + (key != null ? key.location() : "unknown") + ", returning null"); return null;
        }
        return value;
    }

    public boolean isPresent() {
        return value != null;
    }

    public ResourceKey<T> getKey() {
        return key;
    }

    public ResourceLocation getId() {
        return key.location();
    }

    public Optional<Holder<T>> getHolder() {
        return Optional.ofNullable(holder);
    }

    public Holder<T> asHolder() {
        if (holder == null) {
            EpicFight.LOGGER.warn("Accessed before registration: " + (key != null ? key.location() : "unknown") + ", returning null"); return null;
        }
        return holder;
    }

    // Holder<T> interface methods
    @Override
    public T value() {
        return get();
    }

    @Override
    public boolean isBound() {
        return value != null;
    }

    @Override
    public boolean is(ResourceKey<T> key) {
        return this.key == key;
    }

    @Override
    public boolean is(Holder<T> holder) {
        return this.holder != null && this.holder.is(holder);
    }

    @Override
    public Optional<ResourceKey<T>> unwrapKey() {
        return Optional.of(key);
    }

    @Override
    public com.mojang.datafixers.util.Either<ResourceKey<T>, T> unwrap() {
        // Must always be left(key): registry holders serialize by ID. Returning
        // right(value) makes holder codecs treat this as an inline unregistered
        // value and crash with "Unregistered holder" during NBT save.
        return com.mojang.datafixers.util.Either.left(key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(net.minecraft.core.HolderOwner<T> owner) {
        return holder != null ? holder.canSerializeIn(owner) : false;
    }

    @Override
    public java.util.stream.Stream<net.minecraft.tags.TagKey<T>> tags() {
        return holder != null ? holder.tags() : java.util.stream.Stream.empty();
    }

    @Override
    public boolean is(net.minecraft.tags.TagKey<T> tag) {
        return holder != null && holder.is(tag);
    }

    @Override
    public boolean is(java.util.function.Predicate<ResourceKey<T>> predicate) {
        return predicate.test(key);
    }

    @Override
    public boolean is(ResourceLocation location) {
        return this.key.location().equals(location);
    }

    @Override
    public String toString() {
        return "DeferredHolderShim[" + key.location() + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof DeferredHolderShim<?, ?> that) {
            return Objects.equals(key, that.key);
        }
        // Holder.Reference uses identity equals/hashCode, so we must delegate to the
        // underlying bound holder to be compatible with maps keyed by Holder.Reference
        // (e.g. AttributeSupplier.instances, AttributeMap.attributes).
        if (holder != null) {
            return holder.equals(o);
        }
        if (o instanceof Holder<?> other) {
            Optional<?> otherKey = other.unwrapKey();
            return otherKey.isPresent() && Objects.equals(key, otherKey.get());
        }
        return false;
    }

    @Override
    public int hashCode() {
        // Delegate to the bound Holder.Reference's identity hashCode so that HashMap
        // lookups against maps keyed by Holder.Reference (like AttributeSupplier.instances)
        // find this shim. Holder.Reference does NOT override hashCode, so it uses
        // System.identityHashCode — we must match that.
        if (holder != null) {
            return System.identityHashCode(holder);
        }
        return key.hashCode();
    }
}
