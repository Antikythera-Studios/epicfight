package yesman.epicfight.world.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.registry.entries.EpicFightEntityTypes;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.registry.entries.EpicFightSounds;

public class DroppedNetherStar extends ItemEntity {
	
	public DroppedNetherStar(EntityType<? extends DroppedNetherStar> entityType, Level level) {
		super(entityType, level);
	}
	
	public DroppedNetherStar(Level level, double x, double y, double z, ItemStack itemstack, double dx, double dy, double dz) {
		this(EpicFightEntityTypes.DROPPED_NETHER_STAR.get(), level);
		this.setPos(x, y, z);
		this.setDeltaMovement(dx, dy, dz);
		this.setItem(itemstack);
		this.setExtendedLifetime();
		this.noPhysics = true;
		this.setPickUpDelay(30);
		this.setNoGravity(true);
	}
	
	public DroppedNetherStar(Level level, Vec3 position, Vec3 deltaMovement) {
		this(level, position.x, position.y, position.z, new ItemStack(Items.NETHER_STAR), deltaMovement.x, deltaMovement.y, deltaMovement.z);
	}
	
	@Override
	public void tick() {
		super.tick();
		
		if (this.tickCount % 70 == 0) {
			this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), EpicFightSounds.NETHER_STAR_GLITTER.get(), this.getSoundSource(), 1.0F, 1.0F, false);
		}
		
		Vec3 deltaMove = this.getDeltaMovement();
		
		if (this.level().isClientSide()) {
			Vec3 particleDeltaMove = new Vec3(-deltaMove.x, -1.0D, -deltaMove.z).normalize().add((this.random.nextFloat() - 0.5F) * 0.1F, 0.0D, (this.random.nextFloat() - 0.5F) * 0.1F);
			this.level().addParticle(EpicFightParticles.NORMAL_DUST.get(), this.getX() + (this.random.nextFloat() - 0.5F) * this.getBbWidth(), this.getY() + this.getBbHeight() * 2.5D, this.getZ() + (this.random.nextFloat() - 0.5F) * this.getBbWidth(), particleDeltaMove.x, 0.0D, particleDeltaMove.z);
		}
		
		this.setDeltaMovement(deltaMove.multiply(0.68D, 0.68D, 0.68D));
	}
	
	@Override
	public boolean isOnFire() {
		return true;
	}
	
	@Override
	public boolean displayFireAnimation() {
		return false;
	}
}