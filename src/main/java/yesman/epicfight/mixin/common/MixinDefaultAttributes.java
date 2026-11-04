package yesman.epicfight.mixin.common;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import yesman.epicfight.platform.fabric.event.EntityAttributeCreationEvent;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import java.util.Map;

/// Mixin to inject Epic Fight's custom attributes into the vanilla DefaultAttributes system.
/// On NeoForge, this was handled by EntityAttributeCreationEvent and EntityAttributeModificationEvent.
/// On Fabric, we intercept getSupplier() to add our custom attributes to the returned supplier.
@Mixin(value = DefaultAttributes.class)
public abstract class MixinDefaultAttributes {

    @Inject(method = "getSupplier", at = @At("HEAD"), cancellable = true)
    private static void epicfight$getSupplier(EntityType<? extends LivingEntity> entityType, CallbackInfoReturnable<AttributeSupplier> cir) {
        // Check for custom entity attribute suppliers (creation event)
        if (EntityAttributeCreationEvent.hasSupplier(entityType)) {
            AttributeSupplier customSupplier = EntityAttributeCreationEvent.getSupplier(entityType);
            // Apply modifications on top of the custom supplier
            AttributeSupplier modified = applyModifications(entityType, customSupplier);
            cir.setReturnValue(modified);
        }
    }

    @Inject(method = "getSupplier", at = @At("RETURN"), cancellable = true)
    private static void epicfight$getSupplierReturn(EntityType<? extends LivingEntity> entityType, CallbackInfoReturnable<AttributeSupplier> cir) {
        // Don't interfere with custom entities (already handled in HEAD inject)
        if (EntityAttributeCreationEvent.hasSupplier(entityType)) {
            return;
        }

        // For existing entities, apply modifications to the vanilla supplier
        if (EntityAttributeModificationEvent.hasModifications(entityType)) {
            AttributeSupplier vanillaSupplier = cir.getReturnValue();
            AttributeSupplier modified = applyModifications(entityType, vanillaSupplier);
            cir.setReturnValue(modified);
        }
    }

    /// Apply pending attribute modifications to a supplier.
    /// Uses AttributeSupplier.builder() to create a new supplier that combines the base
    /// attributes with our custom Epic Fight attributes.
    private static AttributeSupplier applyModifications(EntityType<?> entityType, AttributeSupplier base) {
        // Normalize modification keys to canonical registry holders as well: DeferredHolderShim
        // keys stored before binding must still compare equal to the Holder.Reference keys used
        // by the runtime attribute map.
        Map<Holder<Attribute>, Double> mods = new java.util.HashMap<>();
        for (Map.Entry<Holder<Attribute>, Double> entry : EntityAttributeModificationEvent.getPendingModifications().getOrDefault(entityType, Map.of()).entrySet()) {
            mods.put(epicfight$canonicalAttributeHolder(entry.getKey()), entry.getValue());
        }
        AttributeSupplier.Builder builder = AttributeSupplier.builder();

        // Copy existing attribute base values from the base supplier. Attribute holders must
        // be canonical registry holders: packets resolve attributes to Holder.Reference, and
        // DeferredHolderShim keys are not reachable from Holder.Reference HashMap lookups.
        // Base entries whose canonical holder is overridden by a modification are skipped so
        // the builder never sees duplicate keys and the modification value wins.
        for (Map.Entry<Holder<Attribute>, AttributeInstance> entry : base.instances.entrySet()) {
            Holder<Attribute> attribute = epicfight$canonicalAttributeHolder(entry.getKey());
            if (!mods.containsKey(attribute)) {
                builder.add(attribute, entry.getValue().getBaseValue());
            }
        }

        // Add our custom Epic Fight attributes (modifications override base values, matching
        // NeoForge's EntityAttributeModificationEvent semantics)
        for (Map.Entry<Holder<Attribute>, Double> entry : mods.entrySet()) {
            builder.add(epicfight$canonicalAttributeHolder(entry.getKey()), entry.getValue());
        }

        return builder.build();
    }

    private static Holder<Attribute> epicfight$canonicalAttributeHolder(Holder<Attribute> holder) {
        ResourceKey<Attribute> key = holder.unwrapKey().orElse(null);
        if (key == null) {
            return holder;
        }
        return BuiltInRegistries.ATTRIBUTE.getHolder(key).map(reference -> (Holder<Attribute>)reference).orElse(holder);
    }
}
