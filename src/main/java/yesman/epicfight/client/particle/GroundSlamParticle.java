package yesman.epicfight.client.particle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.TerrainParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.world.level.block.FractureBlockState;

import java.util.Random;

public class GroundSlamParticle extends NoRenderParticle {
	protected GroundSlamParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, BlockPos bp, BlockState bs) {
		super(level, x, y, z, dx, dy, dz);
		
		if (bs.isAir()) {
			bs = level.getBlockState(bp.below()); // one more step chance
		}
		
		if (bs instanceof FractureBlockState fractureBlockState) {
			bs = fractureBlockState.getOriginalBlockState(bp);
			
			if (bs == null) {
				bs = level.getBlockState(bp);
			}
		}
		
		if (!bs.shouldSpawnTerrainParticles()) {
			return;
		}
		
		Minecraft mc = Minecraft.getInstance();
		
		for (int i = 0; i < (int)dy; i ++) {
			OpenMatrix4f mat = OpenMatrix4f.createRotatorDeg((float)Math.random() * 360.0F, Vec3f.Y_AXIS);
			Vec3f positionVec = OpenMatrix4f.transform3v(mat, Vec3f.Z_AXIS, null).scale((float)dx);
			Vec3f moveVec = OpenMatrix4f.transform3v(mat, Vec3f.Z_AXIS, null).scale((float)dz);
			
			Particle blockParticle = new TerrainParticle(level, x + positionVec.x, y, z + positionVec.z, 0, 0, 0, bs, bp);
			blockParticle.setParticleSpeed((moveVec.x + (Math.random() - 0.5)) * 0.3D, (Math.random()) * 0.5D, (moveVec.z + (Math.random() - 0.5)) * 0.3D);
			blockParticle.setLifetime(60 + (new Random().nextInt(20)));
			
			Particle smokeParticle = mc.particleEngine.createParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x + positionVec.x * 0.5D, y + 1.5D, z + positionVec.z * 0.5D, 0, 0, 0); 
			smokeParticle.setParticleSpeed(moveVec.x * 0.1D, Math.random() * 0.05D, moveVec.z * 0.1D);
			smokeParticle.scale(3.0F);
			smokeParticle.alpha = 0.33F;
			mc.particleEngine.add(blockParticle);
			mc.particleEngine.add(smokeParticle);
		}
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		@Override
		public Particle createParticle(SimpleParticleType particleOption, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			BlockPos blockpos = new BlockPos.MutableBlockPos(x, y, z);
			BlockState blockstate = level.getBlockState(blockpos);
			if (blockstate == null) return null; 
			
			return new GroundSlamParticle(level, x, y, z, xSpeed, ySpeed, zSpeed, blockpos, blockstate);
		}
	}

    public static class BlockParticleProvider implements ParticleProvider<BlockParticleOption> {
        @Override
        public @Nullable Particle createParticle(BlockParticleOption particleOption, ClientLevel clientLevel, double x, double y, double z, double dx, double dy, double dz) {
            TerrainParticle blockParticle = new TerrainParticle(clientLevel, x, y, z, dx, dy, dz, particleOption.getState(), new net.minecraft.core.BlockPos((int)x, (int)y, (int)z));
            blockParticle.setParticleSpeed((Math.random() - 0.5D) * 0.3D, Math.random() * 0.5D, (Math.random() - 0.5D) * 0.3D);
            blockParticle.setLifetime(10 + new Random().nextInt(60));

            return blockParticle;
        }
    }
}