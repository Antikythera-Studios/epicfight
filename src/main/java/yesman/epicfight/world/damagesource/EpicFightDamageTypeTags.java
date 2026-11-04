package yesman.epicfight.world.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;
import yesman.epicfight.EpicFight;

public interface EpicFightDamageTypeTags {
	/**
	 * Decides if damage source can hurt the entity that is lying on ground
	 */
	TagKey<DamageType> FINISHER = create("finisher");
	
	/**
	 * Decides if damage source can neutralize the entity that is invulnerable
	 */
	TagKey<DamageType> COUNTER = create("counter");

	/**
	 * Decides if damage source type is execution
	 */
	TagKey<DamageType> EXECUTION = create("execution");
	
	/**
	 * Decides if damage source is weapon innate attack
	 */
	TagKey<DamageType> WEAPON_INNATE = create("weapon_innate");
	
	/**
	 * Decides if damage source can ignore guard
	 */
	TagKey<DamageType> GUARD_PUNCTURE = create("guard_puncture");
	
	/**
	 * Decides if the damage type is blockable by guard skills
	 */
	TagKey<DamageType> UNBLOCKALBE = create("unblockable");
	
	/**
	 * Decides if the damage type is blockable by guard skills
	 */
	TagKey<DamageType> NO_STUN = create("no_stun");
	
	/**
	 * This tag means if the damage source bypasses the dodge invulnerability
	 */
	TagKey<DamageType> BYPASS_DODGE = create("bypass_dodge");
	
	/**
	 * To express null value for skill data
	 */
	TagKey<DamageType> NONE = create("none");
	
	/**
	 * To express null value for skill data
	 */
	TagKey<DamageType> IS_MELEE = create("is_melee");
	
	/**
	 * To express null value for skill data
	 */
	TagKey<DamageType> IS_MAGIC = create("is_magic");
	
	private static TagKey<DamageType> create(String tagName) {
		return TagKey.create(Registries.DAMAGE_TYPE, EpicFight.identifier(tagName));
	}
}