package com.yesman.epicfight.neoforged.registries;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/// Another custom implementation of neoforge's [DeferredHolder], with scalable generic type for the subtypes of
/// the original registry's generic type. This is because Holder<R> can't be cast to Holder<T> whereas Epic Fight wants
/// to return Holders with subtype generics
@NullMarked
public class SubtypedDeferredHolder<R, T extends R> implements Holder<T> {
    protected final ResourceKey<T> key;

    protected SubtypedDeferredHolder(ResourceKey<T> key) {
        this.key = Objects.requireNonNull(key);
        this.bind(false);
    }

    @Nullable
    private Holder<T> holder = null;

    @Nullable
    @SuppressWarnings("unchecked")
    protected Registry<R> getRegistry() {
        return (Registry<R>) BuiltInRegistries.REGISTRY.getValue(this.key.registry());
    }

    @SuppressWarnings("unchecked")
    protected final void bind(boolean throwOnMissingRegistry) {
        if (this.holder != null) return;

        Registry<R> registry = getRegistry();

        if (registry != null) {
            this.holder = (Holder<T>)registry.get((ResourceKey<R>)this.key).orElse(null);
        } else if (throwOnMissingRegistry) {
            throw new IllegalStateException("Registry not present for " + this + ": " + this.key.registry());
        }
    }

    @Override
    public T value() {
        bind(true);
        if (this.holder == null) {
            throw new NullPointerException("Trying to access unbound value: " + this.key);
        }

        return this.holder.value();
    }

    @Nullable
    public ResourceKey<T> getKey() {
        return ((Holder<T>) this).unwrapKey().orElse(null);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Holder<?> h && h.kind() == Kind.REFERENCE && h.getKey() == this.key;
    }

    @Override
    public int hashCode() {
        return this.key.hashCode();
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH, "DeferredHolder{%s}", this.key);
    }

    @Override
    public boolean isBound() {
        bind(false);
        return this.holder != null && this.holder.isBound();
    }

    @Override
    public boolean areComponentsBound() {
        bind(false);
        return this.holder != null && this.holder.areComponentsBound();
    }

    @Override
    public boolean is(Identifier id) {
        return id.equals(this.key.identifier());
    }

    @Override
    public boolean is(ResourceKey<T> key) {
        return key == this.key;
    }

    @Override
    public boolean is(Predicate<ResourceKey<T>> filter) {
        return filter.test(this.key);
    }

    @Override
    public boolean is(TagKey<T> tag) {
        bind(false);
        return this.holder != null && this.holder.is(tag);
    }

    @Override
    @Deprecated
    public boolean is(Holder<T> holder) {
        bind(false);
        return this.holder != null && this.holder.is(holder);
    }

    @Override
    public <Z> @Nullable Z getData(DataMapType<T, Z> type) {
        bind(false);
        return holder == null ? null : holder.getData(type);
    }

    @Override
    public Stream<TagKey<T>> tags() {
        bind(false);
        return this.holder != null ? this.holder.tags() : Stream.empty();
    }

    @Override
    public DataComponentMap components() {
        bind(true);
        return this.holder != null ? this.holder.components() : DataComponentMap.EMPTY;
    }

    @Override
    public Either<ResourceKey<T>, T> unwrap() {
        // Holder.Reference always returns the key, do the same here.
        return Either.left(this.key);
    }

    @Override
    public Optional<ResourceKey<T>> unwrapKey() {
        return Optional.of(this.key);
    }

    @Override
    public Kind kind() {
        return Kind.REFERENCE;
    }

    @Override
    public boolean canSerializeIn(HolderOwner<T> owner) {
        bind(false);
        return this.holder != null && this.holder.canSerializeIn(owner);
    }

    @Override
    public Holder<T> getDelegate() {
        bind(false);
        return this.holder != null ? this.holder.getDelegate() : this;
    }
}
