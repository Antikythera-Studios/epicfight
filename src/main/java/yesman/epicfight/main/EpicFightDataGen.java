package yesman.epicfight.main;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.entries.EpicFightEnchantments;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class EpicFightDataGen extends DatapackBuiltinEntriesProvider {
    public EpicFightDataGen(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, new RegistrySetBuilder().add(Registries.ENCHANTMENT, EpicFightEnchantments::bootstrap), Set.of(EpicFight.MODID));
    }
}
