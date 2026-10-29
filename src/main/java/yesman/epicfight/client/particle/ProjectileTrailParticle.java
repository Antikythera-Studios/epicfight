package yesman.epicfight.client.particle;

import java.util.List;

import com.google.common.collect.Lists;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.physics.bezier.CubicBezierCurve;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.entries.EpicFightParticles;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.projectile.ProjectilePatch;

public class ProjectileTrailParticle extends AbstractTrailParticle<ProjectilePatch<AbstractArrow>> {
	protected float lastXRot;
	protected float lastYRot;
	
	protected ProjectileTrailParticle(ClientLevel level, ProjectilePatch<AbstractArrow> entitypatch, TrailInfo trailInfo) {
		super(level, entitypatch, trailInfo);
		
		this.rCol = trailInfo.rCol();
		this.gCol = trailInfo.gCol();
		this.bCol = trailInfo.bCol();
	}
	
	@Override
	protected boolean canContinue() {
		if (this.owner.getOriginal() instanceof ThrownTrident thrownTrident && thrownTrident.clientSideReturnTridentTickCount > 0) {
			return false;
		}
		
		if (this.owner.hit()) {
			return false;
		}
		
		return this.owner.getOriginal().isAlive() && !this.owner.getOriginal().inGround;
	}
	
	@Override
	protected void createNextCurve() {
		if (this.shouldRemove) {
			return;
		}
		
		if (this.owner.getOriginal() instanceof Arrow arrow) {
			int color = arrow.getColor();
			float r = ((color & 0x00FF0000) >> 16) / 255.0F;
			float g = ((color & 0x0000FF00) >> 8) / 255.0F;
			float b = ((color & 0x000000FF)) / 255.0F;
			this.rCol = r;
			this.gCol = g;
			this.bCol = b;
		}
		
		boolean isFirstTrail = this.trailEdges.isEmpty();
		
		if (isFirstTrail) {
			this.lastXRot = this.owner.getOriginal().getXRot();
			this.lastYRot = 180.0F + this.owner.getOriginal().getYRot();
		}
		
		TrailInfo trailInfo = this.trailInfo;
		Vec3 posOld = this.owner.getOriginal().getPosition(0.0F);
		Vec3 posCur = this.owner.getOriginal().getPosition(1.0F);
		Vec3 posMid = MathUtils.lerpVector(posOld, posCur, 0.5F);
		
		float xRotO = this.lastXRot;
		float xRot = this.owner.getOriginal().getXRot();
		float xRotMod = Mth.rotLerp(0.5F, xRotO, xRot);
		float yRotO =  this.lastYRot;
		float yRot =  180.0F + this.owner.getOriginal().getYRot();
		float yRotMod = Mth.rotLerp(0.5F, yRotO, yRot);
		
		OpenMatrix4f prevTransform
			= OpenMatrix4f
				.createTranslation((float)posOld.x, (float)posOld.y, (float)posOld.z)
				.rotateDeg(yRotO, Vec3f.Y_AXIS)
				.rotateDeg(xRotO, Vec3f.X_AXIS)
		;
		OpenMatrix4f modTransform
			= OpenMatrix4f
				.createTranslation((float)posMid.x, (float)posMid.y, (float)posMid.z)
				.rotateDeg(yRotMod, Vec3f.Y_AXIS)
				.rotateDeg(xRotMod, Vec3f.X_AXIS)
		;
		OpenMatrix4f curTransform
			= OpenMatrix4f
				.createTranslation((float)posCur.x, (float)posCur.y, (float)posCur.z)
				.rotateDeg(yRot, Vec3f.Y_AXIS)
				.rotateDeg(xRot, Vec3f.X_AXIS)
		;
		
		Vec3 prevStartPos = OpenMatrix4f.transform(prevTransform, trailInfo.start());
		Vec3 prevEndPos = OpenMatrix4f.transform(prevTransform, trailInfo.end());
		Vec3 middleStartPos = OpenMatrix4f.transform(modTransform, trailInfo.start());
		Vec3 middleEndPos = OpenMatrix4f.transform(modTransform, trailInfo.end());
		Vec3 currentStartPos = OpenMatrix4f.transform(curTransform, trailInfo.start());
		Vec3 currentEndPos = OpenMatrix4f.transform(curTransform, trailInfo.end());
		List<Vec3> finalStartPositions;
		List<Vec3> finalEndPositions;
		List<Vec3> startPosList = Lists.newArrayList();
		List<Vec3> endPosList = Lists.newArrayList();
		TrailEdge edge1;
		TrailEdge edge2;
		
		if (isFirstTrail) {
			edge1 = new TrailEdge(prevStartPos, prevEndPos, -1);
			edge2 = new TrailEdge(middleStartPos, middleEndPos, -1);
		} else {
			edge1 = this.trailEdges.get(this.trailEdges.size() - (this.trailInfo.interpolateCount() / 2 + 1));
			edge2 = this.trailEdges.get(this.trailEdges.size() - 1);
			edge2.lifetime++;
		}
		
		startPosList.add(edge1.start);
		endPosList.add(edge1.end);
		startPosList.add(edge2.start);
		endPosList.add(edge2.end);
		startPosList.add(middleStartPos);
		endPosList.add(middleEndPos);
		startPosList.add(currentStartPos);
		endPosList.add(currentEndPos);
		
		finalStartPositions = CubicBezierCurve.getBezierInterpolatedPoints(startPosList, 1, 3, this.trailInfo.interpolateCount());
		finalEndPositions = CubicBezierCurve.getBezierInterpolatedPoints(endPosList, 1, 3, this.trailInfo.interpolateCount());
		
		if (!isFirstTrail) {
			finalStartPositions.remove(0);
			finalEndPositions.remove(0);
		}
		
		this.makeTrailEdges(finalStartPositions, finalEndPositions, this.trailEdges);
		
		this.lastXRot = xRot;
		this.lastYRot = yRot;
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		public static final TrailInfo ARROW_TRAIL_DEFAULT
			= TrailInfo
				.builder()
				.type(EpicFightParticles.PROJECTILE_TRAIL.get())
				.startPos(new Vec3(-0.1D, 0.0D, 0.7D))
				.endPos(new Vec3(0.1D, 0.0D, 0.7D))
				.interpolations(4)
				.lifetime(9)
				.updateInterval(1)
				.texture(EpicFight.identifier("textures/particle/projectile_trail.png"))
				.create();
		
		public static final TrailInfo SPECTRAL_ARROW_TRAIL_DEFAULT
			= TrailInfo
				.builder()
				.type(EpicFightParticles.PROJECTILE_TRAIL.get())
				.startPos(new Vec3(-0.1D, 0.0D, 0.7D))
				.endPos(new Vec3(0.1D, 0.0D, 0.7D))
				.interpolations(4)
				.lifetime(9)
				.updateInterval(1)
				.r(252.0F / 255.0F)
				.g(252.0F / 255.0F)
				.b(118.0F / 255.0F)
				.texture(EpicFight.identifier("textures/particle/projectile_trail.png"))
				.create();
		
		public static final TrailInfo TRIDENT_TRAIL_DEFAULT
			= TrailInfo
				.builder()
				.type(EpicFightParticles.PROJECTILE_TRAIL.get())
				.startPos(new Vec3(-0.1D, 0.0D, 1.8D))
				.endPos(new Vec3(0.1D, 0.0D, 1.8D))
				.interpolations(4)
				.lifetime(9)
				.updateInterval(1)
				.r(0.0F / 255.0F)
				.g(232.0F / 255.0F)
				.b(245.0F / 255.0F)
				.texture(EpicFight.identifier("textures/particle/projectile_trail.png"))
				.create();
		
		@SuppressWarnings("unchecked")
		@Override
		public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int eid = (int)Double.doubleToRawLongBits(x);
			Entity entity = level.getEntity(eid);
			
			if (entity == null) {
				return null;
			}
			
			if (!(entity instanceof AbstractArrow)) {
				return null;
			}
			
			ProjectilePatch<AbstractArrow> entitypatch = EpicFightCapabilities.getEntityPatch(entity, ProjectilePatch.class);
			
			if (entitypatch != null) {
				TrailInfo trailInfo;
				
				if (entitypatch.getOriginal() instanceof Arrow) {
					trailInfo = ARROW_TRAIL_DEFAULT;
				} else if (entitypatch.getOriginal() instanceof SpectralArrow) {
					trailInfo = SPECTRAL_ARROW_TRAIL_DEFAULT;
				} else if (entitypatch.getOriginal() instanceof ThrownTrident) {
					trailInfo = TRIDENT_TRAIL_DEFAULT;
				} else {
					return null;
				}
				
				return new ProjectileTrailParticle(level, entitypatch, trailInfo);
			}
			
			return null;
		}
	}
}
