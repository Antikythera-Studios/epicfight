package yesman.epicfight.client.particle;

import java.util.Random;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import yesman.epicfight.config.ClientConfig;

public class CutParticle extends HitParticle {
	public CutParticle(ClientLevel world, double x, double y, double z, SpriteSet animatedSprite) {
		super(world, x, y, z, animatedSprite);
	    this.rCol = 1.0F;
	    this.gCol = 1.0F;
	    this.bCol = 1.0F;
	    this.quadSize = 1.0F;
		this.lifetime = 4;
		
		Random rand = new Random();
		float angle = (float)Math.toRadians(rand.nextFloat() * 90.0F);
		this.oRoll = angle;
		this.roll = angle;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet spriteSet;

		public Provider(SpriteSet spriteSet) {
	         this.spriteSet = spriteSet;
	    }
	    
		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			if (!ClientConfig.bloodEffects) {
				return null;
			}
			
			CutParticle particle = new CutParticle(worldIn, x, y, z, spriteSet);
			return particle;
		}
	}
}