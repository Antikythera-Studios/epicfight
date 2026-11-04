package yesman.epicfight.data.loot;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import yesman.epicfight.api.event.types.registry.RegisterMobSkillBookLootTableEvent;
import yesman.epicfight.config.CommonConfig;
import yesman.epicfight.data.loot.function.SetSkillFunction;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.registry.entries.EpicFightSkills;

public class EpicFightLootTables {

	public static void registerLootTableEvents() {
		// Register entity skill book loot tables first
		OnSkillBookDroppedByEntity.registerEntitySkillLootTable();

		LootTableEvents.MODIFY.register((key, tableBuilder, source, registries) -> {
			if (source.isBuiltin()) {
				ResourceLocation name = key.location();
				modifyBuiltinLootTable(name, tableBuilder);
				modifyEntityLootTable(name, tableBuilder);
			}
		});
	}

	/// Injects skill book drops into entity loot tables.
	/// Entity loot tables have keys like "minecraft:entities/zombie".
	private static void modifyEntityLootTable(ResourceLocation name, LootTable.Builder tableBuilder) {
		if (!name.getNamespace().equals("minecraft") || !name.getPath().startsWith("entities/")) {
			return;
		}

		String entityTypeName = name.getPath().substring("entities/".length());
		// Find the entity type that matches this loot table
		for (var entry : OnSkillBookDroppedByEntity.getSkillLootPools().entrySet()) {
			EntityType<?> entityType = entry.getKey();
			ResourceLocation entityKey = EntityType.getKey(entityType);
			if (entityKey.getPath().equals(entityTypeName)) {
				// Add all pool builders from the skill loot table to the entity's loot table
				for (var poolBuilder : entry.getValue()) {
					tableBuilder.withPool(poolBuilder);
				}
				break;
			}
		}
	}

	private static void modifyBuiltinLootTable(ResourceLocation name, LootTable.Builder tableBuilder) {
		int modifier = CommonConfig.skillBookChestLootModifier;
		int dropChance = 100 + modifier;
		int antiDropChance = 100 - modifier;
		float dropChanceModifier = dropChance / (float)(antiDropChance + dropChance);

		if (name.equals(BuiltInLootTables.DESERT_PYRAMID.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 2.0F))
				.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
					EpicFightSkills.BERSERKER,
					EpicFightSkills.STAMINA_PILLAGER,
					EpicFightSkills.TECHNICIAN,
					EpicFightSkills.SWORD_MASTER,
					EpicFightSkills.GUARD,
					EpicFightSkills.STEP,
					EpicFightSkills.ROLL,
					EpicFightSkills.PHANTOM_ASCENT
				)).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier)))
			);

			tableBuilder.withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(0.25F))
				.add(LootItem.lootTableItem(EpicFightItems.UCHIGATANA.get()))
			);
		}

		if (name.equals(BuiltInLootTables.JUNGLE_TEMPLE.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 2.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL,
				EpicFightSkills.PHANTOM_ASCENT
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier))
        	);

			tableBuilder.withPool(LootPool.lootPool().when(LootItemRandomChanceCondition.randomChance(0.25F))
				.add(LootItem.lootTableItem(EpicFightItems.UCHIGATANA.get()))
			);
		}

		if (name.equals(BuiltInLootTables.SIMPLE_DUNGEON.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
        	);
		}

		if (name.equals(BuiltInLootTables.ABANDONED_MINESHAFT.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
        	);
		}

		if (name.equals(BuiltInLootTables.PILLAGER_OUTPOST.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
        	);
		}

		if (name.equals(BuiltInLootTables.UNDERWATER_RUIN_BIG.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 3.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL,
				EpicFightSkills.PHANTOM_ASCENT
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
        	);
		}

		if (name.equals(BuiltInLootTables.SHIPWRECK_MAP.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 2.0F))
        		.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL
        		))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
        	);
		}

		if (name.equals(BuiltInLootTables.STRONGHOLD_LIBRARY.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 5.0F))
				.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.HYPERVITALITY,
				EpicFightSkills.FORBIDDEN_STRENGTH,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL,
				EpicFightSkills.PHANTOM_ASCENT
				))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
			);
		}

		if (name.equals(BuiltInLootTables.WOODLAND_MANSION.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 5.0F))
				.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.HYPERVITALITY,
				EpicFightSkills.FORBIDDEN_STRENGTH,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL,
				EpicFightSkills.PHANTOM_ASCENT
				))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
			);
		}

		if (name.equals(BuiltInLootTables.BASTION_OTHER.location())) {
			tableBuilder.withPool(LootPool.lootPool().setRolls(UniformGenerator.between(1.0F, 4.0F))
				.add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
				EpicFightSkills.BERSERKER,
				EpicFightSkills.STAMINA_PILLAGER,
				EpicFightSkills.TECHNICIAN,
				EpicFightSkills.SWORD_MASTER,
				EpicFightSkills.HYPERVITALITY,
				EpicFightSkills.FORBIDDEN_STRENGTH,
				EpicFightSkills.GUARD,
				EpicFightSkills.STEP,
				EpicFightSkills.ROLL,
				EpicFightSkills.PHANTOM_ASCENT
				))).when(LootItemRandomChanceCondition.randomChance(dropChanceModifier * 0.3F))
			);
		}
	}

    /// Registers entity skill book drop loot tables. Called when the SKILLBOOK_LOOT_TABLE
    /// event fires. Uses event.add() with LootPool.Builder so pool builders are tracked
    /// for Fabric loot table injection.
    public static void createSkillLootTable(RegisterMobSkillBookLootTableEvent event) {
        int modifier = CommonConfig.skillBookMobDropChanceModifier;
        int dropChance = 100 + modifier;
        int antiDropChance = 100 - modifier;
        float dropChanceModifier = antiDropChance == 0 ? Float.MAX_VALUE : dropChance / (float) antiDropChance;

        event.add(EntityType.ZOMBIE, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.BERSERKER,
                1.0F, EpicFightSkills.STAMINA_PILLAGER,
                1.0F, EpicFightSkills.ROLL,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.GUARD,
                0.5F, EpicFightSkills.ENDURANCE
            )))
        );

        event.add(EntityType.HUSK, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.BERSERKER,
                1.0F, EpicFightSkills.STAMINA_PILLAGER,
                1.0F, EpicFightSkills.ROLL,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.GUARD,
                0.5F, EpicFightSkills.ENDURANCE
            )))
        );

        event.add(EntityType.DROWNED, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.BERSERKER,
                1.0F, EpicFightSkills.STAMINA_PILLAGER,
                1.0F, EpicFightSkills.ROLL,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.GUARD,
                0.5F, EpicFightSkills.ENDURANCE
            )))
        );

        event.add(EntityType.SKELETON, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.SWORD_MASTER,
                1.0F, EpicFightSkills.TECHNICIAN,
                1.0F, EpicFightSkills.ROLL,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.GUARD,
                0.5F, EpicFightSkills.EMERGENCY_ESCAPE
            )))
        );

        event.add(EntityType.STRAY, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.SWORD_MASTER,
                1.0F, EpicFightSkills.TECHNICIAN,
                1.0F, EpicFightSkills.ROLL,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.GUARD,
                0.5F, EpicFightSkills.EMERGENCY_ESCAPE
            )))
        );

        event.add(EntityType.SPIDER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.ROLL,
                EpicFightSkills.STEP,
                EpicFightSkills.GUARD
            )))
        );

        event.add(EntityType.CAVE_SPIDER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.ROLL,
                EpicFightSkills.STEP,
                EpicFightSkills.GUARD
            )))
        );

        event.add(EntityType.CREEPER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.HYPERVITALITY,
                EpicFightSkills.IMPACT_GUARD
            )))
        );

        event.add(EntityType.ENDERMAN, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.HYPERVITALITY,
                EpicFightSkills.FORBIDDEN_STRENGTH,
                EpicFightSkills.ENDURANCE,
                EpicFightSkills.EMERGENCY_ESCAPE,
                EpicFightSkills.PARRYING,
                EpicFightSkills.IMPACT_GUARD
            )))
        );

        event.add(EntityType.VINDICATOR, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.HYPERVITALITY,
                EpicFightSkills.BERSERKER,
                EpicFightSkills.GUARD,
                EpicFightSkills.STEP,
                EpicFightSkills.ROLL
            )))
        );

        event.add(EntityType.PILLAGER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.HYPERVITALITY,
                EpicFightSkills.STAMINA_PILLAGER,
                EpicFightSkills.GUARD,
                EpicFightSkills.STEP,
                EpicFightSkills.ROLL
            )))
        );

        event.add(EntityType.WITCH, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.FORBIDDEN_STRENGTH,
                EpicFightSkills.BERSERKER
            )))
        );

        event.add(EntityType.EVOKER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.PARRYING,
                EpicFightSkills.IMPACT_GUARD
            )))
        );

        event.add(EntityType.EVOKER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.1F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.DEATH_HARVEST,
                EpicFightSkills.EMERGENCY_ESCAPE
            )))
        );

        event.add(EntityType.PIGLIN, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.SWORD_MASTER,
                EpicFightSkills.STAMINA_PILLAGER,
                EpicFightSkills.GUARD,
                EpicFightSkills.STEP,
                EpicFightSkills.ROLL
            )))
        );

        event.add(EntityType.PIGLIN_BRUTE, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.HYPERVITALITY,
                EpicFightSkills.PARRYING,
                EpicFightSkills.ENDURANCE,
                EpicFightSkills.IMPACT_GUARD
            )))
        );

        event.add(EntityType.ZOMBIFIED_PIGLIN, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.BERSERKER,
                EpicFightSkills.STAMINA_PILLAGER,
                EpicFightSkills.GUARD,
                EpicFightSkills.STEP,
                EpicFightSkills.ROLL
            )))
        );

        event.add(EntityType.WITHER_SKELETON, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F)).when(LootItemRandomChanceCondition.randomChance(0.025F * dropChanceModifier))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                1.0F, EpicFightSkills.SWORD_MASTER,
                1.0F, EpicFightSkills.STAMINA_PILLAGER,
                1.0F, EpicFightSkills.GUARD,
                1.0F, EpicFightSkills.STEP,
                1.0F, EpicFightSkills.ROLL,
                0.75F, EpicFightSkills.DEATH_HARVEST
            )))
        );

        event.add(EntityType.WITHER, LootPool.lootPool()
            .setRolls(ConstantValue.exactly(1.0F))
            .add(LootItem.lootTableItem(EpicFightItems.SKILLBOOK.get()).apply(SetSkillFunction.builder(
                EpicFightSkills.DEATH_HARVEST
            )))
        );
    }
}
