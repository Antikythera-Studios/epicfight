package yesman.epicfight.api.animation.types;

import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageTypes;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageTypeTags;
import yesman.epicfight.world.damagesource.StunType;

public class KnockdownAnimation extends LongHitAnimation {
	public KnockdownAnimation(float transitionTime, AnimationAccessor<? extends KnockdownAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, accessor, armature);
		
		this.stateSpectrumBlueprint
			.addState(EntityState.KNOCKDOWN, true)
			.addState(EntityState.ATTACK_RESULT, (damagesource) -> {
				if (damagesource.getEntity() != null && !damagesource.is(DamageTypeTags.IS_EXPLOSION) && !damagesource.is(DamageTypes.MAGIC) && !damagesource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
					if (damagesource instanceof EpicFightDamageSource epicfight$damagesource) {
						if (epicfight$damagesource.is(EpicFightDamageTypeTags.FINISHER)) {
							epicfight$damagesource.setStunType(StunType.NONE);
							return AttackResult.ResultType.SUCCESS;
						}
						
						return AttackResult.ResultType.BLOCKED;
					} else {
						return AttackResult.ResultType.BLOCKED;
					}
				}
				
				return AttackResult.ResultType.SUCCESS;
			});
	}
	
	/**
	 * For resourcepack animation
	 */
	public KnockdownAnimation(float transitionTime, String path, AssetAccessor<? extends Armature> armature) {
		super(transitionTime, path, armature);
		
		this.stateSpectrumBlueprint
			.addState(EntityState.KNOCKDOWN, true)
			.addState(EntityState.ATTACK_RESULT, (damagesource) -> {
				if (damagesource.getEntity() != null && !damagesource.is(DamageTypeTags.IS_EXPLOSION) && !damagesource.is(DamageTypes.MAGIC) && !damagesource.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
					if (damagesource instanceof EpicFightDamageSource epicfight$damagesource) {
						if (epicfight$damagesource.is(EpicFightDamageTypeTags.FINISHER)) {
							epicfight$damagesource.setStunType(StunType.NONE);
							return AttackResult.ResultType.SUCCESS;
						}
						
						return AttackResult.ResultType.BLOCKED;
					} else {
						return AttackResult.ResultType.BLOCKED;
					}
				}
				
				return AttackResult.ResultType.SUCCESS;
			});
	}
}