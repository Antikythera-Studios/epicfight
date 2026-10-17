package yesman.epicfight.registry.deferred.holders;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.world.capabilities.item.custom.CustomData;
import yesman.epicfight.registry.EpicFightRegistries;

import java.util.function.Supplier;

public class DeferredCustomData<T extends CustomData<?>> extends DeferredHolderShim<CustomData<?>, T> {
    public DeferredCustomData(ResourceKey<CustomData<?>> key, Supplier<T> supplier) {
        super(EpicFightRegistries.Keys.WEAPON_DATA, key.location(), supplier);
    }
}
