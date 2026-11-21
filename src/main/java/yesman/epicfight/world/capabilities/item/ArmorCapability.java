package yesman.epicfight.world.capabilities.item;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.Enchantment;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightEnchantments;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;

public class ArmorCapability extends CapabilityItem {
	protected final double weight;
	protected final double stunArmor;
	protected final ArmorItem.Type armorType;
	
	protected ArmorCapability(ArmorCapability.Builder builder) {
		super(builder);
		
		this.armorType = builder.armorType;
		this.weight = builder.weight;
		this.stunArmor = builder.stunArmor;
	}
	
	@Override
	public void modifyItemTooltip(ItemStack itemStack, List<Component> itemTooltip, LivingEntityPatch<?> entitypatch) {
        int index = 0;
        boolean modifyIn = false;

		double weightValue = this.weight;
		double stunArmorValue = this.stunArmor;
		Holder<Enchantment> lightEnchantment = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.LIGHTWEIGHT);
		Holder<Enchantment> unshakableEnchantment = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.UNSHAKABLE);
		if (itemStack.getEnchantmentLevel(lightEnchantment) > 0) {
			weightValue *= 1 - (itemStack.getEnchantmentLevel(lightEnchantment) * 0.1);
		}
		if (itemStack.getEnchantmentLevel(unshakableEnchantment) > 0) {
			stunArmorValue *= 1 + (itemStack.getEnchantmentLevel(unshakableEnchantment) * 0.07);
		}

        for (int i = 0; i < itemTooltip.size(); i++) {
            Component textComp = itemTooltip.get(i);
            index = i;

            if (this.findComponentArgument(textComp, Attributes.ARMOR.value().getDescriptionId()) != null) {
                modifyIn = true;
            }
            // Keep searching if Armor Toughness attribute is on tooltip
            else if (this.findComponentArgument(textComp, Attributes.ARMOR_TOUGHNESS.value().getDescriptionId()) != null) {
                modifyIn = true;
            }
            // Keep searching if Knockback Resistence attribute is on tooltip
            else if (this.findComponentArgument(textComp, Attributes.KNOCKBACK_RESISTANCE.value().getDescriptionId()) != null) {
                modifyIn = true;
                break;
            }
        }

        index++;

        if (!modifyIn) {
            itemTooltip.add(index, Component.literal(""));
            index++;
            itemTooltip.add(index, Component.translatable("epicfight.gui.attribute").withStyle(ChatFormatting.GRAY));
            index++;
        }

        Holder<Attribute> stunArmor = EpicFightAttributes.STUN_ARMOR;
        Holder<Attribute> weight = EpicFightAttributes.WEIGHT;

        if (this.stunArmor != 0.0D && validateAttribute(entitypatch, stunArmor)) {
            itemTooltip.add(index, Component.literal("+").append(Component.translatable(stunArmor.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(stunArmorValue))).withStyle(ChatFormatting.BLUE));
        }

        if (this.weight != 0.0D && validateAttribute(entitypatch, weight)) {
            itemTooltip.add(index, Component.literal("+").append(Component.translatable(weight.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(weightValue))).withStyle(ChatFormatting.BLUE));
        }
	}
	
	public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiersForArmor(LivingEntityPatch<?> patch, EquipmentSlot slot) {
		Multimap<Holder<Attribute>, AttributeModifier> map = HashMultimap.create();
		ResourceLocation modifierId = ResourceLocation.withDefaultNamespace("armor." + this.armorType.getName());

		if (slot.isArmor())
		{
			double weight = this.weight;
			double stunArmor = this.stunArmor;
			Holder<Enchantment> lightEnchantment = patch.getOriginal().level().holderOrThrow(EpicFightEnchantments.LIGHTWEIGHT);
			Holder<Enchantment> unshakableEnchantment = patch.getOriginal().level().holderOrThrow(EpicFightEnchantments.UNSHAKABLE);
			ItemStack stack = patch.getOriginal().getItemBySlot(slot);
			if (stack.getEnchantmentLevel(lightEnchantment) > 0) {
				weight *= 1 - (stack.getEnchantmentLevel(lightEnchantment) * 0.1);
			}
			if (stack.getEnchantmentLevel(unshakableEnchantment) > 0) {
				stunArmor *= 1 + (stack.getEnchantmentLevel(unshakableEnchantment) * 0.07);
			}

			map.put(EpicFightAttributes.WEIGHT, new AttributeModifier(modifierId, weight, Operation.ADD_VALUE));
			map.put(EpicFightAttributes.STUN_ARMOR, new AttributeModifier(modifierId, stunArmor, Operation.ADD_VALUE));
		}
        return map;
    }

	@Override
	public void modifySimplyTooltip(ItemStack itemStack, List<Component> itemTooltip, LivingEntityPatch<?> entitypatch) {
		Holder<Attribute> stunArmor = EpicFightAttributes.STUN_ARMOR;
		Holder<Attribute> weight = EpicFightAttributes.WEIGHT;

		double weightValue = this.weight;
		double stunArmorValue = this.stunArmor;
		Holder<Enchantment> lightEnchantment = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.LIGHTWEIGHT);
		Holder<Enchantment> unshakableEnchantment = entitypatch.getOriginal().level().holderOrThrow(EpicFightEnchantments.UNSHAKABLE);
		if (itemStack.getEnchantmentLevel(lightEnchantment) > 0) {
			weightValue *= 1 - (itemStack.getEnchantmentLevel(lightEnchantment) * 0.1);
		}
		if (itemStack.getEnchantmentLevel(unshakableEnchantment) > 0) {
			stunArmorValue *= 1 + (itemStack.getEnchantmentLevel(unshakableEnchantment) * 0.07);
		}

		if (this.stunArmor != 0.0D && validateAttribute(entitypatch, stunArmor)) {
			itemTooltip.add(Component.literal("+").append(Component.translatable(stunArmor.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(stunArmorValue))).withStyle(ChatFormatting.BLUE));
		}

		if (this.weight != 0.0D && validateAttribute(entitypatch, weight)) {
			itemTooltip.add(Component.literal("+").append(Component.translatable(weight.value().getDescriptionId() + ".value", ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT.format(weightValue))).withStyle(ChatFormatting.BLUE));
		}
	}

	public static ArmorCapability.Builder builder() {
		return new ArmorCapability.Builder();
	}
	
	public static class Builder extends CapabilityItem.Builder<Builder> {
		private ArmorItem.Type armorType;
		private double weight;
		private double stunArmor;
		
		protected Builder() {
			this.constructor = ArmorCapability::new;
			this.weight = -1.0D;
			this.stunArmor = -1.0D;
		}
		
		public Builder byItem(Item item) {
			if (item instanceof ArmorItem armorItem) {
				Holder<ArmorMaterial> armorMaterial = armorItem.getMaterial();
				this.armorType = armorItem.getType();
				
				if (this.weight < 0.0D) {
					this.weight = armorMaterial.value().defense().getOrDefault(this.armorType, 1) * 2.5F;
				}
				
				if (this.stunArmor < 0.0D) {
					this.stunArmor = armorMaterial.value().defense().getOrDefault(this.armorType, 1) * 0.375F;
				}
			}
			
			return this;
		}
		
		public Builder weight(double weight) {
			this.weight = weight;
			return this;
		}
		
		public Builder stunArmor(double stunArmor) {
			this.stunArmor = stunArmor;
			return this;
		}
	}
}