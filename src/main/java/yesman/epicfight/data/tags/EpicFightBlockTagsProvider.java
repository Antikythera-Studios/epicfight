package yesman.epicfight.data.tags;
import yesman.epicfight.platform.neoforged.common.data.BlockTagsProvider;
import yesman.epicfight.platform.neoforged.common.data.ExistingFileHelper;
import yesman.epicfight.EpicFight;

import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.data.PackOutput;


import yesman.epicfight.main.EpicFightMod;

public class EpicFightBlockTagsProvider extends BlockTagsProvider {
	public EpicFightBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, EpicFight.MODID, existingFileHelper);
    }
	
	protected void addTags(Provider provider) {
	}
}
