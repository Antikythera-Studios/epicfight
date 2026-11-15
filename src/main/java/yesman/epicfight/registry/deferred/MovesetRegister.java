package yesman.epicfight.registry.deferred;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.registry.deferred.holders.DeferredMoveset;

import java.util.function.Supplier;

public final class MovesetRegister extends DeferredRegisterShim<Moveset.Builder> {
    private MovesetRegister(ResourceKey<? extends Registry<Moveset.Builder>> registryKey, String namespace) {
        super(registryKey, namespace);
    }

    public static MovesetRegister create(String namespace)
    {
        return new MovesetRegister(EpicFightRegistries.Keys.MOVESETS, namespace);
    }

    public DeferredMoveset registerMoveset(String name, Supplier<Moveset.Builder> builder) {
        ResourceKey<Moveset.Builder> key = ResourceKey.create(
                this.getRegistryKey(),
                ResourceLocation.fromNamespaceAndPath(this.getNamespace(), name)
        );
        DeferredMoveset moveset = new DeferredMoveset(key, builder);
        this.addEntry(moveset);
        return moveset;
    }
}
