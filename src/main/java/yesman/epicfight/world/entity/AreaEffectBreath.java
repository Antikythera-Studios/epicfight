package yesman.epicfight.world.entity;

import java.util.List;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;
import yesman.epicfight.world.damagesource.EpicFightDamageSources;
import yesman.epicfight.world.damagesource.StunType;

public class AreaEffectBreath extends AreaEffectCloud {
	private static final EntityDataAccessor<Boolean> DATA_HORIZONTAL = SynchedEntityData.defineId(AreaEffectBreath.class, EntityDataSerializers.BOOLEAN);
	private Vec3 initialFirePosition;
	
	public AreaEffectBreath(EntityType<? extends AreaEffectBreath> entityType, Level level) {
		super(entityType, level);
		this.setDuration(5);
		this.addEffect(new MobEffectInstance(MobEffects.HARM, 1, 1));
	}
	
	public AreaEffectBreath(Level level, double x, double y, double z) {
		this(EpicFightEntityTypes.AREA_EFFECT_BREATH.get(), level);
		this.setPos(x, y, z);
		this.initialFirePosition = new Vec3(x, y, z);
	}
	
	@Override
	public void tick() {
		this.move(MoverType.SELF, this.getDeltaMovement());
		
		if (!this.level().isClientSide()) {
			if (this.tickCount >= this.getDuration()) {
				this.discard();
				return;
			}
			
			float f = this.getRadius();
			float radiusPerTick = this.getRadiusPerTick();
			
			if (radiusPerTick != 0.0F) {
				f += radiusPerTick;
				if (f < 0.5F) {
					this.discard();
					return;
				}
				
				this.setRadius(f);
			}
			
			((yesman.epicfight.mixin.common.AreaEffectCloudAccessor) this).epicfight$getVictims().entrySet().removeIf((p_146784_) -> {
				return this.tickCount >= p_146784_.getValue();
			});
			
			List<LivingEntity> list1 = this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox());
			
			if (!list1.isEmpty()) {
				for (LivingEntity livingentity : list1) {
					if (livingentity.is(this.getOwner())) {
						continue;
					}
					
					EpicFightDamageSource damageSource = EpicFightDamageSources.enderDragonBreath(this.getOwner(), this).setStunType(StunType.SHORT);
					
					if (livingentity.isInvulnerableTo(damageSource)) {
						continue;
					}
					
					if (!((yesman.epicfight.mixin.common.AreaEffectCloudAccessor) this).epicfight$getVictims().containsKey(livingentity) && livingentity.isAffectedByPotions()) {
						double d8 = livingentity.getX() - this.getX();
						double d1 = livingentity.getZ() - this.getZ();
						double d3 = d8 * d8 + d1 * d1;
						
						if (d3 <= (double) (f * f)) {
							((yesman.epicfight.mixin.common.AreaEffectCloudAccessor) this).epicfight$getVictims().put(livingentity, this.tickCount + 3);
							livingentity.invulnerableTime = 0;
							
							damageSource.setInitialPosition(this.initialFirePosition);
							damageSource.setBaseImpact(2.0F);
							
							livingentity.hurt(damageSource, 3.0F);
						}
					}
				}
			}
		}
	}
	
	@Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
        builder.define(DATA_HORIZONTAL, true);
    }
	
	protected void setHorizontal(boolean setter) {
		this.getEntityData().set(DATA_HORIZONTAL, setter);
	}
	
	public boolean isHorizontal() {
		return this.getEntityData().get(DATA_HORIZONTAL);
	}
	
	@Override
	public EntityDimensions getDimensions(Pose pose) {
		boolean horizontal = this.isHorizontal();
		float width = horizontal ? this.getRadius() * 2.0F : 1.0F;
		float height = horizontal ? 5.0F : this.getRadius() * 2.0F;
		
		return EntityDimensions.scalable(width, height);
	}
}