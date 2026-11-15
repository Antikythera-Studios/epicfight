package yesman.epicfight.platform.neoforged.client.extensions.common;

import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

import java.util.function.UnaryOperator;

/// Fabric-compatible equivalent of NeoForge's IClientItemExtensions.
/// Provides per-item client-side extensions for armor rendering, dye colors, etc.
/// The default implementation matches vanilla behavior.
public interface IClientItemExtensions {
	/// The default instance, matching vanilla behavior.
	IClientItemExtensions DEFAULT = new IClientItemExtensions() {
		@Override
		public int getDefaultDyeColor(ItemStack stack) {
			return -1;
		}

		@Override
		public int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor) {
			if (layer.dyeable()) {
				net.minecraft.world.item.component.DyedItemColor dyedColor = stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
				if (dyedColor != null) {
					return dyedColor.rgb();
				}
			}
			return fallbackColor;
		}

		@Override
		public UnaryOperator<Style> getDefaultStyleModifier(ItemStack stack) {
			return style -> style;
		}
	};

	/// Returns the default dye color for the given item stack.
	/// NeoForge mods can override this to provide custom default dye colors.
	/// Default: -1 (no custom dye).
	int getDefaultDyeColor(ItemStack stack);

	/// Returns the packed tint color for an armor layer.
	/// Default: checks DyedItemColor data component if the layer is dyeable.
	int getArmorLayerTintColor(ItemStack stack, LivingEntity entity, ArmorMaterial.Layer layer, int layerIdx, int fallbackColor);

	/// Returns the default style modifier for the item.
	/// Default: identity (no modification).
	UnaryOperator<Style> getDefaultStyleModifier(ItemStack stack);

	/// Gets the IClientItemExtensions for the given item stack.
	/// On NeoForge, this looks up a per-item registration.
	/// On Fabric, we always return DEFAULT since there's no equivalent registration system.
	static IClientItemExtensions of(ItemStack stack) {
		return DEFAULT;
	}
}
