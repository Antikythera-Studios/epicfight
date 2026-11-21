package yesman.epicfight.registry.entries;

import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.item.enchantment.effects.EnchantmentAttributeEffect;
import yesman.epicfight.EpicFight;

import java.util.List;
import java.util.Optional;

public final class EpicFightEnchantments {
    private EpicFightEnchantments() {}

    public static final ResourceKey<Enchantment> LIGHTWEIGHT = key("lightweight");
    public static final ResourceKey<Enchantment> UNSHAKABLE = key("unshakable");
    public static final ResourceKey<Enchantment> IMPACTFUL = key("impactful");
    public static final ResourceKey<Enchantment> ARMOR_PIERCING = key("armor_piercing");

    public static void bootstrap(BootstrapContext<Enchantment> context) {
        var items = context.lookup(Registries.ITEM);
        HolderSet.Named<Item> armor = items.getOrThrow(ItemTags.ARMOR_ENCHANTABLE);

        context.register(LIGHTWEIGHT, Enchantment.enchantment(new Enchantment.EnchantmentDefinition(armor, Optional.empty(), 6, 10, Enchantment.constantCost(2), Enchantment.constantCost(45), 2, List.of(
                EquipmentSlotGroup.ARMOR
        ))).withEffect(EnchantmentEffectComponents.ATTRIBUTES, Effects.LIGHTWEIGHT).build(LIGHTWEIGHT.location()));
    }

    public static final class Effects {

        public static final EnchantmentAttributeEffect LIGHTWEIGHT = new EnchantmentAttributeEffect(EpicFightEnchantments.LIGHTWEIGHT.location(), EpicFightAttributes.WEIGHT, LevelBasedValue.perLevel(-0.1f, -0.1f), AttributeModifier.Operation.ADD_MULTIPLIED_BASE);

    }

    private static ResourceKey<Enchantment> key(String name)
    {
        return ResourceKey.create(Registries.ENCHANTMENT, EpicFight.identifier(name));
    }
}
