package yesman.epicfight.registry.callbacks;

import net.minecraft.core.Registry;

/// Stub interfaces for NeoForge registry callbacks.
/// On Fabric, registry callbacks are handled differently.
public interface RegistryCallback {

    interface BakeCallback<T> extends RegistryCallback {
        void onBake(Registry<T> registry);
    }

    interface ClearCallback<T> extends RegistryCallback {
        void onClear(Registry<T> registry, boolean full);
    }

    interface AddCallback<T> extends RegistryCallback {
        void onAdd(Registry<T> registry, int rawId, net.minecraft.resources.ResourceKey<T> key, T newValue);
    }
}
