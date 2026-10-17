package yesman.epicfight.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

public class CatharsisParticle extends TextureSheetParticle {
	private final SpriteSet sprites;
	
	protected CatharsisParticle(ClientLevel pLevel, double pX, double pY, double pZ, SpriteSet sprites) {
		super(pLevel, pX, pY, pZ);
		
		this.yd = 0.1D;
		this.quadSize = 0.75F;
		this.sprites = sprites;
		this.setSpriteFromAge(sprites);
	}
	
	@Override
	public void tick() {
		super.tick();
		this.setSpriteFromAge(this.sprites);
		this.alpha -= 0.05F;
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		private final SpriteSet sprites;

		public Provider(SpriteSet pSprites) {
			this.sprites = pSprites;
		}

		@Override
		public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
			CatharsisParticle catharsisparticle = new CatharsisParticle(pLevel, pX, pY, pZ, this.sprites);
			catharsisparticle.setAlpha(0.8F);
			catharsisparticle.setLifetime(12);
			
			return catharsisparticle;
		}
	}
}
