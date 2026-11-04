package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

public class DragonFlyingPhase extends PatchedDragonPhase {
	private Path currentPath;
	private Vec3 targetLocation;
	private boolean clockwise;
	private boolean executeAirstrike;
	
	public DragonFlyingPhase(EnderDragon p_31230_) {
		super(p_31230_);
	}
	
	@Override
	public EnderDragonPhase<DragonFlyingPhase> getPhase() {
		return PatchedPhases.FLYING;
	}
	
	@Override
	public void doServerTick() {
		double d0 = this.targetLocation == null ? 0.0D : this.targetLocation.distanceToSqr(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ());
		
		if (d0 < 100.0D || d0 > 22500.0D || this.dragon.horizontalCollision || this.dragon.verticalCollision && this.dragon.getDragonFight() != null) {
			this.findNewTarget();
		}
	}
	
	@Override
	public void begin() {
		this.currentPath = null;
		this.targetLocation = null;
	}
	
	@Nullable@Override
	public Vec3 getFlyTargetLocation() {
		return this.dragonpatch.getEntityState().inaction() ? null : this.targetLocation;
	}
	
	public void enableAirstrike() {
		this.executeAirstrike = false;
	}
	
	private void findNewTarget() {
		if (this.currentPath != null && this.currentPath.isDone()) {
			List<Player> players = this.getPlayersNearbyWithin(100.0D);
			
			for (Player player : players) {
				if (isValidTarget(player)) {
					if (!this.executeAirstrike && this.dragon.getRandom().nextFloat() > this.dragonpatch.getNearbyCrystals() * 0.1F) {
						if (isInEndSpikes(player)) {
							this.executeAirstrike = true;
						}
						
						this.dragonpatch.setAttakTargetSync(player);
						this.dragon.getPhaseManager().setPhase(PatchedPhases.AIRSTRIKE);
					} else if (isInEndSpikes(player)) {
						this.dragon.getPhaseManager().setPhase(PatchedPhases.LANDING);
					}
					
					return;
				}
			}
		}
		
		if (this.currentPath == null || this.currentPath.isDone()) {
			int j = this.dragon.findClosestNode();
			int k = j;
			
			if (this.dragon.getRandom().nextInt(8) == 0) {
				this.clockwise = !this.clockwise;
				k = j + 6;
			}
			
			if (this.clockwise) {
				++k;
			} else {
				--k;
			}
			
			if (this.dragon.getDragonFight() != null && this.dragonpatch.getNearbyCrystals() >= 0) {
				k = k % 12;
				if (k < 0) {
					k += 12;
				}
			} else {
				k = k - 12;
				k = k & 7;
				k = k + 12;
			}
			
			this.currentPath = this.dragon.findPath(j, k, null);
			
			if (this.currentPath != null) {
				this.currentPath.advance();
			}
		}

		this.navigateToNextPathNode();
	}
	
	private void navigateToNextPathNode() {
		if (this.currentPath != null && !this.currentPath.isDone()) {
			Vec3i vec3i = this.currentPath.getNextNodePos();
			this.currentPath.advance();
			double d0 = vec3i.getX();
			double d1 = vec3i.getZ();
			double d2;
			
			do {
				d2 = (float) vec3i.getY() + this.dragon.getRandom().nextFloat() * 20.0F;
			} while (d2 < (double) vec3i.getY());
			
			this.targetLocation = new Vec3(d0, d2, d1);
		}
	}
}