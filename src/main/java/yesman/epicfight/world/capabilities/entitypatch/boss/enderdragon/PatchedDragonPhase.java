package yesman.epicfight.world.capabilities.entitypatch.boss.enderdragon;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.enderdragon.phases.AbstractDragonPhaseInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.EndPodiumFeature;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;

public abstract class PatchedDragonPhase extends AbstractDragonPhaseInstance {
	protected final EnderDragonPatch dragonpatch;
	
	public PatchedDragonPhase(EnderDragon dragon) {
		super(dragon);
		this.dragonpatch = EpicFightCapabilities.getEntityPatch(dragon, EnderDragonPatch.class);
	}
	
	@Override
	public void doClientTick() {
		this.dragon.oFlapTime = 0.5F;
		this.dragon.flapTime = 0.5F;
	}
	
	protected static boolean isValidTarget(LivingEntity entity) {
		return entity.canBeSeenAsEnemy();
	}
	
	protected static boolean isInEndSpikes(LivingEntity entity) {
		BlockPos blockpos = entity.level().getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EndPodiumFeature.getLocation(new BlockPos(0, 0, 0)));
		return blockpos.distSqr(new BlockPos.MutableBlockPos(entity.getX(), blockpos.getY(), entity.getZ())) < 2000.0D;
	}
	
	protected List<Player> getPlayersNearbyWithin(double within) {
		return this.dragon.level().getNearbyPlayers(EnderDragonPatch.DRAGON_TARGETING, this.dragon, this.dragon.getBoundingBox().inflate(within, within, within));
	}
}