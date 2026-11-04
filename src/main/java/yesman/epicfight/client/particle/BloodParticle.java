package yesman.epicfight.client.particle;

import java.util.Random;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import yesman.epicfight.config.ClientConfig;

public class BloodParticle extends TextureSheetParticle {
	protected BloodParticle(ClientLevel world, double x, double y, double z, double motionX, double motionY, double motionZ) {
		super(world, x, y, z, motionX, motionY, motionZ);
		this.x = x + (this.random.nextDouble() - 0.5D) * this.bbWidth;
		this.y = y + (this.random.nextDouble() + this.bbHeight) * 0.5D;
		this.z = z + (this.random.nextDouble() - 0.5D) * this.bbWidth;
		this.xd = motionX;
		this.yd = motionY;
		this.zd = motionZ;
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;
		
		public Provider(SpriteSet spriteSet) {
			this.spriteSet = spriteSet;
		}
		
		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			BloodParticle particle = new BloodParticle(worldIn, x, y, z, xSpeed, ySpeed, zSpeed);
			Random random = new Random();
			particle.move(0, 0.5F, 0);
			
			float mass = random.nextFloat() + 0.2F;
			particle.lifetime = 10 + (int)(mass * 10.0F);
			particle.gravity = mass * 4.0F;
			particle.yd = (0.9F - mass) * 0.4F;
			particle.setSize(mass, mass);
			particle.pickSprite(this.spriteSet);
			
			float green = ClientConfig.bloodEffects ? 0.0F : Mth.clamp(random.nextFloat(), 0.6F, 0.4F);
			particle.setColor(Mth.clamp(random.nextFloat(), 0.6F, 0.4F), green, 0.0F);
			
			return particle;
		}
	}
}