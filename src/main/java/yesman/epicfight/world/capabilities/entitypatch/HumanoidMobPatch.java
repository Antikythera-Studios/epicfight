package yesman.epicfight.world.capabilities.entitypatch;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.*;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.LivingMotion;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.model.armature.HumanoidArmature;
import yesman.epicfight.network.EpicFightNetworkManager;
import yesman.epicfight.network.server.SPChangeLivingMotion;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.WeaponCategories;
import yesman.epicfight.world.capabilities.item.Style;
import yesman.epicfight.world.capabilities.item.WeaponCategory;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.ai.goal.CombatBehaviors;
import yesman.epicfight.world.entity.ai.goal.TargetChasingGoal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class HumanoidMobPatch<T extends PathfinderMob> extends MobPatch<T> {
	protected Map<WeaponCategory, Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>>> weaponLivingMotions;
	protected Map<WeaponCategory, Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>>> weaponAttackMotions;
	
	public HumanoidMobPatch(T original, Faction faction) {
		super(original, faction);
		this.setWeaponMotions();
	}
	
	@Override
	public HumanoidArmature getArmature() {
		return (HumanoidArmature)this.armature;
	}
	
	@Override
	protected void initAI() {
		super.initAI();
		
		if (this.original.getVehicle() != null && this.original.getVehicle() instanceof Mob) {
			this.setAIAsMounted(this.original.getVehicle());
		} else {
			this.setAIAsInfantry(this.original.getMainHandItem().getItem() instanceof ProjectileWeaponItem);
		}
	}
	
	@Override
	public void onStartTracking(ServerPlayer trackingPlayer) {
		this.modifyLivingMotionByCurrentItem(true);
	}
	
	protected void setWeaponMotions() {
		this.weaponLivingMotions = Maps.newHashMap();
		this.weaponLivingMotions.put(WeaponCategories.GREATSWORD, ImmutableMap.of(
			CapabilityItem.Styles.TWO_HAND, Set.of(
				Pair.of(LivingMotions.WALK, Animations.BIPED_WALK_TWOHAND),
				Pair.of(LivingMotions.CHASE, Animations.BIPED_WALK_TWOHAND)
			)
		));
		
		this.weaponAttackMotions = Maps.newHashMap();
		this.weaponAttackMotions.put(WeaponCategories.AXE, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS));
		this.weaponAttackMotions.put(WeaponCategories.HOE, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS));
		this.weaponAttackMotions.put(WeaponCategories.PICKAXE, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS));
		this.weaponAttackMotions.put(WeaponCategories.SHOVEL, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS));
		this.weaponAttackMotions.put(WeaponCategories.SWORD, ImmutableMap.of(CapabilityItem.Styles.ONE_HAND, MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS, CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_DUAL_SWORD));
		this.weaponAttackMotions.put(WeaponCategories.GREATSWORD, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_GREATSWORD));
		this.weaponAttackMotions.put(WeaponCategories.UCHIGATANA, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_KATANA));
		this.weaponAttackMotions.put(WeaponCategories.LONGSWORD, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_LONGSWORD));
		this.weaponAttackMotions.put(WeaponCategories.TACHI, ImmutableMap.of(CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_TACHI));
		this.weaponAttackMotions.put(WeaponCategories.SPEAR, ImmutableMap.of(CapabilityItem.Styles.ONE_HAND, MobCombatBehaviors.HUMANOID_SPEAR_ONEHAND, CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_SPEAR_TWOHAND));
		this.weaponAttackMotions.put(WeaponCategories.FIST, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_FIST));
		this.weaponAttackMotions.put(WeaponCategories.DAGGER, ImmutableMap.of(CapabilityItem.Styles.ONE_HAND, MobCombatBehaviors.HUMANOID_ONEHAND_DAGGER, CapabilityItem.Styles.TWO_HAND, MobCombatBehaviors.HUMANOID_TWOHAND_DAGGER));
		this.weaponAttackMotions.put(WeaponCategories.RANGED, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_FIST));
		this.weaponAttackMotions.put(WeaponCategories.TRIDENT, ImmutableMap.of(CapabilityItem.Styles.COMMON, MobCombatBehaviors.HUMANOID_SPEAR_ONEHAND));
	}
	
	protected CombatBehaviors.Builder<HumanoidMobPatch<?>> getHoldingItemWeaponMotionBuilder() {
		CapabilityItem itemCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		
		if (this.weaponAttackMotions.containsKey(itemCap.getWeaponCategory())) {
			Map<Style, CombatBehaviors.Builder<HumanoidMobPatch<?>>> motionByStyle = this.weaponAttackMotions.get(itemCap.getWeaponCategory());
			Style style = itemCap.getStyle(this);
			
			if (motionByStyle.containsKey(style) || motionByStyle.containsKey(CapabilityItem.Styles.COMMON)) {
				return motionByStyle.getOrDefault(style, motionByStyle.get(CapabilityItem.Styles.COMMON));
			}
		}
		
		return this.original.getMainHandItem().isEmpty() ? MobCombatBehaviors.HUMANOID_FIST : MobCombatBehaviors.HUMANOID_ONEHAND_TOOLS;
	}
	
	public void setAIAsInfantry(boolean holdingRanedWeapon) {
		CombatBehaviors.Builder<HumanoidMobPatch<?>> builder = this.getHoldingItemWeaponMotionBuilder();
		
		if (builder != null) {
			this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, builder.build(this)));
			this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.getOriginal(), 1.0D, true));
		}
	}
	
	public void setAIAsMounted(Entity ridingEntity) {
		if (ridingEntity instanceof AbstractHorse) {
			if (this.isArmed()) {
				this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, MobCombatBehaviors.MOUNT_HUMANOID_BEHAVIORS.build(this)));
				this.original.goalSelector.addGoal(1, new TargetChasingGoal(this, this.getOriginal(), 1.0D, true));
			}
		} else {
			// Non-horse mounts (e.g. chicken jockey, spider jockey) carry the rider rather than provide
			// dedicated mounted-combat motions, so let the rider keep its on-foot AI for attack timing.
			// The vehicle is driven by the preTick hook below, not by a goal -- adding a goal during
			// setAIAsMounted is unreliable because the chicken-jockey spawn sequence can race against
			// EF's capability-attach (rider becomes a passenger before its patch's setAIAsMounted runs),
			// leaving the goal never installed. preTick runs unconditionally every tick the rider lives.
			this.setAIAsInfantry(this.original.getMainHandItem().getItem() instanceof ProjectileWeaponItem);
		}
	}

	/** Heavy additive boost on the vehicle's MOVEMENT_SPEED while a hostile rider is chasing.
	 *  Applied/removed dynamically from preTick to track the rider's target acquisition state. */
	private static final AttributeModifier PASSENGER_CHASE_SPEED_BOOST =
		new AttributeModifier(EpicFightMod.identifier("passenger_chase_speed_boost"), 0.00005D, AttributeModifier.Operation.ADD_VALUE);

	private boolean drivingVehicle = false;

	@Override
	public void preTick() {
		super.preTick();

		// Drive the vehicle from the rider every tick. preTick runs regardless of goal state, so this
		// works even when the rider was spawned already-mounted (chicken jockey via /summon) and any
		// goal-based hookup might have missed its window during EF's capability-attach race.
		this.drivePassengerVehicle();
	}

	private void drivePassengerVehicle() {
		if (this.original == null || this.original.level().isClientSide()) {
			return;
		}

		Entity vehicleEntity = this.original.getVehicle();
		if (!(vehicleEntity instanceof Mob vehicleMob) || vehicleEntity instanceof AbstractHorse) {
			// No vehicle (or it's a horse, which has its own mounted-combat handling) -- make sure
			// we strip the boost if we just dismounted, then bail.
			this.applyVehicleSpeedBoost(null);
			this.drivingVehicle = false;
			return;
		}

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			// No target: revert speed and stop nav so the chicken doesn't keep streaking.
			if (this.drivingVehicle) {
				this.applyVehicleSpeedBoost(null);
				vehicleMob.getNavigation().stop();
				this.drivingVehicle = false;
			}
			return;
		}

		this.applyVehicleSpeedBoost(vehicleMob);
		this.drivingVehicle = true;

		double dx = target.getX() - vehicleMob.getX();
		double dz = target.getZ() - vehicleMob.getZ();
		double horizDistSqr = dx * dx + dz * dz;

		vehicleMob.getLookControl().setLookAt(target, 30.0F, 30.0F);

		if (horizDistSqr >= 36.0D) {
			// Far: pathfind. Re-path every tick so the chicken always heads at the player's
			// current position rather than a stale target location. speedModifier 1.5 matches the
			// pace of vanilla mob chase goals (vanilla MeleeAttackGoal uses ~1.0-1.2).
			Path path = vehicleMob.getNavigation().createPath(target, 0);
			if (path != null) {
				vehicleMob.getNavigation().moveTo(path, 1.5D);
			}
		} else if (horizDistSqr > 0.0001D) {
			// Close: stop navigation and overwrite delta each tick with a fixed velocity straight
			// at the player. SET, not ADD: friction can't erode a re-set delta, and collision-zeroing
			// only lasts the single tick before we restore it next tick. 0.25 b/tick = 5 b/s,
			// roughly vanilla zombie chase pace -- enough to wedge against the player without
			// rocketing past them.
			if (!vehicleMob.getNavigation().isDone()) {
				vehicleMob.getNavigation().stop();
			}
			double dist = Math.sqrt(horizDistSqr);
			Vec3 delta = vehicleMob.getDeltaMovement();
			vehicleMob.setDeltaMovement(
				(dx / dist) * 0.25D,
				delta.y,
				(dz / dist) * 0.25D
			);
			float yaw = (float) (Math.atan2(dz, dx) * (180D / Math.PI)) - 90.0F;
			vehicleMob.setYRot(yaw);
			vehicleMob.yBodyRot = yaw;
		}
	}

	/** Sync the chase speed modifier on a target vehicle (or strip it from any prior vehicle). */
	private void applyVehicleSpeedBoost(Mob targetVehicle) {
		if (targetVehicle != null) {
			AttributeInstance speedAttr = targetVehicle.getAttribute(Attributes.MOVEMENT_SPEED);
			if (speedAttr != null && !speedAttr.hasModifier(PASSENGER_CHASE_SPEED_BOOST.id())) {
				speedAttr.addTransientModifier(PASSENGER_CHASE_SPEED_BOOST);
			}
		}
	}
	
	protected final void commonMobAnimatorInit(Animator animator) {
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
		animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
		animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
		animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
	}
	
	protected final void commonAggresiveMobAnimatorInit(Animator animator) {
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.BIPED_IDLE);
		animator.addLivingAnimation(LivingMotions.WALK, Animations.BIPED_WALK);
		animator.addLivingAnimation(LivingMotions.CHASE, Animations.BIPED_WALK);
		animator.addLivingAnimation(LivingMotions.FALL, Animations.BIPED_FALL);
		animator.addLivingAnimation(LivingMotions.MOUNT, Animations.BIPED_MOUNT);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.BIPED_DEATH);
	}
	
	@Override
	public void updateHeldItem(CapabilityItem fromCap, CapabilityItem toCap, ItemStack from, ItemStack to, InteractionHand hand) {
		this.initAI();
		
		if (hand == InteractionHand.OFF_HAND) {
			if (!from.isEmpty()) {
                from.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
					if (attribute == Attributes.ATTACK_SPEED) {
						this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED).removeModifier(modifier);
					}
				});
			}
			
			if (!fromCap.isEmpty()) {
				fromCap.getAttributeModifiers(this).get(Attributes.ATTACK_SPEED).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED)::removeModifier);
				fromCap.getAttributeModifiers(this).get(EpicFightAttributes.ARMOR_NEGATION).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)::removeModifier);
				fromCap.getAttributeModifiers(this).get(EpicFightAttributes.IMPACT).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT)::removeModifier);
				fromCap.getAttributeModifiers(this).get(EpicFightAttributes.MAX_STRIKES).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES)::removeModifier);
			}
			
			if (!to.isEmpty()) {
				to.getAttributeModifiers().forEach(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
					if (attribute == Attributes.ATTACK_SPEED) {
						this.original.getAttribute(EpicFightAttributes.OFFHAND_ATTACK_SPEED).addTransientModifier(modifier);
					}
				});
			}
			
			if (!toCap.isEmpty()) {
				toCap.getAttributeModifiers(this).get(Attributes.ATTACK_SPEED).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)::addTransientModifier);
				toCap.getAttributeModifiers(this).get(EpicFightAttributes.ARMOR_NEGATION).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_ARMOR_NEGATION)::addTransientModifier);
				toCap.getAttributeModifiers(this).get(EpicFightAttributes.IMPACT).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_IMPACT)::addTransientModifier);
				toCap.getAttributeModifiers(this).get(EpicFightAttributes.MAX_STRIKES).forEach(this.original.getAttribute(EpicFightAttributes.OFFHAND_MAX_STRIKES)::addTransientModifier);
			}
		}
		
		this.modifyLivingMotionByCurrentItem(false);
		
		super.updateHeldItem(fromCap, toCap, from, to, hand);
	}
	
	public void modifyLivingMotionByCurrentItem(boolean onStartTracking) {
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> oldLivingAnimations = this.getAnimator().getLivingAnimations();
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> newLivingAnimations = Maps.newHashMap();
		
		CapabilityItem mainhandCap = this.getHoldingItemCapability(InteractionHand.MAIN_HAND);
		CapabilityItem offhandCap = this.getAdvancedHoldingItemCapability(InteractionHand.OFF_HAND);
		
		Map<LivingMotion, AssetAccessor<? extends StaticAnimation>> livingMotionModifiers = new HashMap<>(mainhandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND));
		Map<LivingMotion, ? extends AssetAccessor<? extends StaticAnimation>> offhandModifiers = offhandCap.getLivingMotionModifier(this, InteractionHand.OFF_HAND);

		// Same offhand-mirror reroute as ServerPlayerPatch: built-in caps already cover the OFF_HAND
		// path, so they take the first query and we never re-query. Addon caps that only register
		// MAIN_HAND modifiers return empty from OFF_HAND, fall through to the MAIN_HAND query, and
		// the visual flip in ClientAnimator.getPose lands the result on the correct side.
		if (offhandModifiers.isEmpty() && this.isMirrorMode()) {
			offhandModifiers = offhandCap.getLivingMotionModifier(this, InteractionHand.MAIN_HAND);
		}
		livingMotionModifiers.putAll(offhandModifiers);
		
		boolean hasChange = false;
		
		for (Map.Entry<LivingMotion, AssetAccessor<? extends StaticAnimation>> entry : livingMotionModifiers.entrySet()) {
			AssetAccessor<? extends StaticAnimation> aniamtion = entry.getValue();
			
			if (!oldLivingAnimations.containsKey(entry.getKey())) {
				hasChange = true;
			} else if (oldLivingAnimations.get(entry.getKey()) != aniamtion) {
				hasChange = true;
			}
			
			newLivingAnimations.put(entry.getKey(), aniamtion);
		}
		
		if (this.weaponLivingMotions != null && this.weaponLivingMotions.containsKey(mainhandCap.getWeaponCategory())) {
			Map<Style, Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>>> byStyle = this.weaponLivingMotions.get(mainhandCap.getWeaponCategory());
			Style style = mainhandCap.getStyle(this);
			
			if (byStyle.containsKey(style) || byStyle.containsKey(CapabilityItem.Styles.COMMON)) {
				Set<Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>>> animModifierSet = byStyle.getOrDefault(style, byStyle.get(CapabilityItem.Styles.COMMON));
				
				for (Pair<LivingMotion, AnimationAccessor<? extends StaticAnimation>> pair : animModifierSet) {
					newLivingAnimations.put(pair.getFirst(), pair.getSecond());
				}
			}
		}
		
		if (!hasChange) {
			for (LivingMotion oldLivingMotion : oldLivingAnimations.keySet()) {
				if (!newLivingAnimations.containsKey(oldLivingMotion)) {
					hasChange = true;
					break;
				}
			}
		}
		
		if (hasChange || onStartTracking) {
			this.getAnimator().resetLivingAnimations();
			newLivingAnimations.forEach(this.getAnimator()::addLivingAnimation);
			
			SPChangeLivingMotion msg = new SPChangeLivingMotion(this.original.getId());
			msg.putEntries(newLivingAnimations.entrySet());
			EpicFightNetworkManager.sendToAllPlayerTrackingThisEntity(msg, this.original);
		}
	}
	
	public boolean isArmed() {
		Item heldItem = this.original.getMainHandItem().getItem();
		return heldItem instanceof SwordItem || heldItem instanceof DiggerItem || heldItem instanceof TridentItem;
	}
	
	@Override
	public void onMount(boolean isMountOrDismount, Entity ridingEntity) {
		if (this.original == null) {
			return;
		}
		
		if (!this.original.level().isClientSide() && !this.original.isNoAi()) {
			Set<Goal> toRemove = Sets.newHashSet();
			this.selectGoalToRemove(toRemove);
			toRemove.forEach(this.original.goalSelector::removeGoal);
			
			if (isMountOrDismount) {
				this.setAIAsMounted(ridingEntity);
			} else {
				this.setAIAsInfantry(this.original.getMainHandItem().getItem() instanceof ProjectileWeaponItem);
			}
		}
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		if (this.original.getVehicle() != null) {
			return Animations.BIPED_HIT_ON_MOUNT;
		} else {
			switch (stunType) {
			case LONG:
				return Animations.BIPED_HIT_LONG;
			case SHORT:
				return Animations.BIPED_HIT_SHORT;
			case HOLD:
				return Animations.BIPED_HIT_SHORT;
			case KNOCKDOWN:
				return Animations.BIPED_KNOCKDOWN;
			case NEUTRALIZE:
				return Animations.BIPED_COMMON_NEUTRALIZED;
			case FALL:
				return Animations.BIPED_LANDING;
			case NONE:
				return null;
			}
		}
		
		return null;
	}
}