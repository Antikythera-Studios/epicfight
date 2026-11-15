package yesman.epicfight.data.loot;

import com.google.common.collect.Maps;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;

import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.registry.RegisterMobSkillBookLootTableEvent;

import java.util.List;
import java.util.Map;

/// Fabric replacement for NeoForge's OnSkillBookDroppedByEntity global loot modifier.
///
/// Instead of using a LootModifier, we register entity-specific loot pool builders via
/// RegisterMobSkillBookLootTableEvent and then inject them into the corresponding
/// entity loot tables via LootTableEvents.MODIFY in EpicFightLootTables.
public class OnSkillBookDroppedByEntity {
	/// Stores loot pool builders per entity type. We keep builders (not built pools)
	/// because LootTable.Builder.withPool() requires LootPool.Builder, and built
	/// LootPool objects cannot be reused across multiple loot table builds.
	private static final Map<EntityType<?>, List<LootPool.Builder>> SKILL_LOOT_POOLS = Maps.newHashMap();

	public static void registerEntitySkillLootTable() {
		// The event is created, posted (populating it via createSkillLootTable callback),
		// and then the pool builders are extracted from the event.
		Map<EntityType<?>, net.minecraft.world.level.storage.loot.LootTable.Builder> builders = new java.util.HashMap<> ();
        RegisterMobSkillBookLootTableEvent skillBookLootTableRegistryEvent = new RegisterMobSkillBookLootTableEvent(builders);
        EpicFightEventHooks.Registry.SKILLBOOK_LOOT_TABLE.post(skillBookLootTableRegistryEvent);
		SKILL_LOOT_POOLS.clear();

		// Extract pool builders from the event (Fabric-only field)
		skillBookLootTableRegistryEvent.getAllPoolBuilders().forEach((k, v) -> {
			SKILL_LOOT_POOLS.put(k, v);
		});
	}

	/// Returns the list of loot pool builders for the given entity type, or null if none exists.
	public static List<LootPool.Builder> getSkillLootPools(EntityType<?> entityType) {
		return SKILL_LOOT_POOLS.get(entityType);
	}

	/// Returns the map of all registered entity skill loot pool builders.
	public static Map<EntityType<?>, List<LootPool.Builder>> getSkillLootPools() {
		return SKILL_LOOT_POOLS;
	}
}
