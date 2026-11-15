package yesman.epicfight.world.capabilities.entitypatch.boss;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import com.google.common.collect.ImmutableList;
import net.minecraft.commands.arguments.EntityAnchorArgument.Anchor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.platform.neoforged.event.EventHooks;
import yesman.epicfight.api.extension.BlockStateExtension;


import yesman.epicfight.registry.deferred_shim.DeferredHolderShim;
import org.joml.Quaternionf;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.Animator;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.LivingMotions;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.property.AnimationProperty.ActionAnimationProperty;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.MobCombatBehaviors;
import yesman.epicfight.mixin.common.MixinWitherBossAccessor;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightExpandedEntityDataAccessors;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.StunType;
import yesman.epicfight.world.entity.DroppedNetherStar;
import yesman.epicfight.world.entity.WitherGhostClone;
import yesman.epicfight.world.entity.ai.goal.AnimatedAttackGoal;
import yesman.epicfight.world.entity.data.ExpandedEntityDataAccessor;
import yesman.epicfight.world.entity.data.ExpandedSyncedData;
import yesman.epicfight.world.gamerule.EpicFightGameRules;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class WitherPatch extends MobPatch<WitherBoss> implements BossPatch<WitherBoss> {
	public WitherPatch(WitherBoss entity) {
		super(entity);
	}

	private static final List<DeferredHolderShim<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Vec3>>> DATA_LASER_TARGET_LOCATION_LIST = ImmutableList.of(
		  EpicFightExpandedEntityDataAccessors.WITHER_HEAD_LEFT_TARGET_LOCATION
		, EpicFightExpandedEntityDataAccessors.WITHER_HEAD_CENTER_TARGET_LOCATION
		, EpicFightExpandedEntityDataAccessors.WITHER_HEAD_RIGHT_TARGET_LOCATION
	);
	
	private static final List<DeferredHolderShim<ExpandedEntityDataAccessor<?>, ExpandedEntityDataAccessor<Integer>>> DATA_TARGET_ENTITY_ID_LIST = ImmutableList.of(
		  EpicFightExpandedEntityDataAccessors.WITHER_HEAD_LEFT_TARGET_ENTITY_ID
		, EpicFightExpandedEntityDataAccessors.WITHER_HEAD_CENTER_TARGET_ENTITY_ID
		, EpicFightExpandedEntityDataAccessors.WITHER_HEAD_RIGHT_TARGET_ENTITY_ID
	);
	
	public static final TargetingConditions WTIHER_GHOST_TARGETING_CONDITIONS = WitherBoss.TARGETING_CONDITIONS.copy().ignoreLineOfSight();
	
	private boolean blockedNow;
	private int deathTimerExt;
	private int blockingCount;
	private int blockingStartTick;
	private LivingEntityPatch<?> blockingEntity;
	
	@Override
	protected void registerExpandedEntityDataAccessors(final ExpandedSyncedData expandedSynchedData) {
		super.registerExpandedEntityDataAccessors(expandedSynchedData);
		
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_ARMOR_ACTIVATED);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_GHOST_MODE);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_TRANSPARENCY);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_LEFT_TARGET_LOCATION);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_CENTER_TARGET_LOCATION);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_RIGHT_TARGET_LOCATION);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_LEFT_TARGET_ENTITY_ID);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_CENTER_TARGET_ENTITY_ID);
		expandedSynchedData.register(EpicFightExpandedEntityDataAccessors.WITHER_HEAD_RIGHT_TARGET_ENTITY_ID);
	}
	
	@Override
	public void onStartTracking(ServerPlayer trackingPlayer) {
		this.recordBossEventOwner(trackingPlayer);
	}
	
	@Override
	public void onStopTracking(ServerPlayer trackingPlayer) {
		this.removeBossEventOwner(trackingPlayer);
	}
	
	@Override @ClientOnly
	public void entityPairing(SPEntityPairingPacket packet) {
		super.entityPairing(packet);
		
		if (packet.pairingPacketType() == EntityPairingPacketTypes.SET_BOSS_EVENT_OWNER) {
			this.processOwnerRecordPacket(packet.buffer());
		}
	}
	
	@Override
	public void initAI() {
		super.initAI();
		this.original.goalSelector.addGoal(1, new WitherChasingGoal());
		this.original.goalSelector.addGoal(0, new WitherGhostAttackGoal());
		this.original.goalSelector.addGoal(0, new AnimatedAttackGoal<>(this, MobCombatBehaviors.WITHER.build(this)));
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.WITHER, EpicFightAttributes.IMPACT, 3.0D);
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		animator.addLivingAnimation(LivingMotions.IDLE, Animations.WITHER_IDLE);
		animator.addLivingAnimation(LivingMotions.DEATH, Animations.WITHER_DEATH);
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		if (this.original.getHealth() <= 0.0F) {
			currentLivingMotion = LivingMotions.DEATH;
		} else {
			currentLivingMotion = LivingMotions.IDLE;
		}
	}
	
	@Override
	public void preTick() {
		if (this.original.getHealth() <= 0.0F) {
			if (this.original.deathTime > 1 && this.deathTimerExt < 17) {
				this.deathTimerExt++;
				this.original.deathTime--;
			}
		}
		
		if (!this.getEntityState().inaction()) {
			int targetId = this.original.getAlternativeTarget(0);
			Entity target = this.original.level().getEntity(targetId);
			
			if (target != null) {
				Vec3 vec3 = target.position().subtract(this.original.position()).normalize();
				float yrot = MathUtils.rotlerp(this.original.getYRot(), (float)Mth.atan2(vec3.z, vec3.x) * (180F / (float)Math.PI) - 90.0F, 10.0F);
				this.original.setYRot(yrot);
			}
		}
		
		super.preTick();
	}
	
	@Override
	public void poseTick(DynamicAnimation animation, Pose pose, float time, float partialTicks) {
		MixinWitherBossAccessor originalAccessor = this.getOriginalAsMixinAccessor();
		
		if (pose.hasTransform("Head_M")) {
			float headRotO = this.original.yBodyRotO - this.original.yHeadRotO;
			float headRot = this.original.yBodyRot - this.original.yHeadRot;
			float partialHeadRot = MathUtils.lerpBetween(headRotO, headRot, partialTicks);
			Quaternionf headRotation = OpenMatrix4f.createRotatorDeg(-this.original.getXRot(), Vec3f.X_AXIS).mulFront(OpenMatrix4f.createRotatorDeg(partialHeadRot, Vec3f.Y_AXIS)).toQuaternion();
			pose.orElseEmpty("Head_M").frontResult(JointTransform.rotation(headRotation), OpenMatrix4f::mul);
		}
		
		if (pose.hasTransform("Head_R")) {
			float rightHeadYRot = Mth.rotLerp(partialTicks, this.original.yBodyRotO, this.original.yBodyRot) - Mth.rotLerp(partialTicks, originalAccessor.getYRotOHeads()[1], originalAccessor.getYRotHeads()[1]);
			float rightHeadXRot = Mth.rotLerp(partialTicks, originalAccessor.getXRotOHeads()[1], originalAccessor.getXRotHeads()[1]);
			Quaternionf headRotation = OpenMatrix4f.createRotatorDeg(rightHeadYRot, Vec3f.Y_AXIS).rotateDeg(-rightHeadXRot, Vec3f.X_AXIS).toQuaternion();
			pose.orElseEmpty("Head_R").frontResult(JointTransform.rotation(headRotation), OpenMatrix4f::mul);
		}
		
		if (pose.hasTransform("Head_L")) {
			float leftHeadYRot = Mth.rotLerp(partialTicks, this.original.yBodyRotO, this.original.yBodyRot) - Mth.rotLerp(partialTicks, originalAccessor.getYRotOHeads()[0], originalAccessor.getYRotHeads()[0]);
			float leftHeadXRot = Mth.rotLerp(partialTicks, originalAccessor.getXRotOHeads()[0], originalAccessor.getXRotHeads()[0]);
			Quaternionf headRotation = OpenMatrix4f.createRotatorDeg(leftHeadYRot, Vec3f.Y_AXIS).rotateDeg(-leftHeadXRot, Vec3f.X_AXIS).toQuaternion();
			pose.orElseEmpty("Head_L").frontResult(JointTransform.rotation(headRotation), OpenMatrix4f::mul);
		}
	}
	
	@Override
	public void preTickClient() {
		super.preTickClient();
		this.original.setDeltaMovement(0.0D, 0.0D, 0.0D);
		int transparencyCount = this.getTransparency();
		
		if (transparencyCount != 0) {
			this.setTransparency(transparencyCount + (transparencyCount > 0 ? -1 : 1));
		}
	}
	
	@Override
	public void preTickServer() {
		super.preTickServer();
		
		if (this.original.getHealth() <= this.original.getMaxHealth() * 0.5F) {
			if (!this.isArmorActivated() && !this.getEntityState().inaction() && this.original.getInvulnerableTicks() <= 0 && this.original.isAlive()) {
				this.playAnimationSynchronized(Animations.WITHER_SPELL_ARMOR, 0.0F);
			}
		} else {
			if (this.isArmorActivated()) {
				this.setArmorActivated(false);
			}
		}
		
		if (this.animator.getPlayerFor(null).getAnimation().equals(Animations.WITHER_CHARGE) && this.getEntityState().attacking() && this.original.level().getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING)) {
			int x = Mth.floor(this.original.getX());
			int y = Mth.floor(this.original.getY());
			int z = Mth.floor(this.original.getZ());
			boolean flag = false;
			
			for (int j = -1; j <= 1; ++j) {
				for (int k2 = -1; k2 <= 1; ++k2) {
					for (int k = 0; k <= 3; ++k) {
						int l2 = x + j;
						int l = y + k;
						int i1 = z + k2;
						BlockPos blockpos = new BlockPos(l2, l, i1);
						BlockState blockstate = this.original.level().getBlockState(blockpos);
						
						if (BlockStateExtension.of(blockstate).epicfight$canEntityDestroy(this.original.level(), blockpos, this.original) && EventHooks.onEntityDestroyBlock(this.original, blockpos, blockstate)) {
							flag = this.original.level().destroyBlock(blockpos, true, this.original) || flag;
						}
					}
				}
			}
			
			if (flag) {
				this.original.level().levelEvent(null, 1022, this.original.blockPosition(), 0);
			}
		}
		
		if (this.blockedNow) {
			if (this.blockingCount < 0) {
				this.playAnimationSynchronized(Animations.WITHER_NEUTRALIZED, 0.0F);
				this.original.playSound(EpicFightSounds.NEUTRALIZE_BOSSES.get(), 5.0F, 1.0F);
				this.blockedNow = false;
				this.blockingEntity = null;
			} else {
				if (this.original.tickCount % 4 == (this.blockingStartTick - 1) % 4) {
					if (this.original.position().distanceToSqr(this.blockingEntity.getOriginal().position()) < 9.0D) {
						EpicFightDamageSource extendedSource = this.getDamageSource(Animations.WITHER_CHARGE, InteractionHand.MAIN_HAND);
						extendedSource
							.setStunType(StunType.KNOCKDOWN)
							.setBaseImpact(4.0F)
							.setInitialPosition(this.lastAttackPosition);
						
						AttackResult attackResult = this.tryHarm(this.blockingEntity.getOriginal(), extendedSource, blockingCount);
						
						if (attackResult.resultType == AttackResult.ResultType.SUCCESS) {
							this.blockingEntity.getOriginal().hurt(extendedSource, 4.0F);
							this.blockedNow = false;
							this.blockingEntity = null;
						}
					} else {
						this.blockedNow = false;
						this.blockingEntity = null;
					}
				}
			}
		}
	}
	
	@Override
	public void onAttackBlocked(DamageSource damageSource, LivingEntityPatch<?> opponent) {
		if (damageSource instanceof EpicFightDamageSource extendedDamageSource) {
			if (Animations.WITHER_CHARGE.equals(extendedDamageSource.getAnimation())) {
				if (!this.blockedNow) {
					this.blockedNow = true;
					this.blockingStartTick = this.original.tickCount;
					this.blockingEntity = opponent;
					this.playAnimationSynchronized(Animations.WITHER_BLOCKED, 0.0F);
				}
				
				this.blockingCount--;
				Vec3 lookAngle = opponent.getOriginal().getLookAngle();
				lookAngle = lookAngle.subtract(0.0D, lookAngle.y, 0.0D);
				lookAngle.scale(0.1D);
				this.original.setPos(opponent.getOriginal().position().add(lookAngle));
			}
		}
	}
	
	@Override
	public AttackResult tryHurt(DamageSource damageSource, float amount) {
		AssetAccessor<? extends DynamicAnimation> animation = this.getAnimator().getPlayerFor(null).getAnimation();
		
		if (animation.equals(Animations.WITHER_CHARGE) || animation.equals(Animations.WITHER_BLOCKED)) {
			Entity entity = damageSource.getDirectEntity();
			
			if (entity instanceof AbstractArrow) {
				return AttackResult.blocked(0.0F);
			}
		}
		
		return super.tryHurt(damageSource, amount);
	}

	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);
		
		if (!this.isLogicalClient() && this.original.level().getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT) && EpicFightGameRules.EPIC_DROP.getRuleValue(this.original.level())) {
			Vec3 startMovement = this.original.getLookAngle().scale(0.4D).add(0.0D, 0.63D, 0.0D);
			ItemEntity itemEntity = new DroppedNetherStar(this.original.level(), this.original.position().add(0.0D, this.original.getBbHeight() * 0.5D, 0.0D), startMovement);
			this.original.level().addFreshEntity(itemEntity);
		}
	}
	
	@Override
	public boolean onDrop(DamageSource source, Collection<ItemEntity> drops) {
		if (EpicFightGameRules.EPIC_DROP.getRuleValue(this.original.level())) {
            drops.removeIf(itemEntity -> itemEntity.getItem().is(Items.NETHER_STAR));
		}
		
		return false;
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		float prevYRot;
		float yRot;
		
		if (this.original.getVehicle() instanceof LivingEntity ridingEntity) {
			prevYRot = ridingEntity.yBodyRotO;
			yRot = ridingEntity.yBodyRot;
		} else {
			prevYRot = this.isLogicalClient() ? this.original.yBodyRotO : this.original.yRotO;
			yRot = this.isLogicalClient() ? this.original.yBodyRot : this.original.getYRot();
		}
		
		return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, prevYRot, yRot, partialTicks, 1.0F, 1.0F, 1.0F);
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		return null;
	}
	
	public void startCharging() {
		this.setLastAttackPosition();
		this.blockingCount = 3;
	}
	
	public void setArmorActivated(boolean set) {
		this.getExpandedSynchedData().set(EpicFightExpandedEntityDataAccessors.WITHER_ARMOR_ACTIVATED, set);
	}
	
	public boolean isArmorActivated() {
		return this.getExpandedSynchedData().get(EpicFightExpandedEntityDataAccessors.WITHER_ARMOR_ACTIVATED);
	}
	
	public void setGhost(boolean set) {
		this.getExpandedSynchedData().set(EpicFightExpandedEntityDataAccessors.WITHER_GHOST_MODE, set);
		this.original.setNoGravity(set);
		this.setTransparency(set ? 40 : -40);
		this.original.setInvisible(set);
	}
	
	public boolean isGhost() {
		return this.getExpandedSynchedData().get(EpicFightExpandedEntityDataAccessors.WITHER_GHOST_MODE);
	}
	
	public void setTransparency(int set) {
		this.getExpandedSynchedData().set(EpicFightExpandedEntityDataAccessors.WITHER_TRANSPARENCY, set);
	}
	
	public int getTransparency() {
		return this.getExpandedSynchedData().get(EpicFightExpandedEntityDataAccessors.WITHER_TRANSPARENCY);
	}
	
	public void setLaserTargetPosition(int head, Vec3 pos) {
		this.getExpandedSynchedData().set(DATA_LASER_TARGET_LOCATION_LIST.get(head), pos);
	}
	
	public Vec3 getLaserTargetPosition(int head) {
		return this.getExpandedSynchedData().get(DATA_LASER_TARGET_LOCATION_LIST.get(head));
	}
	
	public void setLaserTarget(int head, Entity target) {
		this.getExpandedSynchedData().set(DATA_TARGET_ENTITY_ID_LIST.get(head), target != null ? target.getId() : -1);
	}
	
	public Entity getLaserTargetEntity(int head) {
		int laserTarget = this.getExpandedSynchedData().get(DATA_TARGET_ENTITY_ID_LIST.get(head));
		return laserTarget > 0 ? this.original.level().getEntity(laserTarget) : null;
	}
	
	public Entity getAlternativeTargetEntity(int head) {
		int id = this.original.getAlternativeTarget(head);
		
		return id > 0 ? this.original.level().getEntity(id) : null;
	}
	
	public double getHeadX(int index) {
		if (index <= 0) {
			return this.original.getX();
		} else {
			float f = (this.original.getYRot() + (float) (180 * (index - 1))) * ((float) Math.PI / 180F);
			float f1 = Mth.cos(f);
			return this.original.getX() + (double) f1 * 1.3D;
		}
	}
	
	public double getHeadY(int index) {
		return index <= 0 ? this.original.getY() + 3.0D : this.original.getY() + 2.2D;
	}
	
	public double getHeadZ(int index) {
		if (index <= 0) {
			return this.original.getZ();
		} else {
			float f = (this.original.getYRot() + (float) (180 * (index - 1))) * ((float) Math.PI / 180F);
			float f1 = Mth.sin(f);
			return this.original.getZ() + (double) f1 * 1.3D;
		}
	}
	
	@Override
	public BossEvent getBossEvent() {
		return this.getOriginalAsMixinAccessor().getBossEvent();
	}
	
	public MixinWitherBossAccessor getOriginalAsMixinAccessor() {
		return (MixinWitherBossAccessor)this.original;
	}
	
	public class WitherGhostAttackGoal extends Goal {
		private int ghostSummonCount;
		private int maxGhostSpawn;
		private int summonInverval;
		private int cooldown;
		
		public WitherGhostAttackGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
		}
		
		@Override
		public boolean canUse() {
			return --this.cooldown < 0 && WitherPatch.this.isArmorActivated() && !WitherPatch.this.getEntityState().inaction() && WitherPatch.this.original.getTarget() != null;
		}
		
		@Override
		public boolean canContinueToUse() {
			return this.ghostSummonCount <= this.maxGhostSpawn;
		}
		
		@Override
		public void start() {
			WitherPatch.this.playAnimationSynchronized(Animations.WITHER_GHOST_STANDBY, 0.0F);
			WitherPatch.this.updateEntityState();
			WitherPatch.this.setGhost(true);
			List<LivingEntity> nearbyEnemies = this.getNearbyTargets();
			this.ghostSummonCount = 0;
			this.summonInverval = 25;
			this.maxGhostSpawn = Mth.clamp(nearbyEnemies.size() / 2, 2, 4);
		}
		
		@Override
		public void tick() {
			if (--this.summonInverval <= 0) {
				if (this.ghostSummonCount < this.maxGhostSpawn) {
					List<LivingEntity> nearbyEnemies = this.getNearbyTargets();
					
					if (!nearbyEnemies.isEmpty()) {
						LivingEntity randomTarget = nearbyEnemies.get(WitherPatch.this.original.getRandom().nextInt(nearbyEnemies.size()));
						Vec3 summonPosition = randomTarget.position().add(new Vec3(0.0D, 0.0D, 6.0D).yRot(WitherPatch.this.original.getRandom().nextFloat() * 360.0F));
						WitherGhostClone ghostclone = new WitherGhostClone((ServerLevel)WitherPatch.this.original.level(), summonPosition, randomTarget);
						WitherPatch.this.original.level().addFreshEntity(ghostclone);
					} else {
						this.ghostSummonCount = this.maxGhostSpawn + 1;
					}
				}
				
				this.ghostSummonCount++;
				this.summonInverval = (this.ghostSummonCount < this.maxGhostSpawn) ? 25 : 35;
				
				if (this.ghostSummonCount == this.maxGhostSpawn) {
					LivingEntity target = WitherPatch.this.original.getTarget();
					
					if (target != null) {
						Vec3 summonPosition = target.position().add(new Vec3(0.0D, 0.0D, 6.0D).yRot(WitherPatch.this.original.getRandom().nextFloat() * 360.0F)).add(0.0D, 5.0D, 0.0D);
						WitherPatch.this.original.setPos(summonPosition);
						WitherPatch.this.original.lookAt(Anchor.FEET, WitherPatch.this.original.getTarget().position());
					}
				}
			}
		}
		
		@Override
		public void stop() {
			this.cooldown = 300;
			
			if (WitherPatch.this.original.getTarget() != null) {
				WitherPatch.this.playSound(SoundEvents.WITHER_AMBIENT, -0.1F, 0.1F);
				WitherPatch.this.playAnimationSynchronized(Animations.WITHER_CHARGE, 0.0F);
			} else {
				WitherPatch.this.playAnimationSynchronized(Animations.OFF_ANIMATION_HIGHEST, 0.0F);
			}
			
			WitherPatch.this.setGhost(false);
		}
		
		public List<LivingEntity> getNearbyTargets() {
			return WitherPatch.this.original.level().getNearbyEntities(LivingEntity.class, WTIHER_GHOST_TARGETING_CONDITIONS, WitherPatch.this.original, WitherPatch.this.original.getBoundingBox().inflate(20.0D, 5.0D, 20.0D));
		}
	}
	
	public class WitherChasingGoal extends Goal {
		public WitherChasingGoal() {
			this.setFlags(EnumSet.of(Goal.Flag.MOVE));
		}
		
		@Override
		public boolean canUse() {
			return WitherPatch.this.original.getAlternativeTarget(0) > 0;
		}
		
		@Override
		public void tick() {
			WitherBoss witherBoss = WitherPatch.this.getOriginal();
			Vec3 vec3 = witherBoss.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D);
			Entity entity = witherBoss.level().getEntity(WitherPatch.this.original.getAlternativeTarget(0));
			
			if (!WitherPatch.this.getEntityState().hurt() && !WitherPatch.this.blockedNow) {
				if (entity != null) {
					Vec3 vec31 = new Vec3(entity.getX() - witherBoss.getX(), 0.0D, entity.getZ() - witherBoss.getZ());
					double d0 = vec3.y;
					
					if (witherBoss.getY() < entity.getY() || !witherBoss.isPowered() && witherBoss.getY() < entity.getY() + 5.0D && !WitherPatch.this.getAnimator().getPlayerFor(null).getAnimation().get().getProperty(ActionAnimationProperty.MOVE_VERTICAL).orElse(false)) {
						d0 = Math.max(0.0D, d0);
						d0 = d0 + (0.3D - d0 * (double) 0.6F);
					}
					
					vec3 = new Vec3(vec3.x, d0, vec3.z);
					double followingRange = witherBoss.isPowered() ? 9.0D : 49.0D;
					
					if (vec31.horizontalDistanceSqr() > followingRange && !WitherPatch.this.getEntityState().inaction()) {
						Vec3 vec32 = vec31.normalize();
						vec3 = vec3.add(vec32.x * 0.3D - vec3.x * 0.6D, 0.0D, vec32.z * 0.3D - vec3.z * 0.6D);
					}
				}
				
				witherBoss.setDeltaMovement(vec3);
			}
		}
	}
}