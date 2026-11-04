package yesman.epicfight.registry.deferred_shim;

import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.Holder;
import com.mojang.serialization.Lifecycle;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * lookup() must alias an existing bound holder and never register or memoize
 * misses. Uses a standalone MappedRegistry - no EpicFight/Fabric registry init.
 */
class DeferredHolderShimLookupTest {

    private static final ResourceKey<Registry<String>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath("port_test", "strings"));

    private static MappedRegistry<String> newRegistry() {
        return new MappedRegistry<>(REGISTRY_KEY, Lifecycle.stable(), false);
    }

    @Test
    void lookupAliasesBoundEntryWithoutMutation() {
        MappedRegistry<String> registry = newRegistry();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("port_test", "alpha");
        String registered = "alpha-value";
        Registry.register(registry, id, registered);
        registry.freeze(); // holder values bind at freeze, as with runtime registries
        int sizeBefore = registry.size();

        DeferredHolderShim<String, String> shim = DeferredHolderShim.lookup(registry, id);

        assertNotNull(shim);
        assertSame(registered, shim.get(), "lookup returns the exact registered value");
        assertSame(registered, shim.value());
        assertTrue(shim.isBound());
        assertTrue(shim.isPresent());
        assertEquals(ResourceKey.create(REGISTRY_KEY, id), shim.getKey());
        assertSame(registry.getHolder(ResourceKey.create(REGISTRY_KEY, id)).orElseThrow(), shim.asHolder(),
                "asHolder is the registry's own Holder.Reference");
        assertEquals(sizeBefore, registry.size(), "lookup must not register anything");
    }

    @Test
    void lookupMissReturnsNullAndIsNotMemoized() {
        MappedRegistry<String> registry = newRegistry();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("port_test", "late");

        assertNull(DeferredHolderShim.lookup(registry, id), "missing entry returns null");

        String registered = "late-value";
        Registry.register(registry, id, registered);
        registry.freeze();

        DeferredHolderShim<String, String> shim = DeferredHolderShim.lookup(registry, id);
        assertNotNull(shim, "later registration is still found (no negative memoization)");
        assertSame(registered, shim.get());
    }

    @Test
    void lookupRejectsUnboundReference() {
        MappedRegistry<String> registry = newRegistry();
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("port_test", "prefreeze");
        String registered = "prefreeze-value";
        Registry.register(registry, id, registered);

        ResourceKey<String> key = ResourceKey.create(REGISTRY_KEY, id);
        assertFalse(registry.getHolder(key).orElseThrow().isBound(),
                "sanity: stand-alone reference is unbound before freeze");
        assertNull(DeferredHolderShim.lookup(registry, id),
                "an unbound reference is not yet resolvable - must return null");

        registry.freeze();
        DeferredHolderShim<String, String> shim = DeferredHolderShim.lookup(registry, id);
        assertNotNull(shim);
        assertSame(registered, shim.get());
        assertSame(registry.getHolder(key).orElseThrow(), shim.asHolder());
    }
}
