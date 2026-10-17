package yesman.epicfight.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;

public abstract class HitParticle extends TextureSheetParticle {
	protected final SpriteSet animatedSprite;
	
	public HitParticle(ClientLevel world, double x, double y, double z, SpriteSet animatedSprite) {
		super(world, x, y, z);
	    this.rCol = 1.0F;
	    this.gCol = 1.0F;
	    this.bCol = 1.0F;
		this.animatedSprite = animatedSprite;
		this.setSpriteFromAge(animatedSprite);
	}
	
	@Override
	public void tick() {
		this.xo = this.x;
		this.yo = this.y;
		this.zo = this.z;
		
		if (this.age++ >= this.lifetime) {
			this.remove();
		} else {
			this.setSpriteFromAge(this.animatedSprite);
		}
	}
	
	@Override
	public ParticleRenderType getRenderType() {
		return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
	}
	
	@Override
	public int getLightColor(float partialTick) {
		return 15728880;
	}
}