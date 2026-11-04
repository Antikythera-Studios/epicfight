package yesman.epicfight.world.capabilities.provider;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;

import yesman.epicfight.registry.deferred.holders.DeferredPreset;
import yesman.epicfight.registry.entries.EpicFightItemCapabilityPresets;
import yesman.epicfight.world.capabilities.item.*;

public final class CommonItemCapabilityProvider {
	public static final CommonItemCapabilityProvider INSTANCE = new CommonItemCapabilityProvider();
	
	private CommonItemCapabilityProvider() {}
	
	private final Map<Class<? extends Item>, DeferredPreset<? extends CapabilityItem.Builder<?>>> typedCapabilities = new HashMap<> ();
	private final Map<Item, CapabilityItem> capabilities = new HashMap<> ();
	
	public void registerWeaponTypesByClass() {
		this.typedCapabilities.put(ArmorItem.class, EpicFightItemCapabilityPresets.ARMOR);
		this.typedCapabilities.put(ShieldItem.class, EpicFightItemCapabilityPresets.SHIELD);
        this.typedCapabilities.put(SwordItem.class, EpicFightItemCapabilityPresets.BOKKEN);
        this.typedCapabilities.put(PickaxeItem.class, EpicFightItemCapabilityPresets.PICKAXE);
        this.typedCapabilities.put(AxeItem.class, EpicFightItemCapabilityPresets.AXE);
        this.typedCapabilities.put(ShovelItem.class, EpicFightItemCapabilityPresets.SHOVEL);
        this.typedCapabilities.put(HoeItem.class, EpicFightItemCapabilityPresets.HOE);
        this.typedCapabilities.put(BowItem.class, EpicFightItemCapabilityPresets.BOW);
        this.typedCapabilities.put(CrossbowItem.class, EpicFightItemCapabilityPresets.CROSSBOW);
		this.typedCapabilities.put(MapItem.class, EpicFightItemCapabilityPresets.MAP);
	}
	
	public void put(Item item, CapabilityItem cap) {
		this.capabilities.put(item, cap);
	}
	
	public CapabilityItem get(Item item) {
		CapabilityItem capability = capabilities.get(item);
		if (capability != null) {
			return capability;
		}
		for (Class<?> clazz = item.getClass(); clazz != null; clazz = clazz.getSuperclass()) {
			if (this.typedCapabilities.containsKey(clazz)) {
				return getDefault(item, clazz);
			}
		}
		return null;
	}

	private CapabilityItem getDefault(Item item, Class<?> clazz)
	{
		DeferredPreset<? extends CapabilityItem.Builder<?>> builderEntry = this.typedCapabilities.getOrDefault(clazz, null);
		CapabilityItem.Builder<?> result = null;
		if (builderEntry != null)
		{
			if (builderEntry.value() instanceof WeaponCapability.Builder)
			{
				result = WeaponCapabilityPresets.registerPreset(builderEntry.value(), item);
			}
			else if (builderEntry.value() instanceof ArmorCapability.Builder builder)
			{
				result = builder.byItem(item);
			}
			else
			{
				result = builderEntry.value();
			}
		}

		return result != null ? result.build() : null;
	}
	
	public void clear() {
		this.capabilities.clear();
	}
	
	public void addDefaultItems() {
		BuiltInRegistries.ITEM.entrySet().stream().filter(entry -> !this.capabilities.containsKey(entry.getValue())).forEach(entry -> {
			Function<Item, ? extends CapabilityItem.Builder<?>> type = null;
			Item item = entry.getValue();

			if (item instanceof BlockItem) {
				return;
			}

			for (Map.Entry<ResourceLocation, ItemKeywordReloadListener.ItemRegex> regexEntry : ItemKeywordReloadListener.getRegexes().entrySet()) {
				if (regexEntry.getValue().matchesAny(entry.getKey().location().toString())) {
					type = WeaponTypeReloadListener.get(regexEntry.getKey());

					if (type != null) {
						this.capabilities.put(item, type.apply(item).build());
						break;
					}
				}
			}

			if (type == null) {
				Class<?> clazz = item.getClass();
				CapabilityItem capability = null;

				for (; clazz != null && capability == null; clazz = clazz.getSuperclass()) {
					if (this.typedCapabilities.containsKey(clazz)) {
						capability = getDefault(item, clazz);
					}
				}

				if (capability != null) {
					this.capabilities.put(item, capability);
				}
			}
		});
	}
	
	public @Nullable CapabilityItem getCapability(ItemStack itemstack, Void context) {
		if (this.capabilities.containsKey(itemstack.getItem())) {
			CapabilityItem itemCapability = this.capabilities.get(itemstack.getItem());
			
			if (itemCapability instanceof RuntimeCapability) {
				return itemCapability.findRecursive(itemstack);
			}
			
			return itemCapability;
		}
		
		return null;
	}
}