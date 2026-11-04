package yesman.epicfight.api.event.types.registry;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import yesman.epicfight.api.event.Event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RegisterMobSkillBookLootTableEvent extends Event {
	private final Map<EntityType<?>, LootTable.Builder> builders;
	/// Fabric-only: tracks LootPool.Builder objects per entity type so they can be
	/// injected into entity loot tables via LootTableEvents.MODIFY (which requires
	/// LootPool.Builder, not built LootTable).
	private final Map<EntityType<?>, List<LootPool.Builder>> poolBuilders = new HashMap<> ();

	public RegisterMobSkillBookLootTableEvent(Map<EntityType<?>, LootTable.Builder> builders) {
		this.builders = builders;
	}

	public LootTable.Builder get(EntityType<?> entityType) {
		return this.builders.get(entityType);
	}

	public RegisterMobSkillBookLootTableEvent put(EntityType<?> entityType, LootTable.Builder builder) {
		this.builders.put(entityType, builder);
		return this;
	}

	public RegisterMobSkillBookLootTableEvent add(EntityType<?> entityType, LootPool.Builder builder) {
		this.builders.computeIfAbsent(entityType, (k) -> LootTable.lootTable()).withPool(builder);
		// Also track the pool builder for Fabric loot table injection
		this.poolBuilders.computeIfAbsent(entityType, (k) -> new ArrayList<>()).add(builder);
		return this;
	}

	/// Fabric-only: returns the list of LootPool.Builder objects for the given entity type.
	/// Used by OnSkillBookDroppedByEntity to inject pools via LootTableEvents.MODIFY.
	public List<LootPool.Builder> getPoolBuilders(EntityType<?> entityType) {
		return this.poolBuilders.get(entityType);
	}

	/// Fabric-only: returns the map of all LootPool.Builder objects per entity type.
	public Map<EntityType<?>, List<LootPool.Builder>> getAllPoolBuilders() {
		return this.poolBuilders;
	}
}
