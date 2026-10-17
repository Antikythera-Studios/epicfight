package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.utils.ByteBufCodecsExtends;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.SkillDataKey;
import yesman.epicfight.skill.common.ComboAttacks;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.guard.ImpactGuardSkill;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.skill.identity.MeteorSlamSkill;
import yesman.epicfight.skill.identity.RevelationSkill;
import yesman.epicfight.skill.mover.DemolitionLeapSkill;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.skill.passive.AdaptiveSkinSkill;
import yesman.epicfight.skill.passive.AdrenalineFiendSkill;
import yesman.epicfight.skill.passive.BonebreakerSkill;
import yesman.epicfight.skill.passive.VengeanceSkill;
import yesman.epicfight.skill.weapon_passive.BattojutsuPassive;
import yesman.epicfight.skill.weaponinnate.BattojutsuSkill;
import yesman.epicfight.skill.weaponinnate.BladeRushSkill;
import yesman.epicfight.skill.weaponinnate.EverlastingAllegiance;
import yesman.epicfight.skill.weaponinnate.GraspingSpireSkill;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;

public final class EpicFightSkillDataKeys {
	private EpicFightSkillDataKeys() {}
	
	public static final DeferredRegisterShim<SkillDataKey<?>> REGISTRY = new DeferredRegisterShim<>(EpicFightRegistries.Keys.SKILL_DATA_KEY, EpicFight.MODID);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> COMBO_COUNTER = REGISTRY.register("combo_counter", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, ComboAttacks.class, BladeRushSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Boolean>> SHEATH = REGISTRY.register("sheath", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.BOOL, false, false, BattojutsuPassive.class, BattojutsuSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> PENALTY_RESTORE_COUNTER = REGISTRY.register("penalty_restore_counter", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, GuardSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Float>> PENALTY = REGISTRY.register("penalty", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.FLOAT, 0.0F, false, GuardSkill.class, ImpactGuardSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> LAST_ACTIVE = REGISTRY.register("last_active", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, ParryingSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> PARRY_MOTION_COUNTER = REGISTRY.register("parry_motion_counter", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, ParryingSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Float>> FALL_DISTANCE = REGISTRY.register("fall_distance", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.FLOAT, 0.0F, true, MeteorSlamSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Boolean>> PROTECT_NEXT_FALL = REGISTRY.register("slam_protect_next_fall", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.BOOL, false, false, MeteorSlamSkill.class, DemolitionLeapSkill.class, PhantomAscentSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> STACKS = REGISTRY.register("stacks", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, AdaptiveSkinSkill.class, BonebreakerSkill.class, RevelationSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Boolean>> JUMP_KEY_PRESSED_LAST_TICK = REGISTRY.register("jump_key_pressed_last_tick", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.BOOL, false, false, PhantomAscentSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> JUMP_COUNT = REGISTRY.register("jump_count", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, PhantomAscentSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> THROWN_TRIDENT_ENTITY_ID = REGISTRY.register("thrown_trident_entity_id", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, -1, false, EverlastingAllegiance.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> LAST_HIT_COUNT = REGISTRY.register("last_hit_count", () -> 
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, GraspingSpireSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<TagKey<DamageType>>> RESISTING_DAMAGE_TYPE = REGISTRY.register("resisting_damage_type", () ->
		SkillDataKey.createSkillDataKey(ByteBufCodecsExtends.tagKey(Registries.DAMAGE_TYPE), EpicFightDamageTypeTags.NONE, true, AdaptiveSkinSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> TICK_RECORD = REGISTRY.register("tick_record", () ->
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, 0, false, AdaptiveSkinSkill.class, AdrenalineFiendSkill.class, VengeanceSkill.class)
	);
	
	public static final DeferredHolderShim<SkillDataKey<?>, SkillDataKey<Integer>> ENTITY_ID = REGISTRY.register("entity_id", () ->
		SkillDataKey.createSkillDataKey(ByteBufCodecs.INT, -1, false, BonebreakerSkill.class, VengeanceSkill.class)
	);
}