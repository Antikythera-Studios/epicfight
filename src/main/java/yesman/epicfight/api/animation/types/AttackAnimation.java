package yesman.epicfight.api.animation.types;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.AnimationVariables;
import yesman.epicfight.api.animation.AnimationVariables.SharedVariableKey;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackAnimationProperty;
import yesman.epicfight.api.animation.property.AnimationProperty.AttackPhaseProperty;
import yesman.epicfight.api.animation.property.MoveCoordFunctions;
import yesman.epicfight.api.animation.types.EntityState.StateFactor;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.collider.Collider;
import yesman.epicfight.api.event.EpicFightEventHooks;
import yesman.epicfight.api.event.types.animation.AttackPhaseEndEvent;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.HitEntityList;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.ValueModifier;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.main.EpicFightSharedConstants;
import yesman.epicfight.particle.HitParticleType;
import yesman.epicfight.world.capabilities.entitypatch.HumanoidMobPatch;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;

import javax.annotation.Nullable;
import java.util.*;

public class AttackAnimation extends ActionAnimation {
	/** Entities that collided **/
	public static final SharedVariableKey<List<Entity>> ATTACK_TRIED_ENTITIES = AnimationVariables.unsynchShared(animator -> new ArrayList<> (), false);
	
	/** Entities that actually hurt **/
	public static final SharedVariableKey<List<LivingEntity>> ACTUALLY_HIT_ENTITIES = AnimationVariables.unsynchShared(animator -> new ArrayList<> (), false);
	
	public final Phase[] phases;
	
	public AttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, @Nullable Collider collider, Joint colliderJoint, AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		this(transitionTime, accessor, armature, new Phase(0.0F, antic, preDelay, contact, recovery, Float.MAX_VALUE, colliderJoint, collider));
	}
	
	public AttackAnimation(float transitionTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature) {
		this(transitionTime, accessor, armature, new Phase(0.0F, antic, preDelay, contact, recovery, Float.MAX_VALUE, hand, colliderJoint, collider));
	}
	
	public AttackAnimation(float transitionTime, AnimationAccessor<? extends AttackAnimation> accessor, AssetAccessor<? extends Armature> armature, Phase... phases) {
		super(transitionTime, accessor, armature);
		
		this.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_TARGET_DISTANCE);
		this.addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_TARGET_DISTANCE);
		this.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.MODEL_COORD);
		this.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.ATTACK_TARGET_LOCATION);
		this.addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.MOB_ATTACK_TARGET_LOOK);
		this.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true);
		
		this.phases = phases;
		this.stateSpectrumBlueprint.clear();
		
		for (Phase phase : phases) {
			if (!phase.noStateBind) {
				this.bindPhaseState(phase);
			}
		}
	}
	
	/**
	 * For resourcepack animation
	 */
	public AttackAnimation(float convertTime, float antic, float preDelay, float contact, float recovery, InteractionHand hand, @Nullable Collider collider, Joint colliderJoint, String path, AssetAccessor<? extends Armature> armature) {
		this(convertTime, path, armature, new Phase(0.0F, antic, preDelay, contact, recovery, Float.MAX_VALUE, hand, colliderJoint, collider));
	}
	
	/**
	 * For resourcepack animation
	 */
	public AttackAnimation(float convertTime, String path, AssetAccessor<? extends Armature> armature, Phase... phases) {
		super(convertTime, 0.0F, path, armature);
		
		this.addProperty(ActionAnimationProperty.COORD_SET_BEGIN, MoveCoordFunctions.TRACE_TARGET_DISTANCE);
		this.addProperty(ActionAnimationProperty.COORD_SET_TICK, MoveCoordFunctions.TRACE_TARGET_DISTANCE);
		this.addProperty(ActionAnimationProperty.COORD_GET, MoveCoordFunctions.MODEL_COORD);
		this.addProperty(ActionAnimationProperty.DEST_LOCATION_PROVIDER, MoveCoordFunctions.ATTACK_TARGET_LOCATION);
		this.addProperty(ActionAnimationProperty.ENTITY_YROT_PROVIDER, MoveCoordFunctions.MOB_ATTACK_TARGET_LOOK);
		this.addProperty(ActionAnimationProperty.STOP_MOVEMENT, true);
		
		this.phases = phases;
		this.stateSpectrumBlueprint.clear();
		
		for (Phase phase : phases) {
			if (!phase.noStateBind) {
				this.bindPhaseState(phase);
			}
		}
	}
	
	protected void bindPhaseState(Phase phase) {
		float preDelay = phase.preDelay;
		
		this.stateSpectrumBlueprint
			.newTimePair(phase.start, preDelay)
			.addState(EntityState.PHASE_LEVEL, 1)
			.newTimePair(phase.start, phase.contact)
			.addState(EntityState.SKILL_EXECUTABLE, false)
			.newTimePair(phase.start, phase.recovery)
			.addState(EntityState.MOVEMENT_LOCKED, true)
			.addState(EntityState.UPDATE_LIVING_MOTION, false)
			.addState(EntityState.COMBO_ATTACKS_DOABLE, false)
			.newTimePair(phase.start, phase.end)
			.addState(EntityState.INACTION, true)
			.newTimePair(phase.antic, phase.end)
			.addState(EntityState.TURNING_LOCKED, true)
			.newTimePair(preDelay, phase.contact)
			.addState(EntityState.ATTACKING, true)
			.addState(EntityState.PHASE_LEVEL, 2)
			.newTimePair(phase.contact, phase.end)
			.addState(EntityState.PHASE_LEVEL, 3)
			;
	}
	
	@Override
	public void begin(LivingEntityPatch<?> entitypatch) {
		super.begin(entitypatch);
		
		entitypatch.setLastAttackSuccess(false);
	}
	
	@Override
	public void linkTick(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> linkAnimation) {
		super.linkTick(entitypatch, linkAnimation);
		
		if (!entitypatch.isLogicalClient()) {
			this.attackTick(entitypatch, linkAnimation);
		}
	}
	
	@Override
	public void tick(LivingEntityPatch<?> entitypatch) {
		super.tick(entitypatch);
		
		if (!entitypatch.isLogicalClient()) {
			this.attackTick(entitypatch, this.getAccessor());
		}
	}
	
	@Override
	public void end(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> nextAnimation, boolean isEnd) {
		super.end(entitypatch, nextAnimation, isEnd);

        AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(this.getAccessor());

        if (player == null) {
            return;
        }

        float elapsedTime = player.getElapsedTime();
        EntityState state = this.getState(entitypatch, elapsedTime);

        if (!isEnd && state.attacking() && !entitypatch.isLogicalClient()) {
            EpicFightEventHooks.Animation.ATTACK_PHASE_END.postWithListener(new AttackPhaseEndEvent(entitypatch, this.getAccessor(), this.getPhaseByTime(elapsedTime), this.getPhaseOrderByTime(elapsedTime), true), entitypatch.getEventListener());
        }

		if (entitypatch instanceof HumanoidMobPatch<?> mobpatch && entitypatch.isLogicalClient()) {
			Mob entity = mobpatch.getOriginal();
			
			if (entity.getTarget() != null && !entity.getTarget().isAlive()) {
				entity.setTarget(null);
			}
		}
	}
	
	protected void attackTick(LivingEntityPatch<?> entitypatch, AssetAccessor<? extends DynamicAnimation> animation) {
		AnimationPlayer player = entitypatch.getAnimator().getPlayerFor(this.getAccessor());

        if (player == null) {
            return;
        }

		float prevElapsedTime = player.getPrevElapsedTime();
		float elapsedTime = player.getElapsedTime();
		EntityState prevState = animation.get().getState(entitypatch, prevElapsedTime);
		EntityState state = animation.get().getState(entitypatch, elapsedTime);
		Phase phase = this.getPhaseByTime(animation.get().isLinkAnimation() ? 0.0F : elapsedTime);
		
		if (prevState.attacking() || state.attacking() || (prevState.getLevel() <= 2 && state.getLevel() > 2)) {
			if (!prevState.attacking() || (phase != this.getPhaseByTime(prevElapsedTime) && (state.attacking() || (prevState.getLevel() <= 2 && state.getLevel() > 2)))) {
				entitypatch.onStrike(this, phase.effectiveHand(entitypatch));
				entitypatch.playSound(this.getSwingSound(entitypatch, phase), 0.0F, 0.0F);
				entitypatch.removeHurtEntities();
			}
			
			this.hurtCollidingEntities(entitypatch, prevElapsedTime, elapsedTime, prevState, state, phase);
			
			if ((!state.attacking() || elapsedTime >= this.getTotalTime()) && entitypatch instanceof ServerPlayerPatch playerpatch) {
                EpicFightEventHooks.Animation.ATTACK_PHASE_END.postWithListener(new AttackPhaseEndEvent(entitypatch, this.getAccessor(), phase, this.getPhaseOrderByTime(elapsedTime), false), entitypatch.getEventListener());
			}
		}
	}
	
	protected void hurtCollidingEntities(LivingEntityPatch<?> entitypatch, float prevElapsedTime, float elapsedTime, EntityState prevState, EntityState state, Phase phase) {
		LivingEntity entity = entitypatch.getOriginal();
		float prevPoseTime = prevState.attacking() ? prevElapsedTime : phase.preDelay;
		float poseTime = state.attacking() ? elapsedTime : phase.contact;
		List<Entity> list = this.getPhaseByTime(elapsedTime).getCollidingEntities(entitypatch, this, prevPoseTime, poseTime, this.getPlaySpeed(entitypatch, this));
		
		if (!list.isEmpty()) {
			HitEntityList hitEntities = new HitEntityList(entitypatch, list, phase.getProperty(AttackPhaseProperty.HIT_PRIORITY).orElse(HitEntityList.Priority.DISTANCE));
			int maxStrikes = this.getMaxStrikes(entitypatch, phase);
			
			while (entitypatch.getCurrentlyActuallyHitEntities().size() < maxStrikes && hitEntities.next()) {
				Entity target = hitEntities.getEntity();
				LivingEntity trueEntity = this.getTrueEntity(target);
				
				if (trueEntity != null && trueEntity.isAlive() && !entitypatch.getCurrentlyAttackTriedEntities().contains(trueEntity) && !entitypatch.isTargetInvulnerable(target)) {
					if (target instanceof LivingEntity || target instanceof net.minecraft.world.entity.boss.EnderDragonPart) {
						AABB aabb = target.getBoundingBox();
						
						if (MathUtils.canBeSeen(target, entity, target.position().distanceTo(entity.getEyePosition()) + aabb.getCenter().distanceTo(new Vec3(aabb.maxX, aabb.maxY, aabb.maxZ)))) {
							EpicFightDamageSource damagesource = this.getEpicFightDamageSource(entitypatch, target, phase);
							int prevInvulTime = target.invulnerableTime;
							target.invulnerableTime = 0;
							
							AttackResult attackResult = entitypatch.attack(damagesource, target, phase.effectiveHand(entitypatch));
							target.invulnerableTime = prevInvulTime;
							
							if (attackResult.resultType.dealtDamage()) {
                                SoundEvent hitSound = this.getHitSound(entitypatch, phase);

                                if (hitSound != null) {
								    target.level().playSound(null, target.getX(), target.getY(), target.getZ(), hitSound, target.getSoundSource(), 1.0F, 1.0F);
                                }

								this.spawnHitParticle((ServerLevel)target.level(), entitypatch, target, phase);
							}
							
							entitypatch.getCurrentlyAttackTriedEntities().add(trueEntity);
							
							if (attackResult.resultType.shouldCount()) {
								entitypatch.getCurrentlyActuallyHitEntities().add(trueEntity);
							}
						}
					}
				}
			}
		}
	}
	
	public LivingEntity getTrueEntity(Entity entity) {
		if (entity instanceof LivingEntity livingEntity) {
			return livingEntity;
		} else if (entity instanceof net.minecraft.world.entity.boss.EnderDragonPart enderDragonPart) {
			Entity parentEntity = enderDragonPart.parentMob;
			
			if (parentEntity instanceof LivingEntity livingEntity) {
				return livingEntity;
			}
		}
		
		return null;
	}
	
	protected int getMaxStrikes(LivingEntityPatch<?> entitypatch, Phase phase) {
		InteractionHand hand = phase.effectiveHand(entitypatch);
		return phase.getProperty(AttackPhaseProperty.MAX_STRIKES_MODIFIER)
					.map(valueModifier -> (int)ValueModifier.calculator().attach(valueModifier).getResult(entitypatch.getMaxStrikes(hand)))
					.orElse(entitypatch.getMaxStrikes(hand));
	}

	protected SoundEvent getSwingSound(LivingEntityPatch<?> entitypatch, Phase phase) {
		return phase.getProperty(AttackPhaseProperty.SWING_SOUND).orElse(entitypatch.getSwingSound(phase.effectiveHand(entitypatch)));
	}

	protected SoundEvent getHitSound(LivingEntityPatch<?> entitypatch, Phase phase) {
		return phase.getProperty(AttackPhaseProperty.HIT_SOUND).orElse(entitypatch.getWeaponHitSound(phase.effectiveHand(entitypatch)));
	}

	public EpicFightDamageSource getEpicFightDamageSource(LivingEntityPatch<?> entitypatch, Entity target, Phase phase) {
		return this.getEpicFightDamageSource(entitypatch.getDamageSource(this.getAccessor(), phase.effectiveHand(entitypatch)), entitypatch, target, phase);
	}
	
	public EpicFightDamageSource getEpicFightDamageSource(DamageSource originalSource, LivingEntityPatch<?> entitypatch, Entity target, Phase phase) {
		if (phase == null) {
			phase = this.getPhaseByTime(entitypatch.getAnimator().getPlayerFor(this.getAccessor()).getElapsedTime());
		}
		
		EpicFightDamageSource epicfightSource;
		
		if (originalSource instanceof EpicFightDamageSource epicfightDamageSource) {
			epicfightSource = epicfightDamageSource;
		} else {
			epicfightSource = EpicFightDamageSources.fromVanillaDamageSource(originalSource).setAnimation(this.getAccessor());
		}
		
		phase.getProperty(AttackPhaseProperty.DAMAGE_MODIFIER).ifPresent(epicfightSource::attachDamageModifier);
		
		phase.getProperty(AttackPhaseProperty.ARMOR_NEGATION_MODIFIER).ifPresent(epicfightSource::attachArmorNegationModifier);
		
		phase.getProperty(AttackPhaseProperty.IMPACT_MODIFIER).ifPresent(epicfightSource::attachImpactModifier);
		
		phase.getProperty(AttackPhaseProperty.STUN_TYPE).ifPresent(epicfightSource::setStunType);
		
		phase.getProperty(AttackPhaseProperty.SOURCE_TAG).ifPresent(opt -> opt.forEach(epicfightSource::addRuntimeTag));
		
		phase.getProperty(AttackPhaseProperty.EXTRA_DAMAGE).ifPresent(opt -> opt.forEach(epicfightSource::addExtraDamage));
		
		phase.getProperty(AttackPhaseProperty.SOURCE_LOCATION_PROVIDER).ifPresentOrElse(
            opt -> epicfightSource.setInitialPosition(opt.apply(entitypatch)),
            () -> epicfightSource.setInitialPosition(entitypatch.getOriginal().position()))
        ;
		
		return epicfightSource;
	}
	
	protected void spawnHitParticle(ServerLevel world, LivingEntityPatch<?> attacker, Entity hit, Phase phase) {
		Optional<DeferredHolderShim<ParticleType<?>, HitParticleType>> particleOptional = phase.getProperty(AttackPhaseProperty.PARTICLE);
		HitParticleType particle = particleOptional.map(DeferredHolderShim::get).orElseGet(() -> attacker.getWeaponHitParticle(phase.effectiveHand(attacker)));
		particle.spawnParticleWithArgument(world, null, null, hit, attacker.getOriginal());
	}
	
	@Override
	public float getPlaySpeed(LivingEntityPatch<?> entitypatch, DynamicAnimation animation) {
        Phase phase = this.getPhaseByTime(entitypatch.getAnimator().getPlayerFor(this.getAccessor()).getElapsedTime());
        float speedFactor = this.getProperty(AttackAnimationProperty.ATTACK_SPEED_FACTOR).orElse(1.0F);
        Optional<Float> property = this.getProperty(AttackAnimationProperty.BASIS_ATTACK_SPEED);
        InteractionHand effective = phase.effectiveHand(entitypatch);
        float correctedSpeed = property.map((value) -> entitypatch.getAttackSpeed(effective) / value).orElse(this.getTotalTime() * entitypatch.getAttackSpeed(effective));
        correctedSpeed = Math.round(correctedSpeed * 1000.0F) / 1000.0F;

        return 1.0F + (correctedSpeed - 1.0F) * speedFactor;
	}
	
	@SuppressWarnings("unchecked")
	public <V, A extends AttackAnimation> A addProperty(AttackPhaseProperty<V> propertyType, V value) {
		return (A)this.addProperty(propertyType, value, 0);
	}
	
	@SuppressWarnings("unchecked")
	public <V, A extends AttackAnimation> A addProperty(AttackPhaseProperty<V> propertyType, V value, int index) {
		this.phases[index].addProperty(propertyType, value);
		return (A)this;
	}
	
	public <A extends AttackAnimation> A removeProperty(AttackPhaseProperty<?> propertyType) {
		return this.removeProperty(propertyType, 0);
	}
	
	@SuppressWarnings("unchecked")
	public <A extends AttackAnimation> A removeProperty(AttackPhaseProperty<?> propertyType, int index) {
		this.phases[index].removeProperty(propertyType);
		return (A)this;
	}
	
	public Phase getPhaseByTime(float elapsedTime) {
		Phase currentPhase = null;
		
		for (Phase phase : this.phases) {
			currentPhase = phase;
			
			if (phase.end > elapsedTime) {
				break;
			}
		}
		
		return currentPhase;
	}
	
	public int getPhaseOrderByTime(float elapsedTime) {
		int i = 0;
		
		for (Phase phase : this.phases) {
			if (phase.end > elapsedTime) {
				break;
			}
			
			i++;
		}
		
		return i;
	}
	
	@Override
	public Object getModifiedLinkState(StateFactor<?> factor, Object val, LivingEntityPatch<?> entitypatch, float elapsedTime) {
		if (factor == EntityState.ATTACKING && elapsedTime < this.getPlaySpeed(entitypatch, this) * EpicFightSharedConstants.A_TICK) {
			return false;
		}
		
		return val;
	}
	
	@Override @ClientOnly
	public void renderDebugging(PoseStack poseStack, MultiBufferSource buffer, LivingEntityPatch<?> entitypatch, float playbackTime, float partialTicks) {
        AnimationPlayer animPlayer = entitypatch.getAnimator().getPlayerFor(this.getAccessor());
        if (animPlayer == null) {
            return;
        }

		float prevElapsedTime = animPlayer.getPrevElapsedTime();
		float elapsedTime = animPlayer.getElapsedTime();
		Phase phase = this.getPhaseByTime(playbackTime);
		
		for (Pair<Joint, Collider> colliderInfo : phase.colliders) {
			Collider collider = colliderInfo.getSecond();
			
			if (collider == null) {
				collider = entitypatch.getColliderMatching(phase.hand);
			}
			
			collider.draw(poseStack, buffer, entitypatch, this, colliderInfo.getFirst(), prevElapsedTime, elapsedTime, partialTicks, this.getPlaySpeed(entitypatch, this));
		}
	}
	
	public static class JointColliderPair extends Pair<Joint, Collider> {
		public JointColliderPair(Joint first, Collider second) {
			super(first, second);
		}
		
		public static JointColliderPair of(Joint joint, Collider collider) {
			return new JointColliderPair(joint, collider);
		}
	}
	
	public static class Phase {
		private final Map<AttackPhaseProperty<?>, Object> properties = Maps.newHashMap();
		public final float start;
		public final float antic;
		public final float preDelay;
		public final float contact;
		public final float recovery;
		public final float end;
		public final InteractionHand hand;
		public JointColliderPair[] colliders;
		
		//public final Joint first;
		//public final Collider second;
		
		public final boolean noStateBind;
		
		public Phase(float start, float antic, float contact, float recovery, float end, Joint joint, Collider collider) {
			this(start, antic, contact, recovery, end, InteractionHand.MAIN_HAND, joint, collider);
		}
		
		public Phase(float start, float antic, float contact, float recovery, float end, InteractionHand hand, Joint joint, Collider collider) {
			this(start, antic, antic, contact, recovery, end, hand, joint, collider);
		}
		
		public Phase(float start, float antic, float preDelay, float contact, float recovery, float end, Joint joint, Collider collider) {
			this(start, antic, preDelay, contact, recovery, end, InteractionHand.MAIN_HAND, joint, collider);
		}
		
		public Phase(float start, float antic, float preDelay, float contact, float recovery, float end, InteractionHand hand, Joint joint, Collider collider) {
			this(start, antic, preDelay, contact, recovery, end, false, hand, joint, collider);
		}
		
		public Phase(InteractionHand hand, Joint joint, Collider collider) {
			this(0, 0, 0, 0, 0, 0, true, hand, joint, collider);
		}
		
		public Phase(float start, float antic, float preDelay, float contact, float recovery, float end, boolean noStateBind, InteractionHand hand, Joint joint, Collider collider) {
			this(start, antic, preDelay, contact, recovery, end, noStateBind, hand, JointColliderPair.of(joint, collider));
		}
		
		public Phase(float start, float antic, float preDelay, float contact, float recovery, float end, InteractionHand hand, JointColliderPair... colliders) {
			this(start, antic, preDelay, contact, recovery, end, false, hand, colliders);
		}
		
		public Phase(float start, float antic, float preDelay, float contact, float recovery, float end, boolean noStateBind, InteractionHand hand, JointColliderPair... colliders) {
			if (start > end) {
				throw new IllegalArgumentException("Phase create exception: Start time is bigger than end time");
			}
			
			this.start = start;
			this.antic = antic;
			this.preDelay = preDelay;
			this.contact = contact;
			this.recovery = recovery;
			this.end = end;
			this.colliders = colliders;
			this.hand = hand;
			this.noStateBind = noStateBind;
		}
		
		public <V> Phase addProperty(AttackPhaseProperty<V> propertyType, V value) {
			this.properties.put(propertyType, value);
			return this;
		}
		
		public Phase removeProperty(AttackPhaseProperty<?> propertyType) {
			this.properties.remove(propertyType);
			return this;
		}
		
		public void addProperties(Set<Map.Entry<AttackPhaseProperty<?>, Object>> set) {
			for(Map.Entry<AttackPhaseProperty<?>, Object> entry : set) {
				this.properties.put(entry.getKey(), entry.getValue());
			}
		}

		/** Read-only view of all phase properties; used by mirror wrappers to clone phase
		 *  configuration (damage modifiers, hit priority, sounds...) onto a remapped Phase. */
		public Set<Map.Entry<AttackPhaseProperty<?>, Object>> getPropertyEntries() {
			return this.properties.entrySet();
		}

		@SuppressWarnings("unchecked")
		public <V> Optional<V> getProperty(AttackPhaseProperty<V> propertyType) {
			return (Optional<V>) Optional.ofNullable(this.properties.get(propertyType));
		}
		
		public List<Entity> getCollidingEntities(LivingEntityPatch<?> entitypatch, AttackAnimation animation, float prevElapsedTime, float elapsedTime, float attackSpeed) {
			Set<Entity> entities = Sets.newHashSet();
			
			for (Pair<Joint, Collider> colliderInfo : this.colliders) {
				Collider collider = colliderInfo.getSecond();
				
				if (collider == null) {
					collider = entitypatch.getColliderMatching(this.hand);
				}
				
				entities.addAll(collider.updateAndSelectCollideEntity(entitypatch, animation, prevElapsedTime, elapsedTime, colliderInfo.getFirst(), attackSpeed));
			}
			
			return new ArrayList<>(entities);
		}
		
		public JointColliderPair[] getColliders() {
			return this.colliders;
		}
		
		public InteractionHand getHand() {
			return this.hand;
		}

		/** Returns the hand that should drive damage attribution, swing sounds, hit particles
		 *  and similar per-hand reads for this phase given the entity's current state. Equals
		 *  {@link #hand} for the normal mainhand-driven case; flips it when the entity is in
		 *  offhand-only mirror mode so the offhand weapon's sound / impact / damage source is
		 *  used instead of the (empty) mainhand's. */
		public InteractionHand effectiveHand(LivingEntityPatch<?> entitypatch) {
			if (entitypatch.isMirrorMode()) {
				return this.hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			}
			return this.hand;
		}
	}
}
