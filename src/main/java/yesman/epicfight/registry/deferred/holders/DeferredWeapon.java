package yesman.epicfight.registry.deferred.holders;

import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.WeaponCapability;
import yesman.epicfight.registry.EpicFightRegistries;

import java.util.function.Supplier;

public final class DeferredWeapon extends DeferredPreset<WeaponCapability.Builder> {
    @ApiStatus.Internal
    public DeferredWeapon(ResourceKey<CapabilityItem.Builder<?>> key, Supplier<WeaponCapability.Builder> supplier) {
        super(key, supplier);
    }
}
