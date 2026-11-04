package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.DragonPhaseInstance;
import net.minecraft.world.entity.boss.enderdragon.phases.EnderDragonPhase;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.gameasset.Animations;

public class DragonLandingPhase extends PatchedDragonPhase {
	private final BlockPos[] landingCandidates;
	private Vec3 landingPosition;
	private boolean actualLandingPhase;
	
	public DragonLandingPhase(EnderDragon enderdragon) {
		super(enderdragon);
		this.landingCandidates = new BlockPos[3];
		this.landingCandidates[0] = enderdragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(-3, 0, -11));
		this.landingCandidates[1] = enderdragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(17, 0, 0));
		this.landingCandidates[2] = enderdragon.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(0, 0, 17));
	}
	
	@Override
	public void begin() {
		this.actualLandingPhase = false;
		this.landingPosition = this.getFarthestLandingPosition();
		
		if (!this.dragonpatch.isLogicalClient()) {
			this.dragonpatch.getOriginal().getPhaseManager().getPhase(PatchedPhases.GROUND_BATTLE).resetFlyCooldown();
		}
	}
	
	public Vec3 getFarthestLandingPosition() {
		double max = 0.0D;
		Vec3 result = null;
		
		for (int i = 0; i < this.landingCandidates.length; i++) {
			Vec3 vec3d = new Vec3(this.landingCandidates[i].getX(), this.landingCandidates[i].getY(), this.landingCandidates[i].getZ());
			double distanceSqr = vec3d.distanceToSqr(this.dragon.position());
			
			if (distanceSqr > max) {
				max = distanceSqr;
				result = vec3d;
			}
		}
		
		return result;
	}
	
	@Override
	public void doServerTick() {
		double dx = this.landingPosition.x - this.dragon.getX();
		double dy = this.landingPosition.y - this.dragon.getY();
		double dz = this.landingPosition.z - this.dragon.getZ();
		double squaredD = dx * dx + dy * dy + dz * dz;
		double squaredHorizontalD = dx * dx + dz * dz;
		
		if (this.actualLandingPhase) {
			if (squaredHorizontalD < 50.0D) {
				this.dragon.getPhaseManager().setPhase(PatchedPhases.GROUND_BATTLE);
			}
		} else {
			float f5 = this.getFlySpeed();
			double horizontalD = Math.sqrt(squaredHorizontalD);
			double yMove = dy;
			
			if (horizontalD > 0.0D) {
				yMove = Mth.clamp(dy, -f5, f5) * Mth.clamp((Math.abs(dy) - 13.0D) * 0.01D, 0.01D, 0.03D);
			}
			
			this.dragon.setDeltaMovement(this.dragon.getDeltaMovement().add(0.0D, yMove, 0.0D));
			this.dragon.setYRot(Mth.wrapDegrees(this.dragon.getYRot()));
			Vec3 vec32 = this.landingPosition.subtract(this.dragon.getX(), this.dragon.getY(), this.dragon.getZ()).normalize();
			Vec3 vec33 = (new Vec3(Mth.sin(this.dragon.getYRot() * ((float) Math.PI / 180F)), this.dragon.getDeltaMovement().y, -Mth.cos(this.dragon.getYRot() * ((float) Math.PI / 180F)))).normalize();
			float f6 = Math.max(((float) vec33.dot(vec32) + 0.5F) / 1.5F, 0.0F);
			
			if (Math.abs(dx) > (double) 1.0E-5F || Math.abs(dz) > (double) 1.0E-5F) {
				double d5 = Mth.clamp(Mth.wrapDegrees(180.0D - Mth.atan2(dx, dz) * (double) (180F / (float) Math.PI) - (double) this.dragon.getYRot()), -50.0D, 50.0D);
				this.dragon.yRotA *= 0.8F;
				this.dragon.yRotA = (float) ((double) this.dragon.yRotA + d5 * (double) this.getTurnSpeed());
				this.dragon.setYRot(this.dragon.getYRot() + this.dragon.yRotA * 0.1F);
			}
			
			float f18 = (float) (2.0D / (squaredD + 1.0D));
			this.dragon.moveRelative(0.06F * (f6 * f18 + (1.0F - f18)), new Vec3(0.0D, 0.0D, -1.0D));
			
			if (this.dragon.inWall) {
				this.dragon.move(MoverType.SELF, this.dragon.getDeltaMovement().scale(0.8F));
			} else {
				this.dragon.move(MoverType.SELF, this.dragon.getDeltaMovement());
			}
			
			Vec3 vec34 = this.dragon.getDeltaMovement().normalize();
			double d6 = (0.8D + 0.15D * (vec34.dot(vec33) + 1.0D) / 2.0D);
			this.dragon.setDeltaMovement(this.dragon.getDeltaMovement().multiply(d6, 0.91F, d6));
			
			if (squaredD < 400.0D && Math.abs(dy) < 14.0D && (new Vec3(dx, 0, dz)).normalize().dot(Vec3.directionFromRotation(new Vec2(0, 180 + this.dragon.getYRot()))) > 0.95D) {
				this.dragonpatch.playAnimationSynchronized(Animations.DRAGON_FLY_TO_GROUND, 0.0F);
				this.actualLandingPhase = true;
			}
		}
	}
	
	public Vec3 getLandingPosition() {
		return this.landingPosition;
	}
	
	@Override
	public EnderDragonPhase<? extends DragonPhaseInstance> getPhase() {
		return PatchedPhases.LANDING;
	}
}