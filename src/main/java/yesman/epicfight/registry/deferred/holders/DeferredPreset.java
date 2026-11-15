package yesman.epicfight.registry.deferred.holders;

import net.minecraft.resources.ResourceKey;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import yesman.epicfight.registry.deferred.ItemPresetRegister;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.registry.EpicFightRegistries;

/**
 * A type-safe, registry-backed proxy for {@link CapabilityItem.Builder} instances.
 * <p>
 * {@code DeferredPreset} extends {@link DeferredHolderShim} to provide a
 * combat-specific handle that remains synchronized with the global registry
 * throughout the mod-loading lifecycle. It is designed to handle the "lazy"
 * nature of modern registries, ensuring that weapon data is only accessible
 * once the registry bake process is finalized.
 * </p>
 * <h3>Advanced Architectural Role:</h3>
 * <ul>
 * <li><b>Lazy Evaluation:</b> Access to the underlying template (via {@link #value()})
 * is deferred until the registry is unfrozen, preventing "early-access" crashes
 * during static initialization.</li>
 * <li><b>Type Specialization:</b> Utilizes generics to provide a concrete
 * {@code Builder} type at the call site, eliminating the need for
 * unsafe casting when retrieving specialized capability data.</li>
 * <li><b>Single Source of Truth:</b> Directly references the shared registry
 * storage, making it compatible with registry remapping and data-driven
 * overrides.</li>
 * </ul>
 * @param <T> The specific subtype of {@link CapabilityItem.Builder} being held,
 * allowing for specialized capability access (e.g., {@code WeaponCapability.Builder}).
 * @see DeferredHolderShim
 * @see CapabilityItem.Builder
 */
public class DeferredPreset<T extends CapabilityItem.Builder<?>> extends DeferredHolderShim<CapabilityItem.Builder<?>, T> {

    /**
     * Internal constructor used by {@link ItemPresetRegister}.
     * @param key The unique {@link ResourceKey} identifying this preset in the
     * global capability registry.
     */
    @ApiStatus.Internal
    public DeferredPreset(ResourceKey<CapabilityItem.Builder<?>> key, java.util.function.Supplier<T> supplier) {
        super(EpicFightRegistries.Keys.BUILDERS, key.location(), supplier);
    }

    /**
     * Safely retrieves the configuration template.
     * <p>
     * <b>Note for Advanced Users:</b> This method must only be invoked during
     * runtime execution (e.g., capability building or style evaluation). Invoking
     * this during mod construction or setup events will result in a
     * {@link IllegalStateException}.
     * </p>
     * * @return The registered {@link T} template instance.
     */
    @Override
    @SuppressWarnings("unchecked")
    public @NotNull T value() {
        return (T) super.value();
    }
}