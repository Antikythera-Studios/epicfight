package yesman.epicfight.registry.entries;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;

public final class EpicFightCreativeTabs {
	private EpicFightCreativeTabs() {}

	public static final DeferredRegisterShim<CreativeModeTab> REGISTRY = new DeferredRegisterShim<>(Registries.CREATIVE_MODE_TAB, EpicFight.MODID);

	public static final DeferredHolderShim<CreativeModeTab, CreativeModeTab> ITEMS = REGISTRY.register("items", () ->
		CreativeModeTab.builder(CreativeModeTab.Row.TOP, 6)
			.title(Component.translatable("itemGroup.epicfight.items"))
			.icon(() -> new ItemStack(EpicFightItems.SKILLBOOK.get()))
			.backgroundTexture(EpicFight.identifier("textures/gui/container/epicfight_creative_tab.png"))
			.hideTitle()
			.displayItems((params, output) -> {
				EpicFightItems.REGISTRY.getEntries().forEach(item -> {
					// FIXME: bad implement, maybe based protocol better yet.
					// ignore UCHIGATANA_SHEATH
					if (item == EpicFightItems.UCHIGATANA_SHEATH || item == EpicFightItems.SKILLBOOK) {
						return;
					}

					output.accept(item.get());
				});
			})
			.build()
		);
}
