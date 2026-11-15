package yesman.epicfight.world.capabilities.entitypatch;

import com.mojang.datafixers.util.Pair;

import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.data.reloader.MobPatchReloadListener;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.behavior.AnimatedCombatBehavior;
import yesman.epicfight.world.entity.ai.behavior.MoveToTargetSinkStopInaction;
import yesman.epicfight.world.entity.ai.brain.BrainRecomposer;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

public class CustomMobPatch<T extends PathfinderMob> extends MobPatch<T> {
	private final MobPatchReloadListener.CustomMobPatchProvider provider;
	
	public CustomMobPatch(T original, Faction faction, MobPatchReloadListener.CustomMobPatchProvider provider) {
		super(original, faction);
		this.provider = provider;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	protected void initAI() {
		super.initAI();
		
		boolean useBrain = !this.original.getBrain().availableBehaviorsByPriority.isEmpty();
		CombatBehaviors<CustomMobPatch<T>> combatBehaviors = ((CombatBehaviors.Builder<CustomMobPatch<T>>)this.provider.getCombatBehaviorsBuilder()).build(this);
		
		if (useBrain) {
			BrainRecomposer.recomposeBrainByType(this.original.getType(), this.original.getBrain(), new AnimatedCombatBehavior<>(this, combatBehaviors), new MoveToTargetSinkStopInaction());
		} else {
			this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, combatBehaviors));
			this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.getOriginal(), this.provider.getChasingSpeed(), true));
		}
	}
	
	public void initAttributes() {
		this.setBaseValueIfPresent(EpicFightAttributes.MAX_STRIKES);
		this.setBaseValueIfPresent(EpicFightAttributes.ARMOR_NEGATION);
		this.setBaseValueIfPresent(EpicFightAttributes.IMPACT);
		this.setBaseValueIfPresent(EpicFightAttributes.STUN_ARMOR);

		if (this.provider.getAttributeValues().containsKey(Attributes.ATTACK_DAMAGE)) {
			AttributeInstance attackDamage = this.original.getAttribute(Attributes.ATTACK_DAMAGE);

			if (attackDamage != null) {
				attackDamage.setBaseValue(this.provider.getAttributeValues().getDouble(Attributes.ATTACK_DAMAGE));
			}
		}
	}

	private void setBaseValueIfPresent(Holder<Attribute> attribute) {
		AttributeInstance instance = this.original.getAttribute(attribute);

		if (instance != null) {
			instance.setBaseValue(this.provider.getAttributeValues().getDouble(attribute));
		}
	}
	
	@Override
	public void readData(CompoundTag compound) {
		super.readData(compound);
		this.initAttributes();
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		
		for (Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>> pair : this.provider.getDefaultAnimations()) {
			animator.addLivingAnimation(pair.getFirst(), pair.getSecond());
		}
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		super.commonAggressiveMobUpdateMotion(considerInaction);
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		return this.provider.getStunAnimations().get(stunType);
	}
	
	@Override
	public SoundEvent getWeaponHitSound(InteractionHand hand) {
		CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
		
		if (itemCap.isEmpty()) {
			return this.provider.getHitSound();
		}
		
		return itemCap.getHitSound();
	}

	@Override
	public SoundEvent getSwingSound(InteractionHand hand) {
		CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
		
		if (itemCap.isEmpty()) {
			return this.provider.getSwingSound();
		}
		
		return itemCap.getSmashingSound();
	}

	@Override
	public HitParticleType getWeaponHitParticle(InteractionHand hand) {
		CapabilityItem itemCap = this.getAdvancedHoldingItemCapability(hand);
		
		if (itemCap.isEmpty()) {
			return this.provider.getHitParticle();
		}
		
		return itemCap.getHitParticle();
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		float scale = this.provider.getScale();
		return super.getModelMatrix(partialTicks).scale(scale, scale, scale);
	}
}