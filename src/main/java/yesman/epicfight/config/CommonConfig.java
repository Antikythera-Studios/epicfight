package yesman.epicfight.config;

import yesman.epicfight.platform.neoforged.fml.config.ModConfig;

import yesman.epicfight.platform.neoforged.common.ModConfigSpec;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.world.gamerule.EpicFightGameRules;


public class CommonConfig {
	private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
	public static final ModConfigSpec.IntValue SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER = BUILDER.defineInRange("loot.skill_book_mob_drop_chance_modifier", 0, -100, 100);
	public static final ModConfigSpec.IntValue SKILL_BOOK_CHEST_LOOT_MODIFIER = BUILDER.defineInRange("loot.skill_book_chest_drop_chance_modifier", 0, -100, 100);
	public static final ModConfigSpec SPEC;
	
	public static int skillBookMobDropChanceModifier;
	public static int skillBookChestLootModifier;
	
	static {
		EpicFightGameRules.GAME_RULES.values().forEach(configurableGameRule -> configurableGameRule.defineConfig(BUILDER));
		SPEC = BUILDER.build();
	}
	
	
    public static void onLoad(final ModConfig config) {
		if (config.getType() != ModConfig.Type.COMMON) {
			return;
		}

		skillBookMobDropChanceModifier = SKILL_BOOK_MOB_DROP_CHANCE_MODIFIER.get();
		skillBookChestLootModifier = SKILL_BOOK_CHEST_LOOT_MODIFIER.get();
	}
}
