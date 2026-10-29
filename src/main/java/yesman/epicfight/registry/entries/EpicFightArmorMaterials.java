package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import java.util.EnumMap;
import java.util.List;

import net.minecraft.Util;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;

public final class EpicFightArmorMaterials {
	private EpicFightArmorMaterials() {}
	
	public static final DeferredRegisterShim<ArmorMaterial> REGISTRY = new DeferredRegisterShim<>(Registries.ARMOR_MATERIAL, EpicFight.MODID);
	
	public static final DeferredHolderShim<ArmorMaterial, ArmorMaterial> STRAY_CLOTH =
		REGISTRY.register(
			  "stray_cloth"
			, () ->
				new ArmorMaterial(
					Util.make(
						new EnumMap<>(ArmorItem.Type.class),
						enumMap -> {
					        enumMap.put(ArmorItem.Type.BOOTS, 1);
					        enumMap.put(ArmorItem.Type.LEGGINGS, 2);
					        enumMap.put(ArmorItem.Type.CHESTPLATE, 3);
					        enumMap.put(ArmorItem.Type.HELMET, 1);
					        enumMap.put(ArmorItem.Type.BODY, 2);
						}
					)
					, 15
					, SoundEvents.ARMOR_EQUIP_LEATHER
					, () -> Ingredient.of(Items.STRING)
					, List.of(new ArmorMaterial.Layer(EpicFight.identifier("stray_cloth")))
					, 0.0F
					, 0.0F
				)
		);
}