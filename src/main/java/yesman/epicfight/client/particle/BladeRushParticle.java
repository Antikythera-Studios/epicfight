package yesman.epicfight.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;

public class BladeRushParticle extends HitParticle {
	public BladeRushParticle(ClientLevel world, double x, double y, double z, SpriteSet animatedSprite) {
		super(world, x, y, z, animatedSprite);
	    this.rCol = 1.0F;
	    this.gCol = 1.0F;
	    this.bCol = 1.0F;
		this.lifetime = 4;
		this.quadSize = 2.0F;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
	         this.spriteSet = spriteSet;
	    }
	    
		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			BladeRushParticle particle = new BladeRushParticle(worldIn, x, y, z, this.spriteSet);
			return particle;
		}
	}
}