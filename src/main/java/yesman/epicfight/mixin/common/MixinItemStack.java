package yesman.epicfight.mixin.common;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import yesman.epicfight.api.event.impl.VanillaItemEventHooks;

import java.util.function.BiConsumer;

/// Mixin for [ItemStack] that intercepts [forEachModifier] to add Epic Fight's item attribute modifiers.
///
/// This replaces NeoForge's [ItemAttributeModifierEvent].
@Mixin(value = ItemStack.class)
public abstract class MixinItemStack {
    @Inject(
        at = @At(value = "HEAD"),
        method = "forEachModifier(Lnet/minecraft/world/entity/EquipmentSlotGroup;Ljava/util/function/BiConsumer;)V"
    )
    private void epicfight$forEachModifier(EquipmentSlotGroup slotGroup, BiConsumer<Holder<Attribute>, AttributeModifier> consumer, CallbackInfo info) {
        ItemStack self = (ItemStack)(Object)this;
        // Only add Epic Fight modifiers for the MAINHAND slot group
        if (slotGroup == EquipmentSlotGroup.MAINHAND) {
            VanillaItemEventHooks.onModifyItemAttribute(self, (attribute, modifier, slot) -> {
                if (slot == EquipmentSlotGroup.MAINHAND) {
                    consumer.accept(attribute, modifier);
                }
            });
        }
    }
}
