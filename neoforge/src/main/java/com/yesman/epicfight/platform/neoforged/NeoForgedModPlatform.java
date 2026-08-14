package com.yesman.epicfight.platform.neoforged;

import com.mojang.serialization.Codec;
import com.yesman.akythera.core.util.ClientOnly;
import com.yesman.epicfight.neoforged.registries.SubtypedDeferredHolder;
import com.yesman.epicfight.platform.ModPlatform;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.registries.DataPackRegistryEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NewRegistryEvent;
import net.neoforged.neoforge.registries.RegistryBuilder;
import net.neoforged.neoforgespi.language.IModFileInfo;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

@NullMarked
public final class NeoForgedModPlatform implements ModPlatform {
    private final List<Registry<?>> customRegistries = new ArrayList<>();
    private final List<DatapackRegistryEntry<?>> customDataPackRegistries = new ArrayList<> ();
    private final List<NeoForgedRegistrar<?>> deferredRegisters = new ArrayList<>();
    private final Map<Identifier, PreparableReloadListener> resourceReloadListeners = new LinkedHashMap<>();
    private final Map<Identifier, PreparableReloadListener> dataReloadListeners = new LinkedHashMap<>();

    private final List<BiConsumer<ServerPlayer, Boolean>> dataPackSyncCallbacks = new ArrayList<>();

    private boolean frozen = false;

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public boolean triggerMixinHook() {
        return false;
    }

    @Override
    public boolean isModLoaded(final String id) {
        return ModList.get().isLoaded(id);
    }

    @Override
    public @Nullable ModInfo getLoadedModInfo(final String id) {
        IModFileInfo modFileInfo = ModList.get().getModFileById(id);

        if (modFileInfo == null) {
            return null;
        }

        return new ModInfo(id, modFileInfo.versionString(), modFileInfo.getFile().getFilePath().toFile());
    }

    @Override
    public Collection<ModInfo> getLoadedMods() {
        return ModList.get().getMods().stream().map(iModInfo -> {
            IModFileInfo modFileInfo = ModList.get().getModFileById(iModInfo.getModId());
            return new ModInfo(iModInfo.getModId(), iModInfo.getVersion().getQualifier(), modFileInfo.getFile().getFilePath().toFile());
        }).toList();
    }

    @Override
    public <T> Registry<T> createRegistry(ResourceKey<Registry<T>> id) {
        Registry<T> registry = new RegistryBuilder<>(id).create();
        customRegistries.add(registry);
        return registry;
    }

    @Override
    public <T> void createDataPackRegistry(ResourceKey<Registry<T>> id, Codec<T> codec) {
        customDataPackRegistries.add(new DatapackRegistryEntry<>(id, codec));
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Registrar<T> getRegistrar(ResourceKey<? extends Registry<T>> id, String namespace) {
        if (frozen) {
            throw new IllegalStateException("Registry is already frozen");
        }

        NeoForgedRegistrar<T> registrar = new NeoForgedRegistrar<>(id, namespace);
        deferredRegisters.add(registrar);

        return registrar;
    }

    @Override
    public void registerReloadListener(Identifier id, PackType packType, PreparableReloadListener reloadListener) {
        switch (packType) {
            case CLIENT_RESOURCES -> resourceReloadListeners.put(id, reloadListener);
            case SERVER_DATA -> dataReloadListeners.put(id, reloadListener);
        }
    }

    @Override
    public void registerDataPackSyncCallback(BiConsumer<ServerPlayer, Boolean> callback) {
        dataPackSyncCallbacks.add(callback);
    }

    // ********************************************************************************************
    // * NeoForged event handlers
    // *
    // * @SubscribeEvent annotation has no effect here. Only indicates they're used as even handler
    // ********************************************************************************************

    public void addListeners(IEventBus eventBus) {
        eventBus.addListener(this::newRegistryEvent);
        eventBus.addListener(this::newDatapackRegistryEvent);

        NeoForge.EVENT_BUS.addListener(this::addServerReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onDatapackSync);

        deferredRegisters.forEach(neoForgedRegistrar -> neoForgedRegistrar.deferredRegister().register(eventBus));
    }

    @ClientOnly
    public void addClientListeners(IEventBus eventBus) {
        eventBus.addListener(this::addClientReloadListeners);
    }

    @SubscribeEvent
    private void newRegistryEvent(NewRegistryEvent event) {
        customRegistries.forEach(event::register);
        frozen = true;
    }

    @SuppressWarnings("unchecked")
    @SubscribeEvent
    private void newDatapackRegistryEvent(DataPackRegistryEvent.NewRegistry event) {
        customDataPackRegistries.forEach(entry -> {
            DatapackRegistryEntry<Object> casted = (DatapackRegistryEntry<@NonNull Object>)entry;
            event.dataPackRegistry(casted.key(), casted.codec(), casted.codec());
        });
    }

    @SubscribeEvent
    private void addClientReloadListeners(AddClientReloadListenersEvent event) {
        resourceReloadListeners.forEach(event::addListener);
        dataReloadListeners.forEach(event::addListener);
    }

    @SubscribeEvent
    private void addServerReloadListeners(AddServerReloadListenersEvent event) {
        dataReloadListeners.forEach(event::addListener);
    }

    @SubscribeEvent
    private void onDatapackSync(OnDatapackSyncEvent event) {
        // event.getPlayer() == null means /reload -> every relevant player, joined = false
        final boolean joined = event.getPlayer() != null;

        event.getRelevantPlayers().forEach(player ->
            dataPackSyncCallbacks.forEach(callback -> callback.accept(player, joined))
        );
    }

    @Override
    public PhysicalSide getPhysicalSide() {
        return FMLEnvironment.getDist() == Dist.CLIENT ? PhysicalSide.CLIENT : PhysicalSide.SERVER;
    }

    private record NeoForgedRegistrar<T>(DeferredRegister<T> deferredRegister) implements Registrar<T> {
        public NeoForgedRegistrar(ResourceKey<? extends Registry<T>> id, String namespace) {
            this(DeferredRegister.create(id, namespace));
        }

        @Override
        public <O extends T> Holder<O> register(String name, Supplier<O> supplier) {
            deferredRegister.register(name, supplier);
            // TODO: Check if this workaround works fine, the restriction due to deferred holders typed as not an object's type but the registry's type
            return new SubtypedDeferredHolder<>(ResourceKey.create(ResourceKey.createRegistryKey(deferredRegister.getRegistryKey().identifier()),
                Identifier.fromNamespaceAndPath(deferredRegister.getNamespace(), name))) {};
        }

        @Override
        public <O extends T> Holder<O> register(String name, Function<Identifier, O> registryFunction) {
            deferredRegister.register(name, registryFunction);
            // TODO: Check if this workaround works fine, the restriction due to deferred holders typed as not an object's type but the registry's type
            return new SubtypedDeferredHolder<>(ResourceKey.create(ResourceKey.createRegistryKey(deferredRegister.getRegistryKey().identifier()),
                Identifier.fromNamespaceAndPath(deferredRegister.getNamespace(), name))) {};
        }

        @Override
        public String namespace() {
            return deferredRegister.getNamespace();
        }
    }

    private record DatapackRegistryEntry<T>(ResourceKey<Registry<T>> key, Codec<T> codec) {
    }
}
