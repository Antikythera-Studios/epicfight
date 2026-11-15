package yesman.epicfight.registry.deferred.holders;

import net.minecraft.resources.ResourceKey;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.api.ex_cap.data.modifier.WeaponModifier;
import yesman.epicfight.registry.EpicFightRegistries;

import java.util.function.Supplier;

public final class DeferredModifier extends DeferredHolderShim<WeaponModifier.Builder, WeaponModifier.Builder>
{
    public DeferredModifier(ResourceKey<WeaponModifier.Builder> key, Supplier<WeaponModifier.Builder> supplier) {
        super(EpicFightRegistries.Keys.MODIFIERS, key.location(), supplier);
    }
}
