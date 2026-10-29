package yesman.epicfight.registry.deferred.holders;

import net.minecraft.resources.ResourceKey;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import org.jetbrains.annotations.ApiStatus;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.registry.EpicFightRegistries;

import java.util.function.Supplier;

public final class DeferredMoveset extends DeferredHolderShim<Moveset.Builder, Moveset.Builder> {
    @ApiStatus.Internal
    public DeferredMoveset(ResourceKey<Moveset.Builder> key, Supplier<Moveset.Builder> supplier) {
        super(EpicFightRegistries.Keys.MOVESETS, key.location(), supplier);
    }
}
