package yesman.epicfight.registry.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.deferred.holders.DeferredCustomData;
import yesman.epicfight.world.capabilities.item.custom.CustomData;

import java.util.function.Supplier;

public final class CustomDataRegister extends DeferredRegisterShim<CustomData<?>> {

    private CustomDataRegister(ResourceKey<? extends Registry<CustomData<?>>> registryKey, String namespace) {
        super(registryKey, namespace);
    }

    public static CustomDataRegister createWeapon(String namespace)
    {
        return new CustomDataRegister(EpicFightRegistries.Keys.WEAPON_DATA, namespace);
    }

    public static CustomDataRegister createMoveset(String namespace)
    {
        return new CustomDataRegister(EpicFightRegistries.Keys.MOVESET_DATA, namespace);
    }


    public <T> DeferredCustomData<CustomData<T>> registerCustomData(String name, Supplier<CustomData<T>> data) {
        this.register(name, data);
        ResourceKey<CustomData<?>> key = ResourceKey.create(
                this.getRegistryKey(),
                ResourceLocation.fromNamespaceAndPath(this.getNamespace(), name)
        );

        return new DeferredCustomData<>(key, () -> null);
    }
}
