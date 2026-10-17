package yesman.epicfight.mixin.common;
import yesman.epicfight.platform.neoforged.event.EventHooks;

import java.util.List;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.boss.WitherPatch;

@Mixin(value = WitherBoss.class)
public abstract class MixinWitherBoss extends Monster implements PowerableMob, RangedAttackMob, MixinWitherBossAccessor {
	@Shadow private int[] nextHeadUpdate = new int[2];
	@Shadow private int[] idleHeadUpdates = new int[2];
	@Shadow private int destroyBlocksTick;
	@Shadow @Final private ServerBossEvent bossEvent;
	@Unique private WitherPatch epicFight$witherPatch;
	
	protected MixinWitherBoss(EntityType<? extends WitherBoss> entityType, Level level) {
		super(entityType, level);
	}
	
	@Inject(at = @At(value = "RETURN"), method = "<init>")
	private void epicfight$witherBossInit(CallbackInfo info) {
		this.epicFight$witherPatch = EpicFightCapabilities.getEntityPatch(((WitherBoss)((Object)this)), WitherPatch.class);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "aiStep()V", cancellable = true)
	private void epicfight$aiStep(CallbackInfo info) {
		if (this.epicFight$witherPatch != null) {
			info.cancel();
			
			WitherBoss self = this.epicFight$witherPatch.getOriginal();
			MixinWitherBossAccessor self$Accessor = this.epicFight$witherPatch.getOriginalAsMixinAccessor();
			super.aiStep();
			
			for (int i = 0; i < 2; ++i) {
				self$Accessor.getYRotOHeads()[i] = self$Accessor.getYRotHeads()[i];
				self$Accessor.getXRotOHeads()[i] = self$Accessor.getXRotHeads()[i];
			}
			
			for (int j = 0; j < 2; ++j) {
				int k = self.getAlternativeTarget(j + 1);
				Entity entity1 = null;
				
				if (k > 0) {
					entity1 = this.level().getEntity(k);
				}
				
				if (this.epicFight$witherPatch.getLaserTargetEntity(j + 1) != null) {
					Entity laserTarget = this.epicFight$witherPatch.getLaserTargetEntity(j + 1);
					this.lookAt(j, laserTarget.getX(), laserTarget.getEyeY(), laserTarget.getZ(), 360.0F, 360.0F);
				} else if (epicFight$isValid(this.epicFight$witherPatch.getLaserTargetPosition(j + 1))) {
					Vec3 laserTargetPosition = this.epicFight$witherPatch.getLaserTargetPosition(j + 1);
					this.lookAt(j, laserTargetPosition.x, laserTargetPosition.y, laserTargetPosition.z, 360.0F, 360.0F);
				} else if (this.epicFight$witherPatch.getEntityState().inaction()) {
					self$Accessor.getXRotHeads()[j] = this.rotlerp(self$Accessor.getXRotHeads()[j], 0.0F, 40.0F);
					self$Accessor.getYRotHeads()[j] = this.rotlerp(self$Accessor.getYRotHeads()[j], this.yBodyRot, 10.0F);
				} else if (entity1 != null) {
					this.lookAt(j, entity1.getX(), entity1.getEyeY(), entity1.getZ(), 40.0F, 10.0F);
				} else {
					self$Accessor.getYRotHeads()[j] = this.rotlerp(self$Accessor.getYRotHeads()[j], this.yBodyRot, 10.0F);
				}
			}
			
			boolean powered = this.isPowered();
			
			for (int l = 0; l < 3; ++l) {
				double subHeadX = self$Accessor.invoke_getHeadX(l);
				double subHeadY = self$Accessor.invoke_getHeadY(l);
				double subHeadZ = self$Accessor.invoke_getHeadZ(l);
				
				if (!this.epicFight$witherPatch.isGhost()) {
					this.level().addParticle(
						ParticleTypes.SMOKE,
						subHeadX + this.random.nextGaussian() * (double) 0.3F,
						subHeadY + this.random.nextGaussian() * (double) 0.3F,
						subHeadZ + this.random.nextGaussian() * (double) 0.3F,
						0.0D,
						0.0D,
						0.0D
					);
					
					if (powered && this.level().random.nextInt(4) == 0) {
						this.level().addParticle(
							ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.5F),
							subHeadX + this.random.nextGaussian() * (double) 0.3F,
							subHeadY + this.random.nextGaussian() * (double) 0.3F,
							subHeadZ + this.random.nextGaussian() * (double) 0.3F,
							0.0F,
							0.0F,
							0.0D
						);
					}
				}
			}
			
			if (self.getInvulnerableTicks() > 0) {
				for (int i1 = 0; i1 < 3; ++i1) {
					this.level().addParticle(
						ColorParticleOption.create(ParticleTypes.ENTITY_EFFECT, 0.7F, 0.7F, 0.9F),
						this.getX() + this.random.nextGaussian(),
						this.getY() + (double) (this.random.nextFloat() * 3.3F),
						this.getZ() + this.random.nextGaussian(),
						0.0F,
						0.0F,
						0.0F
					);
				}
			}
		}
	}
	
	@Unique
	private void lookAt(int head, double x, double y, double z, float lerpX, float lerpY) {
		MixinWitherBossAccessor self = this.epicFight$witherPatch.getOriginalAsMixinAccessor();
		double d9 = self.invoke_getHeadX(head + 1);
		double d1 = self.invoke_getHeadY(head + 1);
		double d3 = self.invoke_getHeadZ(head + 1);
		double d4 = x - d9;
		double d5 = y - d1;
		double d6 = z - d3;
		double d7 = Math.sqrt(d4 * d4 + d6 * d6);
		float f = (float)(Mth.atan2(d6, d4) * (180F / Math.PI)) - 90.0F;
		float f1 = (float)(-(Mth.atan2(d5, d7) * (180F / Math.PI)));
		self.getXRotHeads()[head] = this.rotlerp(self.getXRotHeads()[head], f1, lerpX);
		self.getYRotHeads()[head] = this.rotlerp(self.getYRotHeads()[head], f, lerpY);
	}
	
	@Inject(at = @At(value = "HEAD"), method = "customServerAiStep()V", cancellable = true)
	private void epicfight$customServerAiStep(CallbackInfo info) {
		if (this.epicFight$witherPatch != null) {
			info.cancel();
			WitherBoss self = this.epicFight$witherPatch.getOriginal();
			MixinWitherBossAccessor self$Accessor = this.epicFight$witherPatch.getOriginalAsMixinAccessor();
			
			if (self.getInvulnerableTicks() > 0) {
				int k1 = self.getInvulnerableTicks() - 1;
				this.bossEvent.setProgress(1.0F - (float) k1 / 220.0F);

				if (k1 <= 0) {
					Level.ExplosionInteraction explosion$blockinteraction = EventHooks.canEntityGrief(self.level(), this) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
					self.level().explode(this, self.getX(), self.getEyeY(), self.getZ(), 7.0F, false, explosion$blockinteraction);

					if (!self.isSilent()) {
						self.level().globalLevelEvent(1023, self.blockPosition(), 0);
					}
				}

				self.setInvulnerableTicks(k1);
				if (self.tickCount % 10 == 0) {
					self.heal(10.0F);
				}
			} else {
				super.customServerAiStep();

				for (int i = 1; i < 3; ++i) {
					if (self.tickCount >= this.nextHeadUpdate[i - 1]) {
						this.nextHeadUpdate[i - 1] = self.tickCount + 10 + self.getRandom().nextInt(10);

						if ((self.level().getDifficulty() == Difficulty.NORMAL || self.level().getDifficulty() == Difficulty.HARD) && !this.epicFight$witherPatch.getEntityState().inaction()) {
							int i3 = i - 1;
							int j3 = this.idleHeadUpdates[i - 1];
							this.idleHeadUpdates[i3] = this.idleHeadUpdates[i - 1] + 1;

							if (j3 > 15) {
								double d0 = Mth.nextDouble(self.getRandom(), self.getX() - 10.0D, self.getX() + 10.0D);
								double d1 = Mth.nextDouble(self.getRandom(), self.getY() - 5.0D, self.getY() + 5.0D);
								double d2 = Mth.nextDouble(self.getRandom(), self.getZ() - 10.0D, self.getZ() + 10.0D);
								self$Accessor.invoke_performRangedAttack(i + 1, d0, d1, d2, true);
								this.idleHeadUpdates[i - 1] = 0;
							}
						}
						
						int l1 = self.getAlternativeTarget(i);

						if (this.epicFight$witherPatch.getEntityState().inaction()) {
							this.nextHeadUpdate[i - 1] = self.tickCount + 30;
						}

						if (l1 > 0) {
							LivingEntity livingentity = (LivingEntity) self.level().getEntity(l1);

							if (livingentity != null && self.canAttack(livingentity) && !(self.distanceToSqr(livingentity) > 900.0D) && self.hasLineOfSight(livingentity)) {
								if (!this.epicFight$witherPatch.getEntityState().inaction()) {
									self$Accessor.invoke_performRangedAttack(i + 1, livingentity);
									this.nextHeadUpdate[i - 1] = self.tickCount + 40 + self.getRandom().nextInt(20);
									this.idleHeadUpdates[i - 1] = 0;
								}
							} else {
								self.setAlternativeTarget(i, 0);
							}
						} else {
							List<LivingEntity> list = self.level().getNearbyEntities(LivingEntity.class, WitherBoss.TARGETING_CONDITIONS, self, self.getBoundingBox().inflate(20.0D, 8.0D, 20.0D));
							
							if (!list.isEmpty()) {
								LivingEntity livingentity1 = list.get(self.getRandom().nextInt(list.size()));
								self.setAlternativeTarget(i, livingentity1.getId());
							}
						}
					}
				}

				if (self.getTarget() != null) {
					self.setAlternativeTarget(0, self.getTarget().getId());
				} else {
					self.setAlternativeTarget(0, 0);
				}

				if (this.destroyBlocksTick > 0) {
					--this.destroyBlocksTick;
					
					if (this.destroyBlocksTick == 0 && EventHooks.canEntityGrief(self.level(), this)) {
						int j1 = Mth.floor(self.getY());
						int i2 = Mth.floor(self.getX());
						int j2 = Mth.floor(self.getZ());
						boolean flag = false;

						for (int j = -1; j <= 1; ++j) {
							for (int k2 = -1; k2 <= 1; ++k2) {
								for (int k = 0; k <= 3; ++k) {
									int l2 = i2 + j;
									int l = j1 + k;
									int i1 = j2 + k2;
									BlockPos blockpos = new BlockPos(l2, l, i1);
									BlockState blockstate = self.level().getBlockState(blockpos);

									if (EventHooks.onEntityDestroyBlock(this, blockpos, blockstate)) {
										flag = self.level().destroyBlock(blockpos, true, this) || flag;
									}
								}
							}
						}

						if (flag) {
							self.level().levelEvent(null, 1022, self.blockPosition(), 0);
						}
					}
				}
				
				this.bossEvent.setProgress(self.getHealth() / self.getMaxHealth());
			}
		}
	}
	
	@Unique
	public boolean isSpectator() {
		return (this.epicFight$witherPatch != null) ? this.epicFight$witherPatch.isGhost() : super.isSpectator();
	}
	
	@Unique
	protected SoundEvent getAmbientSound() {
		return (this.epicFight$witherPatch != null) ? (this.epicFight$witherPatch.isGhost() ? null : SoundEvents.WITHER_AMBIENT) : null;
	}
	
	@Shadow
	private float rotlerp(float p_31443_, float p_31444_, float p_31445_) {
		throw new AbstractMethodError("Shadow");
	}
	
	@Unique
    private static boolean epicFight$isValid(Vec3 vec) {
		return !(Double.isNaN(vec.x)|| Double.isNaN(vec.y) || Double.isNaN(vec.z));
	}
}