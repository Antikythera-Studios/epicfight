package yesman.epicfight.platform.fabric.event;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;

import java.util.HashMap;
import java.util.Map;

/// Fabric-compatible replacement for NeoForge's EntityAttributeModificationEvent.
/// On Fabric, there is no direct event for modifying entity attributes. Instead, we collect
/// the modifications here and apply them via MixinDefaultAttributes which injects into
/// DefaultAttributes.getSupplier() to add our custom attributes to the supplier.
public class EntityAttributeModificationEvent {

    private static final Map<EntityType<?>, Map<Holder<Attribute>, Double>> PENDING_MODIFICATIONS = new HashMap<>();

    public void add(EntityType<? extends LivingEntity> entityType, Holder<Attribute> attribute, double value) {
        // Unwrap DeferredHolderShim to its underlying Holder.Reference so that equals()/hashCode()
        // match the Holder.Reference keys used by vanilla AttributeSupplier.instances.
        Holder<Attribute> resolved = attribute;
        if (attribute instanceof DeferredHolderShim<?, ?> shim) {
            @SuppressWarnings("unchecked")
            Holder<Attribute> unwrapped = (Holder<Attribute>) shim.asHolder();
            if (unwrapped != null) {
                resolved = unwrapped;
            }
        }
        PENDING_MODIFICATIONS.computeIfAbsent(entityType, k -> new HashMap<>()).put(resolved, value);
    }

    public void add(EntityType<? extends LivingEntity> entityType, Holder<Attribute> attribute) {
        add(entityType, attribute, attribute.value().getDefaultValue());
    }

    /// Returns all pending modifications. Called by MixinDefaultAttributes to inject attributes
    /// into the vanilla DefaultAttributes supplier.
    public static Map<EntityType<?>, Map<Holder<Attribute>, Double>> getPendingModifications() {
        return PENDING_MODIFICATIONS;
    }

    /// Check if a given entity type has pending attribute modifications
    public static boolean hasModifications(EntityType<?> entityType) {
        return PENDING_MODIFICATIONS.containsKey(entityType);
    }
}
