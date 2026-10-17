package yesman.epicfight.registry.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.ex_cap.data.modifier.WeaponModifier;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.deferred.holders.DeferredModifier;

import java.util.function.Supplier;

public final class ModifierRegister extends DeferredRegisterShim<WeaponModifier.Builder> {
    private ModifierRegister(ResourceKey<? extends Registry<WeaponModifier.Builder>> registryKey, String namespace) {
        super(registryKey, namespace);
    }

    public static ModifierRegister create(String namespace)
    {
        return new ModifierRegister(EpicFightRegistries.Keys.MODIFIERS, namespace);
    }

    public DeferredModifier registerModifier(String name, Supplier<WeaponModifier.Builder> builder)
    {
        this.register(name, builder);

        ResourceKey<WeaponModifier.Builder> key = ResourceKey.create(
                this.getRegistryKey(),
                ResourceLocation.fromNamespaceAndPath(this.getNamespace(), name)
        );

        return new DeferredModifier(key, () -> null);
    }
}
