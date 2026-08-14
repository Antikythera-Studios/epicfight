package com.yesman.epicfight.platform;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/// Provides access to mod-loader specific functionalities that cannot be achieved using vanilla classes otherwise.
@NullMarked
public interface ModPlatform {
    boolean isDevelopmentEnvironment();

    /// This is for event triggering policy difference in between two mod loaders, Fabric and NeoForged.
    /// Some of NeoForged events are triggered by modification of the original Minecraft code so it is
    /// favorable to use event hooks that NeoForged provides rather than creating branched mixins.
    ///
    /// @return true in Fabric, false in NeoForged
    boolean triggerMixinHook();

    boolean isModLoaded(final String id);

    /// Returns mod info by mod id
    @Nullable ModInfo getLoadedModInfo(final String id);

    /// Returns mod info by mod id
    Collection<ModInfo> getLoadedMods();

    /// Creates a custom registry
    <T> Registry<T> createRegistry(final ResourceKey<Registry<T>> id);

    /// Creates a custom registry for data pack
    <T> void createDataPackRegistry(final ResourceKey<Registry<T>> id, final Codec<T> codec);

    /// Returns a registrar of a mod platform. Should be managed as static-final fields
    <T> Registrar<T> getRegistrar(final ResourceKey<? extends Registry<T>> id, final String namespace);

    interface Registrar<T> {
        <O extends T> Holder<O> register(final String name, final Supplier<O> supplier);

        <O extends T> Holder<O> register(final String name, final Function<Identifier, O> supplier);

        String namespace();

        /// Called on each `REGISTRY` field in entry classes to trigger class loading
        default void notifyPresence() {}
    }

    /// Registers a reload listener in specified dist(client/server).
    void registerReloadListener(final Identifier id, final PackType packType, final PreparableReloadListener reloadListener);

    /// Registers a callback fired whenever a player must receive data-pack-derived
    /// content: once when the player joins, and for every online player after /reload.
    /// `joined` is true on first join, false on /reload re-sync.
    void registerDataPackSyncCallback(final BiConsumer<ServerPlayer, Boolean> callback);

    PhysicalSide getPhysicalSide();

    /// Physical side only have two cases, client for where an integrated server exists and server for where
    /// a dedicated server exists.
    ///
    /// You have to distinguish the physical side from logical side. The physical client contains both logical client
    /// and server, whereas the physical server only contains the logical server.
    enum PhysicalSide {
        CLIENT, SERVER;

        public boolean isClient() {
            return this == CLIENT;
        }

        public boolean isServer() {
            return this == SERVER;
        }
    }

    record ModInfo(
        String modId,
        String versionString,
        File modFile
    ) {
    }
}
