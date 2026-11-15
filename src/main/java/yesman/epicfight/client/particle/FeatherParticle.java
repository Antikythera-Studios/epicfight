package yesman.epicfight.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class FeatherParticle extends TextureSheetParticle {
	protected FeatherParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
		super(level, x, y, z, dx, dy, dz);
		
		this.lifetime = 8 + this.random.nextInt(22);
		this.hasPhysics = true;
		this.gravity = 0.4F;
		this.friction = 0.8F;
		
		float roll = this.random.nextFloat() * 180F;
		this.oRoll = roll;
		this.roll = roll;
		
		this.xd = dx;
		this.yd = dy;
		this.zd = dz;
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprite;

		public Provider(SpriteSet sprite) {
			this.sprite = sprite;
		}
		
		@Override
		public Particle createParticle(SimpleParticleType particleType, ClientLevel level, double x, double y, double z, double dx, double dy, double dz) {
			FeatherParticle featuerparticle = new FeatherParticle(level, x, y, z, dx, dy, dz);
			featuerparticle.pickSprite(this.sprite);
			return featuerparticle;
		}
	}
}