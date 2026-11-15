package yesman.epicfight.data.tags;
import yesman.epicfight.EpicFight;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.entries.EpicFightItems;

import java.util.concurrent.CompletableFuture;

public class EpicFightItemTagsProvider extends ItemTagsProvider {
	public EpicFightItemTagsProvider(
		PackOutput output,
        CompletableFuture<HolderLookup.Provider> lookupProvider,
        CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
        @org.jetbrains.annotations.Nullable yesman.epicfight.platform.neoforged.common.data.ExistingFileHelper existingFileHelper
	) {
		super(output, lookupProvider, blockTags);
	}
	
	@Override
	protected void addTags(Provider provider) {
		// For enchantability
		this.tag(ItemTags.SWORDS)
			.add(
				EpicFightItems.DIAMOND_DAGGER.get(),
				EpicFightItems.DIAMOND_GREATSWORD.get(),
				EpicFightItems.DIAMOND_LONGSWORD.get(),
				EpicFightItems.DIAMOND_SPEAR.get(),
				EpicFightItems.DIAMOND_TACHI.get(),
				EpicFightItems.GLOVE.get(),
				EpicFightItems.GOLDEN_DAGGER.get(),
				EpicFightItems.GOLDEN_GREATSWORD.get(),
				EpicFightItems.GOLDEN_LONGSWORD.get(),
				EpicFightItems.GOLDEN_SPEAR.get(),
				EpicFightItems.GOLDEN_TACHI.get(),
				EpicFightItems.IRON_DAGGER.get(),
				EpicFightItems.IRON_GREATSWORD.get(),
				EpicFightItems.IRON_LONGSWORD.get(),
				EpicFightItems.IRON_SPEAR.get(),
				EpicFightItems.IRON_TACHI.get(),
                EpicFightItems.NETHERITE_DAGGER.get(),
                EpicFightItems.NETHERITE_GREATSWORD.get(),
                EpicFightItems.NETHERITE_LONGSWORD.get(),
                EpicFightItems.NETHERITE_SPEAR.get(),
                EpicFightItems.NETHERITE_TACHI.get(),
				EpicFightItems.STONE_DAGGER.get(),
				EpicFightItems.STONE_GREATSWORD.get(),
				EpicFightItems.STONE_LONGSWORD.get(),
				EpicFightItems.STONE_SPEAR.get(),
				EpicFightItems.STONE_TACHI.get(),
                EpicFightItems.UCHIGATANA.get(),
				EpicFightItems.WOODEN_DAGGER.get(),
				EpicFightItems.WOODEN_GREATSWORD.get(),
				EpicFightItems.WOODEN_LONGSWORD.get(),
				EpicFightItems.WOODEN_SPEAR.get(),
				EpicFightItems.WOODEN_TACHI.get()
			);
	}
}
