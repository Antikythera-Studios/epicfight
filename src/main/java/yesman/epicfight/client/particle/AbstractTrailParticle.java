package yesman.epicfight.client.particle;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector4f;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.world.capabilities.entitypatch.EntityPatch;

public abstract class AbstractTrailParticle<T extends EntityPatch<?>> extends TextureSheetParticle {
	protected final TrailInfo trailInfo;
	protected final T owner;
	protected final List<TrailEdge> trailEdges;
	protected float startEdgeCorrection = 0.0F;
	protected boolean shouldRemove;
	
	protected AbstractTrailParticle(ClientLevel level, T entitypatch, TrailInfo trailInfo) {
		super(level, 0, 0, 0);
		
		this.hasPhysics = false;
		this.owner = entitypatch;
		this.trailEdges = Lists.newLinkedList();
		this.trailInfo = trailInfo;
		
		Vec3 entityPos = entitypatch.getOriginal().position();
		this.move(entityPos.x, entityPos.y + entitypatch.getOriginal().getEyeHeight(), entityPos.z);
		
		float size = (float)Math.max(this.trailInfo.start().length(), this.trailInfo.end().length()) * 2.0F;
		this.setSize(size, size);
		
		this.rCol = Math.max(this.trailInfo.rCol(), 0.0F);
		this.gCol = Math.max(this.trailInfo.gCol(), 0.0F);
		this.bCol = Math.max(this.trailInfo.bCol(), 0.0F);
	}
	
	/**
	 * For datapack editor
	 */
	@Deprecated
	protected AbstractTrailParticle(T entitypatch, TrailInfo trailInfo) {
		super(null, 0, 0, 0);
		
		this.owner = entitypatch;
		this.trailEdges = Lists.newLinkedList();
		this.trailInfo = trailInfo;
		this.rCol = Math.max(this.trailInfo.rCol(), 0.0F);
		this.gCol = Math.max(this.trailInfo.gCol(), 0.0F);
		this.bCol = Math.max(this.trailInfo.bCol(), 0.0F);
	}
	
	protected abstract boolean canContinue();
	
	protected boolean canCreateNextCurve() {
		return this.age % this.trailInfo.updateInterval() == 0 && !this.removed;
	}
	
	protected abstract void createNextCurve();
	
	public void tick() {
		if (this.shouldRemove) {
			if (this.age >= this.lifetime) {
				this.remove();
			}
		} else {
			if (!this.canContinue()) {
				this.shouldRemove = true;
				this.lifetime = this.age + this.trailInfo.trailLifetime();
			}
		}
		
		this.age++;
		this.trailEdges.removeIf(v -> !v.isAlive());
		
		if (!this.canCreateNextCurve()) {
			return;
		}
		
		Vec3 lastPos = this.owner.getOriginal().getPosition(0.0F);
		double xd = Math.pow(this.owner.getOriginal().getX() - lastPos.x, 2);
		double yd = Math.pow(this.owner.getOriginal().getY() - lastPos.y, 2);
		double zd = Math.pow(this.owner.getOriginal().getZ() - lastPos.z, 2);
		float move = (float)Math.sqrt(xd + yd + zd) * 2.0F;
		
		this.setSize(this.bbWidth + move, this.bbHeight + move);
		this.createNextCurve();
	}
	
	public void render(VertexConsumer vertexConsumer, Camera camera, float partialTick) {
		if (this.trailEdges.isEmpty()) {
			return;
		}
		
		PoseStack poseStack = new PoseStack();
		int light = this.getLightColor(partialTick);
		this.setupPoseStack(poseStack, camera, partialTick);
		Matrix4f matrix4f = poseStack.last().pose();
		int edges = this.trailEdges.size() - 1;
		boolean startFade = this.trailEdges.get(0).lifetime == 1;
		boolean endFade = this.trailEdges.get(edges).lifetime == this.trailInfo.trailLifetime();
		float startEdge = (startFade ? this.trailInfo.interpolateCount() * 2 * partialTick : 0.0F) + this.startEdgeCorrection;
		float endEdge = endFade ? Math.min(edges - (this.trailInfo.interpolateCount() * 2) * (1.0F - partialTick), edges - 1) : edges - 1;
		float interval = 1.0F / (endEdge - startEdge);
		float fading = 1.0F;
		
		if (this.shouldRemove) {
			if (TrailInfo.isValidTime(this.trailInfo.fadeTime())) {
				fading = ((float)(this.lifetime - this.age) / (float)this.trailInfo.trailLifetime());
			} else {
				fading = Mth.clamp(((this.lifetime - this.age) + (1.0F - partialTick)) / this.trailInfo.trailLifetime(), 0.0F, 1.0F);
			}
		}
		
		float partialStartEdge = interval * (startEdge % 1.0F);
		float from = -partialStartEdge;
		float to = -partialStartEdge + interval;
		
		for (int i = (int)(startEdge); i < (int)endEdge + 1; i++) {
			TrailEdge e1 = this.trailEdges.get(i);
			TrailEdge e2 = this.trailEdges.get(i + 1);
			Vector4f pos1 = new Vector4f((float)e1.start.x, (float)e1.start.y, (float)e1.start.z, 1.0F);
			Vector4f pos2 = new Vector4f((float)e1.end.x, (float)e1.end.y, (float)e1.end.z, 1.0F);
			Vector4f pos3 = new Vector4f((float)e2.end.x, (float)e2.end.y, (float)e2.end.z, 1.0F);
			Vector4f pos4 = new Vector4f((float)e2.start.x, (float)e2.start.y, (float)e2.start.z, 1.0F);
			
			pos1.mul(matrix4f);
			pos2.mul(matrix4f);
			pos3.mul(matrix4f);
			pos4.mul(matrix4f);
			
			float alphaFrom = Mth.clamp(from, 0.0F, 1.0F);
			float alphaTo = Mth.clamp(to, 0.0F, 1.0F);
			
			vertexConsumer.addVertex(pos1.x(), pos1.y(), pos1.z()).setUv(from, 1.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).setLight(light);
			vertexConsumer.addVertex(pos2.x(), pos2.y(), pos2.z()).setUv(from, 0.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaFrom * fading).setLight(light);
			vertexConsumer.addVertex(pos3.x(), pos3.y(), pos3.z()).setUv(to, 0.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).setLight(light);
			vertexConsumer.addVertex(pos4.x(), pos4.y(), pos4.z()).setUv(to, 1.0F).setColor(this.rCol, this.gCol, this.bCol, this.alpha * alphaTo * fading).setLight(light);
			
			from += interval;
			to += interval;
		}
	}
	
	public AABB getRenderBoundingBox(float partialTicks) {
        return new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    }
	
		public ParticleRenderType getRenderType() {
		return EpicFightParticleRenderTypes.TRAIL_EFFECT.apply(this.trailInfo.texturePath());
	}
	
	protected void setupPoseStack(PoseStack poseStack, Camera camera, float partialTicks) {
		Vec3 vec3 = camera.getPosition();
		float x = (float)-vec3.x();
		float y = (float)-vec3.y();
		float z = (float)-vec3.z();
		
		poseStack.translate(x, y, z);
	}
	
	protected void makeTrailEdges(List<Vec3> startPositions, List<Vec3> endPositions, List<TrailEdge> dest) {
		for (int i = 0; i < startPositions.size(); i++) {
			dest.add(new TrailEdge(startPositions.get(i), endPositions.get(i), this.trailInfo.trailLifetime()));
		}
	}
	
	@SuppressWarnings("deprecation")
	protected int getLightColor(float pPartialTick) {
		BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
		return this.level.hasChunkAt(blockpos) ? this.getLightColor(this.level, this.level.getBlockState(blockpos), blockpos) : 0;
	}
	
	/**
	 * A copy for {@link LevelRenderer#getLightColor(BlockAndTintGetter, BlockState, BlockPos)} 
	 * @param level
	 * @param state
	 * @param pos
	 * @return
	 */
	private int getLightColor(BlockAndTintGetter level, BlockState state, BlockPos pos) {
		if (state.emissiveRendering(level, pos)) {
			return 15728880;
		} else {
			int i = Mth.clamp(Math.max(this.trailInfo.skyLight(), level.getBrightness(LightLayer.SKY, pos)), 0, 15);
			int j = Mth.clamp(Math.max(this.trailInfo.blockLight(), level.getBrightness(LightLayer.BLOCK, pos)), 0, 15);
			int k = state.getLightEmission();
			
			if (j < k) {
				j = k;
			}
			
			return i << 20 | j << 4;
		}
	}
	
	public static class TrailEdge {
		public final Vec3 start;
		public final Vec3 end;
		public int lifetime;
		
		public TrailEdge(Vec3 start, Vec3 end, int lifetime) {
			this.start = start;
			this.end = end;
			this.lifetime = lifetime;
		}
		
		public boolean isAlive() {
			return --this.lifetime > 0;
		}
	}
}