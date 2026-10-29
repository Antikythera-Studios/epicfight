package yesman.epicfight.registry.entries;
import yesman.epicfight.EpicFight;

import net.minecraft.tags.DamageTypeTags;
import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.registry.deferred_shim.DeferredRegisterShim;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.Skill.ActivateType;
import yesman.epicfight.skill.Skill.Resource;
import yesman.epicfight.skill.SkillCategories;
import yesman.epicfight.skill.common.ComboAttacks;
import yesman.epicfight.skill.dodge.DodgeSkill;
import yesman.epicfight.skill.dodge.KnockdownWakeupSkill;
import yesman.epicfight.skill.guard.GuardSkill;
import yesman.epicfight.skill.guard.ImpactGuardSkill;
import yesman.epicfight.skill.guard.ParryingSkill;
import yesman.epicfight.skill.identity.MeteorSlamSkill;
import yesman.epicfight.skill.identity.RevelationSkill;
import yesman.epicfight.skill.mover.DemolitionLeapSkill;
import yesman.epicfight.skill.mover.PhantomAscentSkill;
import yesman.epicfight.skill.passive.*;
import yesman.epicfight.skill.weapon_passive.BattojutsuPassive;
import yesman.epicfight.skill.weaponinnate.*;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.ExtraDamageInstance;
import yesman.epicfight.world.damagesource.StunType;

import java.util.Set;

public final class EpicFightSkills {
    private EpicFightSkills() {}

    public static final DeferredRegisterShim<Skill> REGISTRY = new DeferredRegisterShim<>(EpicFightRegistries.Keys.SKILL, EpicFight.MODID);

    public static final DeferredHolderShim<Skill, Skill> EMPTY = REGISTRY.register("empty", key ->
        Skill.EMPTY
    );

    public static final DeferredHolderShim<Skill, ComboAttacks> COMBO_ATTACKS = REGISTRY.register("combo_attacks", key ->
        ComboAttacks.createComboAttackBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, DodgeSkill> ROLL = REGISTRY.register("roll", key ->
        DodgeSkill.createDodgeBuilder(DodgeSkill::new)
            .setAnimations(Animations.BIPED_ROLL_FORWARD, Animations.BIPED_ROLL_BACKWARD)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, DodgeSkill> STEP = REGISTRY.register("step", key ->
        DodgeSkill.createDodgeBuilder(DodgeSkill::new)
            .setAnimations(Animations.BIPED_STEP_FORWARD, Animations.BIPED_STEP_BACKWARD, Animations.BIPED_STEP_LEFT, Animations.BIPED_STEP_RIGHT)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, KnockdownWakeupSkill> KNOCKDOWN_WAKEUP = REGISTRY.register("knockdown_wakeup", key ->
        DodgeSkill.createDodgeBuilder(KnockdownWakeupSkill::new)
            .setAnimations(Animations.BIPED_KNOCKDOWN_WAKEUP_LEFT, Animations.BIPED_KNOCKDOWN_WAKEUP_RIGHT)
            .setCategory(SkillCategories.KNOCKDOWN_WAKEUP)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, GuardSkill> GUARD = REGISTRY.register("guard", key ->
        GuardSkill.createGuardBuilder(GuardSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, ImpactGuardSkill> IMPACT_GUARD = REGISTRY.register("impact_guard", key ->
        ImpactGuardSkill.createImpactGuardBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, ParryingSkill> PARRYING = REGISTRY.register("parrying", key ->
        ParryingSkill.createActiveGuardBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, AdaptiveSkinSkill> ADAPTIVE_SKIN = REGISTRY.register("adaptive_skin", key ->
        AdaptiveSkinSkill.createAdaptiveSkinBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, AdrenalineFiendSkill> ADRENALINE_FIEND = REGISTRY.register("adrenaline_fiend", key ->
        PassiveSkill.createPassiveBuilder(AdrenalineFiendSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, BerserkerSkill> BERSERKER = REGISTRY.register("berserker", key ->
        PassiveSkill.createPassiveBuilder(BerserkerSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, BonebreakerSkill> BONEBREAKER = REGISTRY.register("bonebreaker", key ->
        PassiveSkill.createPassiveBuilder(BonebreakerSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, CatharsisSkill> CATHARSIS = REGISTRY.register("catharsis", key ->
        PassiveSkill.createPassiveBuilder(CatharsisSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, DeathHarvestSkill> DEATH_HARVEST = REGISTRY.register("death_harvest", key ->
        PassiveSkill.createPassiveBuilder(DeathHarvestSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, EmergencyEscapeSkill> EMERGENCY_ESCAPE = REGISTRY.register("emergency_escape", key ->
        EmergencyEscapeSkill.createEmergencyEscapeBuilder()
            .addAvailableWeaponCategory(WeaponCategories.SWORD, WeaponCategories.UCHIGATANA, WeaponCategories.DAGGER)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, EnduranceSkill> ENDURANCE = REGISTRY.register("endurance", key ->
        PassiveSkill.createPassiveBuilder(EnduranceSkill::new)
            .setResource(Resource.COOLDOWN)
            .setActivateType(ActivateType.DURATION)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, ForbiddenStrengthSkill> FORBIDDEN_STRENGTH = REGISTRY.register("forbidden_strength", key ->
        PassiveSkill.createPassiveBuilder(ForbiddenStrengthSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, HyperVitalitySkill> HYPERVITALITY = REGISTRY.register("hypervitality", key ->
        PassiveSkill.createPassiveBuilder(HyperVitalitySkill::new)
            .setResource(Resource.COOLDOWN)
            .setActivateType(ActivateType.TOGGLE)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, StaminaPillagerSkill> STAMINA_PILLAGER = REGISTRY.register("stamina_pillager", key ->
        PassiveSkill.createPassiveBuilder(StaminaPillagerSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, SwordmasterSkill> SWORD_MASTER = REGISTRY.register("swordmaster", key ->
        SwordmasterSkill.createSwordMasterBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, TechnicianSkill> TECHNICIAN = REGISTRY.register("technician", key ->
        PassiveSkill.createPassiveBuilder(TechnicianSkill::new).build(key)
    );

    public static final DeferredHolderShim<Skill, VengeanceSkill> VENGEANCE = REGISTRY.register("vengeance", key ->
        PassiveSkill.createPassiveBuilder(VengeanceSkill::new).setActivateType(ActivateType.DURATION).build(key)
    );

    public static final DeferredHolderShim<Skill, MeteorSlamSkill> METEOR_SLAM = REGISTRY.register("meteor_slam", key ->
        MeteorSlamSkill.createMeteorSlamBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, RevelationSkill> REVELATION = REGISTRY.register("revelation", key ->
        RevelationSkill.createRevelationSkillBuilder().build(key)
    );

    public static final DeferredHolderShim<Skill, DemolitionLeapSkill> DEMOLITION_LEAP = REGISTRY.register("demolition_leap", key ->
        Skill.createMoverBuilder(DemolitionLeapSkill::new)
            .setActivateType(ActivateType.HELD)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, PhantomAscentSkill> PHANTOM_ASCENT = REGISTRY.register("phantom_ascent", key ->
        Skill.createMoverBuilder(PhantomAscentSkill::new)
            .setResource(Resource.COOLDOWN)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, SimpleWeaponInnateSkill> SWEEPING_EDGE = REGISTRY.register("sweeping_edge", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder()
            .setAnimations(Animations.SWEEPING_EDGE)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(1))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(20.0F))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.6F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, SimpleWeaponInnateSkill> DANCING_EDGE = REGISTRY.register("dancing_edge", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder()
            .setAnimations(Animations.DANCING_EDGE)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.2F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.2F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.2F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, GuillotineAxeSkill> THE_GUILLOTINE = REGISTRY.register("the_guillotine", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder(GuillotineAxeSkill::new)
            .setAnimations(Animations.THE_GUILLOTINE)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.5F))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(20.0F))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, GraspingSpireSkill> GRASPING_SPIRE = REGISTRY.register("grasping_spire", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(GraspingSpireSkill::new)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(3))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(4.0F))
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(4))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.25F))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.2F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .build(key)
    );

    public static final DeferredHolderShim<Skill, SimpleWeaponInnateSkill> HEARTPIERCER = REGISTRY.register("heartpiercer", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder()
            .setAnimations(Animations.HEARTPIERCER)
            .newProperty()
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(10.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, SteelWhirlwindSkill> STEEL_WHIRLWIND = REGISTRY.register("steel_whirlwind", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(SteelWhirlwindSkill::new)
            .setActivateType(ActivateType.HELD)
            .newProperty()
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(1.4F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, BattojutsuSkill> BATTOJUTSU = REGISTRY.register("battojutsu", key ->
        ConditionalWeaponInnateSkill.createConditionalWeaponInnateBuilder(BattojutsuSkill::new)
            .setSelector(executer -> executer.getOriginal().isSprinting() ? 1 : 0)
            .setAnimations(Animations.BATTOJUTSU, Animations.BATTOJUTSU_DASH)
            .newProperty()
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(2.0F))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(50.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(6))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, RushingTempoSkill> RUSHING_TEMPO = REGISTRY.register("rushing_tempo", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(RushingTempoSkill::new)
            .newProperty()
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(50.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.adder(2))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.7F))
                .addProperty(AttackPhaseProperty.SWING_SOUND, EpicFightSounds.WHOOSH_SHARP.get())
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, SimpleWeaponInnateSkill> RELENTLESS_COMBO = REGISTRY.register("relentless_combo", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder()
            .setAnimations(Animations.RELENTLESS_COMBO)
            .newProperty()
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.6F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, SimpleWeaponInnateSkill> SHARP_STAB = REGISTRY.register("sharp_stab", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder()
            .setAnimations(Animations.SHARP_STAB)
            .newProperty()
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(0.4F))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.multiplier(0.5F))
                .addProperty(AttackPhaseProperty.PARTICLE, EpicFightParticles.HIT_BLUNT)
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE, EpicFightDamageTypeTags.GUARD_PUNCTURE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, LiechtenauerSkill> LIECHTENAUER = REGISTRY.register("liechtenauer", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(LiechtenauerSkill::new)
            .setActivateType(ActivateType.DURATION)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, EviscerateSkill> EVISCERATE = REGISTRY.register("eviscerate", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(EviscerateSkill::new)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.IMPACT_MODIFIER, ValueModifier.setter(2.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create(), ExtraDamageInstance.EVISCERATE_LOST_HEALTH.create(0.1F)))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(50.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.LONG)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, BladeRushSkill> BLADE_RUSH = REGISTRY.register("blade_rush", key ->
        BladeRushSkill.createBladeRushBuilder()
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .newProperty()
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.EXECUTION, EpicFightDamageTypeTags.WEAPON_INNATE, DamageTypeTags.BYPASSES_ARMOR))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.NONE)
                .addProperty(AttackPhaseProperty.HIT_SOUND, EpicFightSounds.BLADE_RUSH_FINISHER.get())
                .build(key)
    );

    public static final DeferredHolderShim<Skill, WrathfulLightingSkill> WRATHFUL_LIGHTING = REGISTRY.register("wrathful_lighting", key ->
        SimpleWeaponInnateSkill.createSimpleWeaponInnateBuilder(WrathfulLightingSkill::new)
            .setAnimations(Animations.WRATHFUL_LIGHTING)
            .newProperty()
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(1))
            .newProperty()
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.setter(8.0F))
                .addProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER, ValueModifier.setter(3))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(100.0F))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
            .build(key)
    );

    public static final DeferredHolderShim<Skill, ConditionalWeaponInnateSkill> TSUNAMI = REGISTRY.register("tsunami", key ->
        ConditionalWeaponInnateSkill.createConditionalWeaponInnateBuilder()
            .setSelector(executer -> executer.getOriginal().isInWaterOrRain() ? 1 : 0)
            .setAnimations(Animations.TSUNAMI, Animations.TSUNAMI_REINFORCED)
            .newProperty()
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(100.0F))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.KNOCKDOWN)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, EverlastingAllegiance> EVERLASTING_ALLEGIANCE = REGISTRY.register("everlasting_allegiance", key ->
        WeaponInnateSkill.createWeaponInnateBuilder(EverlastingAllegiance::new)
            .newProperty()
                .addProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER, ValueModifier.adder(30.0F))
                .addProperty(AttackPhaseProperty.DAMAGE_MODIFIER, ValueModifier.multiplier(1.4F))
                .addProperty(AttackPhaseProperty.EXTRA_DAMAGE, Set.of(ExtraDamageInstance.SWEEPING_EDGE_ENCHANTMENT.create()))
                .addProperty(AttackPhaseProperty.SOURCE_TAG, Set.of(EpicFightDamageTypeTags.WEAPON_INNATE))
                .addProperty(AttackPhaseProperty.STUN_TYPE, StunType.HOLD)
            .build(key)
    );

    public static final DeferredHolderShim<Skill, BattojutsuPassive> BATTOJUTSU_PASSIVE = REGISTRY.register("battojutsu_passive", key ->
        Skill.createBuilder(BattojutsuPassive::new)
            .setCategory(SkillCategories.WEAPON_PASSIVE)
            .setActivateType(ActivateType.ONE_SHOT)
            .setResource(Resource.COOLDOWN)
            .build(key)
    );
}
