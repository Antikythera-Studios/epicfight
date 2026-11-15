package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.data.loot.function.SetSkillFunction;
import yesman.epicfight.main.EpicFightMod;

public final class EpicFightLootItemFunctions {
	private EpicFightLootItemFunctions() {}
	
	public static final DeferredRegisterShim<LootItemFunctionType<?>> REGISTRY = new DeferredRegisterShim<>(Registries.LOOT_FUNCTION_TYPE, EpicFight.MODID);
	
	public static final DeferredHolderShim<LootItemFunctionType<?>, LootItemFunctionType<SetSkillFunction>> SKILLS = REGISTRY.register("skillbook_loot_table_modifier", () -> 
		new LootItemFunctionType<> (SetSkillFunction.CODEC)
	);
}
