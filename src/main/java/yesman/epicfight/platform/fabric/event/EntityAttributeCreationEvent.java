package yesman.epicfight.platform.fabric.event;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;

import java.util.HashMap;
import java.util.Map;

/// Fabric-compatible replacement for NeoForge's EntityAttributeCreationEvent.
/// On Fabric, attribute suppliers for custom entities are registered via FabricDefaultAttributeRegistry.
/// This class collects the suppliers and applies them via MixinDefaultAttributes.
public class EntityAttributeCreationEvent {

    private static final Map<EntityType<?>, AttributeSupplier> PENDING_SUPPLIERS = new HashMap<>();

    public void put(EntityType<?> entityType, AttributeSupplier supplier) {
        PENDING_SUPPLIERS.put(entityType, supplier);
    }

    /// Returns all pending attribute suppliers. Called by MixinDefaultAttributes to inject
    /// custom entity attribute suppliers into the vanilla DefaultAttributes system.
    public static Map<EntityType<?>, AttributeSupplier> getPendingSuppliers() {
        return PENDING_SUPPLIERS;
    }

    /// Check if a given entity type has a pending attribute supplier
    public static boolean hasSupplier(EntityType<?> entityType) {
        return PENDING_SUPPLIERS.containsKey(entityType);
    }

    /// Get the pending attribute supplier for a given entity type
    public static AttributeSupplier getSupplier(EntityType<?> entityType) {
        return PENDING_SUPPLIERS.get(entityType);
    }
}
