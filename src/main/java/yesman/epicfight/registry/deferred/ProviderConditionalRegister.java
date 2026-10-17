package yesman.epicfight.registry.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.ex_cap.provider.ProviderConditional;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.deferred.holders.DeferredConditional;

import java.util.function.Supplier;

public final class ProviderConditionalRegister extends DeferredRegisterShim<ProviderConditional.Builder> {
    private ProviderConditionalRegister(ResourceKey<? extends Registry<ProviderConditional.Builder>> registryKey, String namespace) {
        super(registryKey, namespace);
    }

    public static ProviderConditionalRegister create(String namespace)
    {
        return new ProviderConditionalRegister(EpicFightRegistries.Keys.PROVIDER_CONDITIONALS, namespace);
    }

    /**
     * Registers a provider conditional and returns a specialized holder.
     * This handle is used to define logic gates for moveset assignments and style switching.
     */
    public DeferredConditional registerConditional(String name, Supplier<ProviderConditional.Builder> builder) {
        // 1. Register the builder to the NeoForge system via the internal supplier
        this.register(name, builder);

        // 2. Create the ResourceKey that matches the registry entry
        // This uses the registry's namespace (modid) and the path (name)
        ResourceKey<ProviderConditional.Builder> key = ResourceKey.create(
                this.getRegistryKey(),
                ResourceLocation.fromNamespaceAndPath(this.getNamespace(), name)
        );

        // 3. Return the specialized holder wrapping the key
        // This allows for type-safe referencing in moveset definitions
        return new DeferredConditional(key, () -> null);
    }
}
