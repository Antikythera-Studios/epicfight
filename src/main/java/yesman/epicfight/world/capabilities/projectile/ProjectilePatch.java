package yesman.epicfight.world.capabilities.projectile;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.registry.entries.EpicFightAttributes;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;
import yesman.epicfight.world.capabilities.item.CapabilityItem.Styles;
import yesman.epicfight.world.capabilities.item.RangedWeaponCapability;
import yesman.epicfight.world.damagesource.EpicFightDamageSource;

import java.util.Map;

public abstract class ProjectilePatch<T extends Projectile> extends EntityPatch<T> {
	protected float impact;
	protected float armorNegation;
	protected Vec3 initialFirePosition;
	protected boolean hasHit;
	
	public ProjectilePatch(T original) {
		super(original);
	}

    @Override
    public void onJoinWorld(T entity, Level level, boolean worldgenSpawn) {
        super.onJoinWorld(entity, level, worldgenSpawn);

		Entity shooter = entity.getOwner();
		boolean flag = true;
		
		if (shooter != null && shooter instanceof LivingEntity livingshooter) {
			this.initialFirePosition = shooter.position();
			ItemStack heldItem = livingshooter.getMainHandItem();
			CapabilityItem itemCap = EpicFightCapabilities.getItemStackCapability(heldItem);
			
			if (itemCap instanceof RangedWeaponCapability) {
				Map<Holder<Attribute>, AttributeModifier> modifierMap = itemCap.getDamageAttributesInCondition(Styles.RANGED);
				
				if (modifierMap != null) {
					this.armorNegation = 
						modifierMap.containsKey(EpicFightAttributes.ARMOR_NEGATION) ?
							(float)modifierMap.get(EpicFightAttributes.ARMOR_NEGATION).amount()
								: (float)EpicFightAttributes.ARMOR_NEGATION.value().getDefaultValue();
					this.impact =
						modifierMap.containsKey(EpicFightAttributes.IMPACT) ?
							(float)modifierMap.get(EpicFightAttributes.IMPACT).amount()
								: (float)EpicFightAttributes.IMPACT.value().getDefaultValue();
					
					if (modifierMap.containsKey(EpicFightAttributes.MAX_STRIKES)) {
						this.setMaxStrikes(entity, (int)modifierMap.get(EpicFightAttributes.MAX_STRIKES).amount());
					}
				}
				
				flag = false;
			}
		}
		
		if (flag) {
			this.armorNegation = 0.0F;
			this.impact = 0.0F;
		}
	}
	
	@Override
	public void onAddedToLevel() {
		if (this.getOriginal().level().isClientSide()) {
			double entityId = Double.longBitsToDouble((long)this.getOriginal().getId());
			yesman.epicfight.client.particle.AnimationTrailParticle.spawnByProvider(this.getOriginal(), EpicFightParticles.PROJECTILE_TRAIL.get(), entityId, 0, 0, 0, 0, 0);
		}
	}
	
	/**
	 * @return true if event should be canceled
	 */
	public boolean onProjectileImpact(HitResult hitResult) {
		return false;
	}
	
	protected abstract void setMaxStrikes(T projectileEntity, int maxStrikes);
	public abstract EpicFightDamageSource createEpicFightDamageSource();
	
	@Override
	public boolean overrideRender() {
		return false;
	}
	
	@Override
	public OpenMatrix4f getModelMatrix(float partialTicks) {
		return super.getMatrix(partialTicks);
	}
	
	public void setHit(boolean hit) {
		this.hasHit = hit;
	}
	
	public boolean hit() {
		return this.hasHit;
	}
}
