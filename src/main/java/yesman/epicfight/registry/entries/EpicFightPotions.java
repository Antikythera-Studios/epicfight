package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.alchemy.Potion;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.main.EpicFightMod;

public final class EpicFightPotions {
	private EpicFightPotions() {}
	
	public static final DeferredRegisterShim<Potion> REGISTRY = new DeferredRegisterShim<>(Registries.POTION, EpicFight.MODID);
	//public static final RegistryObject<Potion> BLOOMING = POTIONS.register("blooming", () -> new Potion(new MobEffectInstance(EpicFightMobEffects.BLOOMING.get(), 1200)));
	
	public static void addRecipes() {
		//BrewingRecipeRegistry.addRecipe(Ingredient.of(PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.REGENERATION)), Ingredient.of(Items.AMETHYST_SHARD), PotionUtils.setPotion(new ItemStack(Items.POTION), BLOOMING.get()));
	}
}