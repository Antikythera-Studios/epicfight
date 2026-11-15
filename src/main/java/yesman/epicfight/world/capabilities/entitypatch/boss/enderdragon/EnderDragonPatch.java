package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;
import yesman.epicfight.platform.fabric.event.EntityAttributeModificationEvent;

import com.google.common.collect.Maps;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.NotNull;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.*;
import yesman.epicfight.api.animation.property.AnimationProperty.StaticAnimationProperty;
import yesman.epicfight.api.animation.types.ActionAnimation;
import yesman.epicfight.api.animation.types.DynamicAnimation;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.physics.PhysicsSimulator;
import yesman.epicfight.api.physics.SimulationTypes;
import yesman.epicfight.api.physics.ik.InverseKinematicsProvider;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulatable;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator;
import yesman.epicfight.api.physics.ik.InverseKinematicsSimulator.BakedInverseKinematicsDefinition;
import yesman.epicfight.api.utils.AttackResult;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.data.loot.function.SetSkillFunction;
import yesman.epicfight.gameasset.Animations;
import yesman.epicfight.gameasset.Armatures;
import yesman.epicfight.network.EntityPairingPacketTypes;
import yesman.epicfight.network.server.SPEntityPairingPacket;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.registry.entries.EpicFightSkills;
import yesman.epicfight.registry.entries.EpicFightSounds;
import yesman.epicfight.world.capabilities.entitypatch.MobPatch;
import yesman.epicfight.world.capabilities.entitypatch.boss.BossPatch;
import yesman.epicfight.world.damagesource.StunType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class EnderDragonPatch extends MobPatch<EnderDragon> implements InverseKinematicsSimulatable, BossPatch<EnderDragon> {
	public static final TargetingConditions DRAGON_TARGETING = TargetingConditions.forCombat().ignoreLineOfSight();
	
	private final Map<LivingMotions, AnimationAccessor<? extends StaticAnimation>> livingMotions = Maps.newHashMap();
	private final Object2IntMap<Player> contributors = new Object2IntOpenHashMap<>();
	private boolean groundPhase;
	public LivingMotion prevMotion = LivingMotions.FLY;
	
	private float xRoot;
	private float xRootO;
	private float zRoot;
	private float zRootO;
	
	public EnderDragonPatch(EnderDragon entity) {
		super(entity);
		
		this.livingMotions.put(LivingMotions.IDLE, Animations.DRAGON_IDLE);
		this.livingMotions.put(LivingMotions.WALK, Animations.DRAGON_WALK);
		this.livingMotions.put(LivingMotions.FLY, Animations.DRAGON_FLY);
		this.livingMotions.put(LivingMotions.CHASE, Animations.DRAGON_AIRSTRIKE);
		this.livingMotions.put(LivingMotions.DEATH, Animations.DRAGON_DEATH);
		this.currentLivingMotion = LivingMotions.FLY;
	}
	
	@Override
	public void onStartTracking(ServerPlayer trackingPlayer) {
		if (this.getBossEvent() != null) {
			this.recordBossEventOwner(trackingPlayer);
		}
	}
	
	@Override
	public void onStopTracking(ServerPlayer trackingPlayer) {
		if (this.getBossEvent() != null) {
			this.removeBossEventOwner(trackingPlayer);
		}
	}
	
	@Override @ClientOnly
	public void entityPairing(SPEntityPairingPacket packet) {
		super.entityPairing(packet);
		
		if (packet.pairingPacketType() == EntityPairingPacketTypes.SET_BOSS_EVENT_OWNER) {
			this.processOwnerRecordPacket(packet.buffer());
		}
	}
	
	@Override
    public void onJoinWorld(EnderDragon entity, Level level, boolean worldgenSpawn) {
		super.onJoinWorld(entity, level, worldgenSpawn);
		
		DragonPhaseInstance currentPhase = this.original.phaseManager.getCurrentPhase();
		EnderDragonPhase<?> startPhase = (currentPhase == null || !(currentPhase instanceof PatchedDragonPhase)) ? PatchedPhases.FLYING : this.original.phaseManager.getCurrentPhase().getPhase();
		this.original.phaseManager = new PhaseManagerPatch(this.original, this);
		this.original.phaseManager.setPhase(startPhase);
	}
	
	public static void initAttributes(EntityAttributeModificationEvent event) {
		event.add(EntityType.ENDER_DRAGON, EpicFightAttributes.IMPACT, 8.0D);
		event.add(EntityType.ENDER_DRAGON, EpicFightAttributes.MAX_STRIKES, Double.MAX_VALUE);
		event.add(EntityType.ENDER_DRAGON, Attributes.ATTACK_DAMAGE, 10.0D);
		event.add(EntityType.ENDER_DRAGON, Attributes.STEP_HEIGHT, 1.0F);
	}
	
	@Override
	public void initAnimator(Animator animator) {
		super.initAnimator(animator);
		
		for (Map.Entry<LivingMotions, AnimationAccessor<? extends StaticAnimation>> livingmotionEntry : this.livingMotions.entrySet()) {
			animator.addLivingAnimation(livingmotionEntry.getKey(), livingmotionEntry.getValue());
		}
	}
	
	@Override
	public void updateMotion(boolean considerInaction) {
		if (this.original.getHealth() <= 0.0F) {
			currentLivingMotion = LivingMotions.DEATH;
		} else if (this.state.inaction() && considerInaction) {
			this.currentLivingMotion = LivingMotions.INACTION;
		} else {
			DragonPhaseInstance phase = this.original.getPhaseManager().getCurrentPhase();
			
			if (!this.groundPhase) {
				if (phase.getPhase() == PatchedPhases.AIRSTRIKE && ((DragonAirstrikePhase)phase).isActuallyAttacking()) {
					this.currentLivingMotion = LivingMotions.CHASE;
				} else {
					this.currentLivingMotion = LivingMotions.FLY;
				}
			} else {
				if (phase.getPhase() == PatchedPhases.GROUND_BATTLE) {
					if (this.original.getTarget() != null) {
						this.currentLivingMotion = LivingMotions.WALK;
					} else {
						this.currentLivingMotion = LivingMotions.IDLE;
					}
				} else {
					this.currentLivingMotion = LivingMotions.IDLE;
				}
			}
		}
	}
	
	@Override
	public void preTick() {
		super.preTick();
		
		if (this.original.getPhaseManager().getCurrentPhase().isSitting()) {
			this.original.nearestCrystal = null;
		}
	}
	
	@Override
	public void poseTick(DynamicAnimation animation, Pose pose, float elapsedTime, float partialTicks) {
		if (animation instanceof InverseKinematicsProvider inverseKinematicsProvider) {
			if (animation.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).isEmpty()) {
				return;
			}
			
			float x = (float)this.getOriginal().getX();
	    	float y = (float)this.getOriginal().getY();
	    	float z = (float)this.getOriginal().getZ();
	    	float xo = (float)this.getOriginal().xo;
	    	float yo = (float)this.getOriginal().yo;
	    	float zo = (float)this.getOriginal().zo;
	    	OpenMatrix4f toModelPos = OpenMatrix4f.mul(OpenMatrix4f.translate(new Vec3f(xo + (x - xo) * partialTicks, yo + (y - yo) * partialTicks, zo + (z - zo) * partialTicks), new OpenMatrix4f(), null), this.getModelMatrix(partialTicks), null).invert();
	    	
	    	if (pose.hasTransform("Root")) {
	    		inverseKinematicsProvider.correctRootRotation(pose.get("Root"), this, partialTicks);
	    	}
	    	
	    	animation.getProperty(StaticAnimationProperty.BAKED_IK_DEFINITION).ifPresent((ikDefinitions) -> {
	    		for (BakedInverseKinematicsDefinition bakedIKInfo : ikDefinitions) {
		    		if (!this.ikSimulator.isRunning(bakedIKInfo.endJoint())) continue;
		    		
		    		for (String jointName : bakedIKInfo.pathToEndJoint()) {
						pose.putJointData(jointName, animation.getTransfroms().get(jointName).getKeyframes()[bakedIKInfo.initialPoseFrame()].transform().copy());
					}
		    		
		    		InverseKinematicsSimulator.InverseKinematicsObject ikObject = this.ikSimulator.getRunningObject(bakedIKInfo.endJoint()).get();
		    		JointTransform jt = ikObject.getTipTransform(partialTicks);
			    	Vec3f jointModelpos = OpenMatrix4f.transform3v(toModelPos, jt.translation(), null);
			    	inverseKinematicsProvider.applyFabrikToJoint(jointModelpos.multiply(-1.0F, 1.0F, -1.0F), pose, this.getArmature(), bakedIKInfo.startJoint(), bakedIKInfo.endJoint(), jt.rotation());
		    	}
	    	});
		}
	}
	
	@Override
	public void preTickServer() {
		super.preTickServer();

		this.original.hurtTime = 2;
		this.original.getSensing().tick();
		this.updateMotion(true);
		
		if (this.prevMotion != this.currentLivingMotion && !this.animator.getEntityState().inaction()) {
			if (this.livingMotions.containsKey(this.currentLivingMotion)) {
				this.animator.playAnimation(this.livingMotions.get(this.currentLivingMotion), 0.0F);
			}
			
			this.prevMotion = this.currentLivingMotion;
		}
		
		this.ikSimulator.tick(null);
		this.setIKHeightAndRootRotation();
		
		Entity bodyPart = ((net.minecraft.world.entity.boss.enderdragon.EnderDragon) this.original).getSubEntities()[2];
		AABB bodyBoundingBox = bodyPart.getBoundingBox();
		List<Entity> list = this.original.level().getEntities(this.original, bodyBoundingBox, EntitySelector.pushableBy(this.original));
		
		if (!list.isEmpty()) {
			for (int l = 0; l < list.size(); ++l) {
				Entity entity = list.get(l);
				double d0 = entity.getX() - this.original.getX();
				double d1 = entity.getZ() - this.original.getZ();
				double d2 = Mth.absMax(d0, d1);
				
				if (d2 >= 0.01D) {
					d2 = Math.sqrt(d2);
					d0 = d0 / d2;
					d1 = d1 / d2;
					double d3 = 1.0D / d2;
					
					if (d3 > 1.0D) {
						d3 = 1.0D;
					}
					
					d0 = d0 * d3 * 0.2D;
					d1 = d1 * d3 * 0.2D;
					
					if (!entity.isVehicle()) {
						entity.push(d0, 0.0D, d1);
						entity.hurtMarked = true;
					}
				}
			}
		}

		this.contributors.object2IntEntrySet().removeIf((entry) -> this.original.tickCount - entry.getIntValue() > 600 || !entry.getKey().isAlive());
	}
	
	@Override
	public void preTickClient() {
		this.xRootO = this.xRoot;
		this.zRootO = this.zRoot;
		
		super.preTickClient();
		
		this.ikSimulator.tick(null);
		this.setIKHeightAndRootRotation();
	}
	
	@Override
	public void damageStunShield(float damage, float impact) {
		super.damageStunShield(damage, impact);
		
		if (this.getStunShield() <= 0) {
			DragonPhaseInstance currentPhase = this.original.getPhaseManager().getCurrentPhase();
			
			if (currentPhase.getPhase() == PatchedPhases.CRYSTAL_LINK && ((DragonCrystalLinkPhase)currentPhase).getChargingCount() > 0) {
				this.original.playSound(EpicFightSounds.NEUTRALIZE_BOSSES.get(), 5.0F, 1.0F);
				this.original.getPhaseManager().setPhase(PatchedPhases.NEUTRALIZED);
			}
		}
	}
	
	@Override
	public AttackResult tryHurt(DamageSource damageSource, float amount) {
		boolean isConsumingCrystal = this.original.getPhaseManager().getCurrentPhase().getPhase() == PatchedPhases.CRYSTAL_LINK;

		if (!isConsumingCrystal && amount > 0.0F && damageSource.getEntity() instanceof Player player) {
			this.contributors.put(player, this.original.tickCount);
		}

		return super.tryHurt(damageSource, isConsumingCrystal ? 0.0F : amount);
	}
	
	@Override
	public void rotateTo(@NotNull Entity target, float limit, boolean partialSync) {
		double d0 = target.getX() - this.original.getX();
        double d1 = target.getZ() - this.original.getZ();
        float degree = 180.0F - (float)Math.toDegrees(Mth.atan2(d0, d1));
    	super.rotateTo(degree, limit, partialSync);
	}
	
	@Override
	public float getYRotDeltaTo(@NotNull Entity target) {
		double d0 = target.getX() - this.original.getX();
        double d1 = target.getZ() - this.original.getZ();
        float degree = 180.0F - (float)Math.toDegrees(Mth.atan2(d0, d1));
		return Mth.clamp(Mth.wrapDegrees(degree - Mth.wrapDegrees(this.getOriginal().getYRot())), -this.getYRotLimit(), this.getYRotLimit());
	}
	
	@Override
	public void onDeath(DamageSource damageSource) {
		super.onDeath(damageSource);

		for (Player player : this.contributors.keySet()) {
			ItemStack skillbook = new ItemStack(EpicFightItems.SKILLBOOK.get());
			ItemStack modified = SetSkillFunction.builder(EpicFightSkills.DEMOLITION_LEAP.getRegisteredName())
				.build()
				.apply(skillbook,
					new LootContext.Builder(
						new LootParams.Builder(((ServerPlayer)player).serverLevel())
							.withParameter(LootContextParams.THIS_ENTITY, this.original)
							.withParameter(LootContextParams.ORIGIN, player.position())
							.create(LootContextParamSets.ADVANCEMENT_ENTITY)
					)
					.create(Optional.empty())
				);
			
			if (!modified.is(Items.AIR)) {
				player.addItem(modified);
			}
		}
	}
	
	public void setIKHeightAndRootRotation() {
		this.ikSimulator.getAllRunningObjects().stream().map((pair) -> pair.getRight()).filter(InverseKinematicsSimulator.InverseKinematicsObject::isOnWorking).forEach(InverseKinematicsSimulator.InverseKinematicsObject::tick);
		
		if (
			this.ikSimulator.isRunning(Armatures.DRAGON.get().legFrontL3) &&
			this.ikSimulator.isRunning(Armatures.DRAGON.get().legFrontR3) &&
			this.ikSimulator.isRunning(Armatures.DRAGON.get().legBackL3) &&
			this.ikSimulator.isRunning(Armatures.DRAGON.get().legBackR3)
		) {
			InverseKinematicsSimulator.InverseKinematicsObject frontL = this.ikSimulator.getRunningObject(Armatures.DRAGON.get().legFrontL3).get();
			InverseKinematicsSimulator.InverseKinematicsObject frontR = this.ikSimulator.getRunningObject(Armatures.DRAGON.get().legFrontR3).get();
			InverseKinematicsSimulator.InverseKinematicsObject backL = this.ikSimulator.getRunningObject(Armatures.DRAGON.get().legBackL3).get();
			InverseKinematicsSimulator.InverseKinematicsObject backR = this.ikSimulator.getRunningObject(Armatures.DRAGON.get().legBackR3).get();
			
			float entityPosY = (float)this.original.position().y;
			float yFrontL = (frontL != null && frontL.isTouchingGround()) ? frontL.getDestination().y : entityPosY;
			float yFrontR = (frontR != null && frontR.isTouchingGround()) ? frontR.getDestination().y : entityPosY;
			float yBackL = (backL != null && backL.isTouchingGround()) ? backL.getDestination().y : entityPosY;
			float yBackR = (backR != null && backR.isTouchingGround()) ? backR.getDestination().y : entityPosY;
			float xdiff = (yFrontL + yBackL) * 0.5F - (yFrontR + yBackR) * 0.5F;
			float zdiff = (yFrontL + yFrontR) * 0.5F - (yBackL + yBackR) * 0.5F;
			float xdistance = 4.0F;
			float zdistance = 5.7F;
			this.xRoot += Mth.clamp(((float)Math.toDegrees(Math.atan2(zdiff, zdistance)) - this.xRoot), -1.0F, 1.0F);
			this.zRoot += Mth.clamp(((float)Math.toDegrees(Math.atan2(xdiff, xdistance)) - this.zRoot), -1.0F, 1.0F);
			float averageY = (yFrontL + yFrontR + yBackL + yBackR) * 0.25F;
			
			if (!this.isLogicalClient()) {
				float dy = averageY - entityPosY;
				this.original.move(MoverType.SELF, new Vec3(0.0F, dy, 0.0F));
			}
		}
	}
	
	public int getNearbyCrystals() {
		return this.original.getDragonFight() != null ? this.original.getDragonFight().getCrystalsAlive() : 0;
	}
	
	public void setFlyingPhase() {
		this.groundPhase = false;
		this.original.horizontalCollision = false;
		this.original.verticalCollision = false;
	}
	
	public void setGroundPhase() {
		this.groundPhase = true;
	}
	
	public boolean isGroundPhase() {
		return this.groundPhase;
	}
	
	@Override
	public boolean shouldMoveOnCurrentSide(ActionAnimation actionAnimation) {
		return true;
	}
	
	@Override @ClientOnly
	public boolean isOutlineVisible(LocalPlayer player) {
		return false;
	}
	
	@Override
	public SoundEvent getSwingSound(InteractionHand hand) {
		return EpicFightSounds.WHOOSH_BIG.get();
	}
	
	@Override
	public AnimationAccessor<? extends StaticAnimation> getHitAnimation(StunType stunType) {
		return null;
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTick) {
		return MathUtils.getModelMatrixIntegral(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, this.original.yRotO, this.original.getYRot(), partialTick, -1.0F, 1.0F, -1.0F);
	}
	
	@Override
	public double getAngleTo(Entity entityIn) {
		Vec3 a = this.original.getLookAngle().scale(-1.0D);
		Vec3 b = new Vec3(entityIn.getX() - this.original.getX(), entityIn.getY() - this.original.getY(), entityIn.getZ() - this.original.getZ()).normalize();
		double cosTheta = (a.x * b.x + a.y * b.y + a.z * b.z);
		
		return Math.toDegrees(Math.acos(cosTheta));
	}
	
	@Override
	public double getAngleToHorizontal(Entity entityIn) {
		Vec3 a = this.original.getLookAngle().scale(-1.0D);
		Vec3 b = new Vec3(entityIn.getX() - this.original.getX(), 0.0D, entityIn.getZ() - this.original.getZ()).normalize();
		double cos = (a.x * b.x + a.y * b.y + a.z * b.z);
		
		return Math.toDegrees(Math.acos(cos));
	}
	
	private final InverseKinematicsSimulator ikSimulator = new InverseKinematicsSimulator();
	
	@Override
	public <SIM extends PhysicsSimulator<?, ?, ?, ?, ?>> Optional<SIM> getSimulator(SimulationTypes<?, ?, ?, ?, ?, SIM> simulationType) {
		if (simulationType == SimulationTypes.INVERSE_KINEMATICS) {
			Optional.of(this.ikSimulator);
		}
		
		return Optional.empty();
	}
	
	@Override
	public InverseKinematicsSimulator getIKSimulator() {
		return this.ikSimulator;
	}
	
	@Override
	public Entity toEntity() {
		return this.getOriginal();
	}
	
	@Override
	public float getRootXRot() {
		return this.xRoot;
	}
	
	@Override
	public float getRootXRotO() {
		return this.xRootO;
	}
	
	@Override
	public float getRootZRot() {
		return this.zRoot;
	}
	
	@Override
	public float getRootZRotO() {
		return this.zRootO;
	}
	
	@Override
	public BossEvent getBossEvent() {
		/**
		 * A copy from {@link EnderDragon#aiStep()}
		**/
		if (this.original.getDragonFight() == null && !this.original.level().isClientSide) {
			ServerLevel serverlevel = (ServerLevel)this.original.level();
            EndDragonFight enddragonfight = serverlevel.getDragonFight();
            
            if (enddragonfight != null && this.original.getUUID().equals(enddragonfight.getDragonUUID())) {
            	this.original.setDragonFight(enddragonfight);
            }
		}
		
		return this.original.getDragonFight() == null ? null : ((yesman.epicfight.mixin.common.EndDragonFightAccessor) this.original.getDragonFight()).epicfight$getDragonEvent();
	}
}