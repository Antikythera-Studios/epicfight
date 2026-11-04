package yesman.epicfight.api.ex_cap.modules.core.data;

import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.deferred.holders.DeferredPreset;
import yesman.epicfight.world.capabilities.item.WeaponCapability;

/**
 * Represents a single entry in the builder data file.
 * @param id The ID of the entry.
 * @param template The template associated with the entry.
 * @deprecated Use {@link DeferredPreset}, which provides the same deferred registry access.
 */
@Deprecated(forRemoval = true)
public record BuilderEntry(ResourceLocation id, WeaponCapability.Builder template) { }