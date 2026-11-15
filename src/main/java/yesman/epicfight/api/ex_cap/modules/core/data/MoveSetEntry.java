package yesman.epicfight.api.ex_cap.modules.core.data;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.ex_cap.data.Moveset;
import yesman.epicfight.registry.deferred.MovesetRegister;
import yesman.epicfight.registry.deferred.holders.DeferredMoveset;

/**
 * Represents an entry in the {@link Moveset} registry, containing the ID and the builder for the {@link Moveset}.
 * @param id The ID of the moveset.
 * @param builder The builder used to construct the moveset.
 * @deprecated For Removal. Replaced with {@link DeferredMoveset} and registered via {@link MovesetRegister} or by a {@link DeferredRegister}
 */
@Deprecated(forRemoval = true)
public record MoveSetEntry(ResourceLocation id, Moveset.Builder builder) {}