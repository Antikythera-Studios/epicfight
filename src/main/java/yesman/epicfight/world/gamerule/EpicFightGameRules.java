package yesman.epicfight.world.gamerule;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import yesman.epicfight.platform.neoforged.common.ModConfigSpec;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeGamerule;
import yesman.epicfight.skill.SkillSlots;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class EpicFightGameRules {
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> GLOBAL_STUN = create(
			  "globalStun"
			, GameRules.Category.MOBS
			, configBuilder -> configBuilder.define("default_gamerule.globalStun", true)
			, RuleType.BOOLEAN
			, false
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> KEEP_SKILLS = create(
			  "keepSkills"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.define("default_gamerule.keepSkills", true)
			, RuleType.BOOLEAN
			, false
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> HAS_FALL_ANIMATION = create(
			  "hasFallAnimation"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.define("default_gamerule.hasFallAnimation", true)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> DISABLE_ENTITY_UI = create(
			  "disableEntityUI"
			, GameRules.Category.MISC
			, configBuilder -> configBuilder.define("default_gamerule.disapleEntityUI", false)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> CAN_SWITCH_PLAYER_MODE = create(
			  "canSwitchPlayerMode"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.define("default_gamerule.canSwitchPlayerMode", true)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> STIFF_COMBO_ATTACKS = create(
			  "stiffComboAttacks"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.define("default_gamerule.stiffComboAttacks", true)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> NO_MOBS_IN_BOSSFIGHT = create(
			  "noMobsInBossfight"
			, GameRules.Category.SPAWNING
			, configBuilder -> configBuilder.define("default_gamerule.noMobsInBossfight", true)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Integer, ModConfigSpec.IntValue, GameRules.IntegerValue> INITIAL_PLAYER_MODE = create(
			  "initialMode"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.comment("0 = vanilla, 1 = epicfight").defineInRange("default_gamerule.initialMode", 1, 0, 1)
			, RuleType.INTEGER
			, true
	);
	
	public static final ConfigurableGameRule<Integer, ModConfigSpec.IntValue, GameRules.IntegerValue> WEIGHT_PENALTY = EpicFightGameRules.create(
			  "weightPenalty"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.defineInRange("default_gamerule.weightPenalty", 100, 0, 100)
			, RuleType.INTEGER
			, true
	);
	
	public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> EPIC_DROP = EpicFightGameRules.create(
			  "epicDrop"
			, GameRules.Category.DROPS
			, configBuilder -> configBuilder.define("default_gamerule.epicDrop", false)
			, RuleType.BOOLEAN
			, true
	);
	
	public static final ConfigurableGameRule<Integer, ModConfigSpec.IntValue, GameRules.IntegerValue> SKILL_REPLACE_COOLDOWN = EpicFightGameRules.create(
			  "skillReplaceCooldown"
			, GameRules.Category.PLAYER
			, configBuilder -> configBuilder.defineInRange("default_gamerule.skillReplaceCooldown", 6000, 0, Integer.MAX_VALUE)
			, RuleType.INTEGER
			, true
	);

    public static final ConfigurableGameRule<Boolean, ModConfigSpec.BooleanValue, GameRules.BooleanValue> ALLOW_VANILLA_MELEE = EpicFightGameRules.create(
        "allowVanillaMelee"
        , GameRules.Category.PLAYER
        , configBuilder -> configBuilder.define("default_gamerule.allow_vanilla_melee", true)
        , RuleType.BOOLEAN
        , true
    );

    /// Maximum number of passive skills a player may have equipped at once. Default 3 preserves
    /// the legacy hard cap. Upper bound is [SkillSlots#CORE_PASSIVE_SLOT_COUNT] because that's how
    /// many passive `SkillSlots` enum constants this mod actually declares; raising past that has
    /// no effect.
    public static final ConfigurableGameRule<Integer, ModConfigSpec.IntValue, GameRules.IntegerValue> MAX_PASSIVE_SKILLS = EpicFightGameRules.create(
          "maxPassiveSkills"
        , GameRules.Category.PLAYER
        , configBuilder -> configBuilder.defineInRange("default_gamerule.maxPassiveSkills", 3, 0, SkillSlots.CORE_PASSIVE_SLOT_COUNT)
        , RuleType.INTEGER
        , true
    );

	public static final Map<String, ConfigurableGameRule<?, ?, ?>> GAME_RULES = ImmutableMap.<String, ConfigurableGameRule<?, ?, ?>>builder()
			.put("globalStun", GLOBAL_STUN)
			.put("keepSkills", KEEP_SKILLS)
			.put("hasFallAnimation", HAS_FALL_ANIMATION)
			.put("disableEntityUI", DISABLE_ENTITY_UI)
			.put("canSwitchPlayerMode", CAN_SWITCH_PLAYER_MODE)
			.put("stiffComboAttacks", STIFF_COMBO_ATTACKS)
			.put("noMobsInBossfight", NO_MOBS_IN_BOSSFIGHT)
			.put("initialMode", INITIAL_PLAYER_MODE)
			.put("weightPenalty", WEIGHT_PENALTY)
			.put("epicDrop", EPIC_DROP)
			.put("skillReplaceCooldown", SKILL_REPLACE_COOLDOWN)
            .put("allowVanillaMelee", ALLOW_VANILLA_MELEE)
            .put("maxPassiveSkills", MAX_PASSIVE_SKILLS)
			.build();
	
	public static void registerGameRules() {
		GAME_RULES.values().forEach(gamerule -> gamerule.registerGameRule());
	}
	
	public static <Type, Config extends ModConfigSpec.ConfigValue<Type>, RuleValue extends GameRules.Value<RuleValue>> ConfigurableGameRule<Type, Config, RuleValue> create(
		  String ruleName
		, GameRules.Category ruleCategory
		, Function<ModConfigSpec.Builder, Config> configDefinition
		, RuleType<Type, RuleValue> ruleType
		, boolean synchronize
	) {
		return new ConfigurableGameRule<> (ruleName, ruleCategory, configDefinition, ruleType, synchronize);
	}
	
	public static class ConfigurableGameRule<Type, Config extends ModConfigSpec.ConfigValue<Type>, RuleValue extends GameRules.Value<RuleValue>> {
		final String ruleName;
		final GameRules.Category ruleCategory;
		final Function<ModConfigSpec.Builder, Config> configDefinition;
		final RuleType<Type, RuleValue> ruleType; 
		final boolean synchronize;
		
		Config configValueHolder;
		GameRules.Key<RuleValue> gameRuleKey;
		
		private ConfigurableGameRule(
			  String ruleName
			, GameRules.Category ruleCategory
			, Function<ModConfigSpec.Builder, Config> configDefinition
			, RuleType<Type, RuleValue> ruleType
			, boolean synchronize
		) {
			this.ruleName = ruleName;
			this.ruleCategory = ruleCategory;
			this.configDefinition = configDefinition;
			this.ruleType = ruleType;
			this.synchronize = synchronize;
		}
		
		@SuppressWarnings("unchecked")
		public void registerGameRule() {
			if (this.synchronize) {
				this.gameRuleKey = GameRules.register(
						  this.ruleName
						, this.ruleCategory
						, this.ruleType.valueCreator.apply(
							    this.configValueHolder.get()
							  , (server, value) -> EpicFightNetworkManager.sendToAll(server, new SPChangeGamerule(new EpicFightGameRules.KeyValuePair((ConfigurableGameRule<Object, ?, ?>)this, this.ruleType.getRule.apply(value))))
						  )
				);
			} else {
				this.gameRuleKey = GameRules.register(
						  this.ruleName
						, this.ruleCategory
						, this.ruleType.valueCreatorUnsynchronized.apply(this.configValueHolder.get())
				);
			}
		}
		
		public boolean shouldSync() {
			return this.synchronize;
		}
		
		@SuppressWarnings("unchecked")
		public SPChangeGamerule getSyncPacket(ServerPlayer player) {
			return new SPChangeGamerule(new EpicFightGameRules.KeyValuePair((ConfigurableGameRule<Object, ?, ?>)this, this.getRuleValue(player.level())));
		}
		
		public void defineConfig(ModConfigSpec.Builder configBuilder) {
			this.configValueHolder = this.configDefinition.apply(configBuilder);
		}
		
		public String getRuleName() {
			return this.ruleName;
		}
		
		public RuleType<Type, RuleValue> getRuleType() {
			return this.ruleType;
		}
		
		public GameRules.Key<RuleValue> getRuleKey() {
			return this.gameRuleKey;
		}
		
		public Config getConfigHolder() {
			return this.configValueHolder;
		}
		
		public Type getRuleValue(Level level) {
			return this.ruleType.getRule.apply(level.getGameRules().getRule(this.gameRuleKey));
		}
		
		public void setRuleValue(Level level, Type value) {
			this.ruleType.setRule.accept(level.getGameRules().getRule(this.gameRuleKey), value);
		}
	}
	
	public static record RuleType<Type, RuleValue extends GameRules.Value<RuleValue>> (
		  BiFunction<Type, BiConsumer<MinecraftServer, RuleValue>, GameRules.Type<RuleValue>> valueCreator
		, Function<Type, GameRules.Type<RuleValue>> valueCreatorUnsynchronized
		, Function<RuleValue, Type> getRule
		, BiConsumer<RuleValue, Type> setRule
		, StreamCodec<ByteBuf, Type> codec
	) {
		private static final RuleType<Boolean, GameRules.BooleanValue> BOOLEAN = new RuleType<> (
			 GameRules.BooleanValue::create
		   , GameRules.BooleanValue::create
		   , GameRules.BooleanValue::get
		   , (ruleValue, value) -> ruleValue.set(value, null)
		   , ByteBufCodecs.BOOL
		);

		private static final RuleType<Integer, GameRules.IntegerValue> INTEGER = new RuleType<> (
			 GameRules.IntegerValue::create
		   , GameRules.IntegerValue::create
		   , GameRules.IntegerValue::get
		   , (ruleValue, value) -> ruleValue.tryDeserialize(value.toString())
		   , ByteBufCodecs.INT
		);
	}
	
	public static record KeyValuePair(ConfigurableGameRule<Object, ?, ?> gamerule, Object value) {
	}
}