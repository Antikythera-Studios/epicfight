package yesman.epicfight.api.extension;

import net.minecraft.world.entity.Entity;

/// Fabric-compatible extension interface for Entity methods that exist in NeoForge but not vanilla.
/// Implemented via MixinEntity. Defaults match NeoForge's vanilla behavior.
public interface EntityExtension {
	/// NeoForge's Entity#canRiderInteract(). Default: false.
	default boolean epicfight$canRiderInteract() {
		return false;
	}

	/// NeoForge's Entity#shouldRiderSit(). Default: true.
	default boolean epicfight$shouldRiderSit() {
		return true;
	}

	/// Helper to cast an Entity to EntityExtension
	static EntityExtension of(Entity entity) {
		return (EntityExtension) entity;
	}
}
