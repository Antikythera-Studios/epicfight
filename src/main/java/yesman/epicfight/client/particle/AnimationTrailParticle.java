package yesman.epicfight.client.particle;

import java.util.List;
import java.util.Optional;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.google.common.collect.Lists;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.AnimationPlayer;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.animation.JointTransform;
import yesman.epicfight.api.animation.Pose;
import yesman.epicfight.api.animation.types.StaticAnimation;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.animation.property.ClientAnimationProperties;
import yesman.epicfight.api.client.animation.property.TrailInfo;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.bezier.CubicBezierCurve;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.client.events.engine.RenderEngine;
import yesman.epicfight.client.renderer.patched.item.RenderItemBase;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.CapabilityItem;

public class AnimationTrailParticle extends AbstractTrailParticle<LivingEntityPatch<?>> {
	protected final Joint joint;
	protected final AssetAccessor<? extends StaticAnimation> animation;
	protected final List<TrailEdge> invisibleTrailEdges;
	
	protected AnimationTrailParticle(ClientLevel level, LivingEntityPatch<?> owner, Joint joint, AssetAccessor<? extends StaticAnimation> animation, TrailInfo trailInfo) {
		super(level, owner, trailInfo);
		
		this.joint = joint;
		this.animation = animation;
		this.invisibleTrailEdges = Lists.newLinkedList();
		
		Pose prevPose = this.owner.getAnimator().getPose(0.0F);
		Pose middlePose = this.owner.getAnimator().getPose(0.5F);
		Pose currentPose = this.owner.getAnimator().getPose(1.0F);
		Vec3 posOld = this.owner.getOriginal().getPosition(0.0F);
		Vec3 posMid = this.owner.getOriginal().getPosition(0.5F);
		Vec3 posCur = this.owner.getOriginal().getPosition(1.0F);
		
		OpenMatrix4f prvmodelTf
			= OpenMatrix4f.createTranslation((float)posOld.x, (float)posOld.y, (float)posOld.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(this.owner.getModelMatrix(0.0F));
		OpenMatrix4f middleModelTf
			= OpenMatrix4f.createTranslation((float)posMid.x, (float)posMid.y, (float)posMid.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(this.owner.getModelMatrix(0.5F));
		OpenMatrix4f curModelTf
			= OpenMatrix4f.createTranslation((float)posCur.x, (float)posCur.y, (float)posCur.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(this.owner.getModelMatrix(1.0F));
		
		OpenMatrix4f prevJointTf = this.owner.getArmature().getBoundTransformFor(prevPose, this.joint).mulFront(prvmodelTf);
		OpenMatrix4f middleJointTf = this.owner.getArmature().getBoundTransformFor(middlePose, this.joint).mulFront(middleModelTf);
		OpenMatrix4f currentJointTf = this.owner.getArmature().getBoundTransformFor(currentPose, this.joint).mulFront(curModelTf);
		
		Vec3 prevStartPos = OpenMatrix4f.transform(prevJointTf, trailInfo.start());
		Vec3 prevEndPos = OpenMatrix4f.transform(prevJointTf, trailInfo.end());
		Vec3 middleStartPos = OpenMatrix4f.transform(middleJointTf, trailInfo.start());
		Vec3 middleEndPos = OpenMatrix4f.transform(middleJointTf, trailInfo.end());
		Vec3 currentStartPos = OpenMatrix4f.transform(currentJointTf, trailInfo.start());
		Vec3 currentEndPos = OpenMatrix4f.transform(currentJointTf, trailInfo.end());
		
		this.invisibleTrailEdges.add(new TrailEdge(prevStartPos, prevEndPos, this.trailInfo.trailLifetime()));
		this.invisibleTrailEdges.add(new TrailEdge(middleStartPos, middleEndPos, this.trailInfo.trailLifetime()));
		this.invisibleTrailEdges.add(new TrailEdge(currentStartPos, currentEndPos, this.trailInfo.trailLifetime()));
		
		this.rCol = Math.max(this.trailInfo.rCol(), 0.0F);
		this.gCol = Math.max(this.trailInfo.gCol(), 0.0F);
		this.bCol = Math.max(this.trailInfo.bCol(), 0.0F);
		
		if (this.trailInfo.texturePath() != null) {
			TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
			AbstractTexture abstracttexture = texturemanager.getTexture(this.trailInfo.texturePath());
		    
			RenderSystem.bindTexture(abstracttexture.getId());
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		}
	}
	
	@Deprecated /** This constructor is only for {@link ModelPreviewer} **/
	protected AnimationTrailParticle(Armature armature, LivingEntityPatch<?> owner, Joint joint, AssetAccessor<? extends StaticAnimation> animation, TrailInfo trailInfo) {
		super(owner, trailInfo);
		
		this.joint = joint;
		this.animation = animation;
		this.invisibleTrailEdges = Lists.newLinkedList();
		
		Pose prevPose = this.owner.getClientAnimator().getPose(0.0F);
		Pose middlePose = this.owner.getClientAnimator().getPose(0.5F);
		Pose currentPose = this.owner.getClientAnimator().getPose(1.0F);
		
		OpenMatrix4f prevJointTf = armature.getBoundTransformFor(prevPose, this.joint);
		OpenMatrix4f middleJointTf = armature.getBoundTransformFor(middlePose, this.joint);
		OpenMatrix4f currentJointTf = armature.getBoundTransformFor(currentPose, this.joint);
		
		Vec3 prevStartPos = OpenMatrix4f.transform(prevJointTf, trailInfo.start());
		Vec3 prevEndPos = OpenMatrix4f.transform(prevJointTf, trailInfo.end());
		Vec3 middleStartPos = OpenMatrix4f.transform(middleJointTf, trailInfo.start());
		Vec3 middleEndPos = OpenMatrix4f.transform(middleJointTf, trailInfo.end());
		Vec3 currentStartPos = OpenMatrix4f.transform(currentJointTf, trailInfo.start());
		Vec3 currentEndPos = OpenMatrix4f.transform(currentJointTf, trailInfo.end());
		
		this.invisibleTrailEdges.add(new TrailEdge(prevStartPos, prevEndPos, this.trailInfo.trailLifetime()));
		this.invisibleTrailEdges.add(new TrailEdge(middleStartPos, middleEndPos, this.trailInfo.trailLifetime()));
		this.invisibleTrailEdges.add(new TrailEdge(currentStartPos, currentEndPos, this.trailInfo.trailLifetime()));
		
		this.rCol = Math.max(this.trailInfo.rCol(), 0.0F);
		this.gCol = Math.max(this.trailInfo.gCol(), 0.0F);
		this.bCol = Math.max(this.trailInfo.bCol(), 0.0F);
		
		if (this.trailInfo.texturePath() != null) {
			TextureManager texturemanager = Minecraft.getInstance().getTextureManager();
			AbstractTexture abstracttexture = texturemanager.getTexture(this.trailInfo.texturePath());
		    
			RenderSystem.bindTexture(abstracttexture.getId());
			RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
		    RenderSystem.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
		}
	}
	
	@Override
	protected boolean canContinue() {
		AnimationPlayer animPlayer = this.owner.getAnimator().getPlayerFor(this.animation);
		boolean cont = this.owner.getOriginal().isAlive() && this.animation == animPlayer.getRealAnimation() && animPlayer.getElapsedTime() <= this.trailInfo.endTime();
		if (!cont && Provider.DBG.add("cc" + this.hashCode())) {
			yesman.epicfight.EpicFight.LOGGER.info("[trail-provider] canContinue=false anim={} alive={} match={} elapsed={} endTime={} edges={}", this.animation, this.owner.getOriginal().isAlive(), this.animation == animPlayer.getRealAnimation(), animPlayer.getElapsedTime(), this.trailInfo.endTime(), this.trailEdges.size());
		}
		return cont;
	}
	
	@Override
	protected boolean canCreateNextCurve() {
		AnimationPlayer animPlayer = this.owner.getAnimator().getPlayerFor(this.animation);
		
		if (TrailInfo.isValidTime(this.trailInfo.fadeTime()) && this.trailInfo.endTime() < animPlayer.getElapsedTime()) {
			return false;
		}
		
		return super.canCreateNextCurve();
	}
	
	@Override
	protected void createNextCurve() {
		AnimationPlayer animPlayer = this.owner.getAnimator().getPlayerFor(this.animation);
		boolean isTrailInvisible = animPlayer.getAnimation().get().isLinkAnimation() || animPlayer.getElapsedTime() <= this.trailInfo.startTime();
		boolean isFirstTrail = this.trailEdges.isEmpty();
		boolean needCorrection = (!isTrailInvisible && isFirstTrail);

		if (needCorrection) {
			float startCorrection = Math.max((this.trailInfo.startTime() - animPlayer.getPrevElapsedTime()) / (animPlayer.getElapsedTime() - animPlayer.getPrevElapsedTime()), 0.0F);
			this.startEdgeCorrection = this.trailInfo.interpolateCount() * 2 * startCorrection;
		}
		
		TrailInfo trailInfo = this.trailInfo;
		Pose prevPose = this.owner.getAnimator().getPose(0.0F);//this.lastPose;
		Pose currentPose = this.owner.getAnimator().getPose(1.0F);
		Pose middlePose = this.owner.getAnimator().getPose(0.5F);//Pose.interpolatePose(prevPose, currentPose, 0.5F);
		
		Vec3 posOld = this.owner.getOriginal().getPosition(0.0F);
		Vec3 posCur = this.owner.getOriginal().getPosition(1.0F);
		Vec3 posMid = MathUtils.lerpVector(posOld, posCur, 0.5F);
		
		OpenMatrix4f prevModelMatrix = this.owner.getModelMatrix(0.0F);
		OpenMatrix4f curModelMatrix = this.owner.getModelMatrix(1.0F);
		JointTransform lastTransform = JointTransform.fromMatrix(curModelMatrix);
		JointTransform currentTransform = JointTransform.fromMatrix(curModelMatrix);
		
		OpenMatrix4f prvmodelTf
			= OpenMatrix4f
				.createTranslation((float)posOld.x, (float)posOld.y, (float)posOld.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(prevModelMatrix);
		OpenMatrix4f middleModelTf
			= OpenMatrix4f
				.createTranslation((float)posMid.x, (float)posMid.y, (float)posMid.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(JointTransform.interpolate(lastTransform, currentTransform, 0.5F).toMatrix());
		OpenMatrix4f curModelTf
			= OpenMatrix4f
				.createTranslation((float)posCur.x, (float)posCur.y, (float)posCur.z)
				.rotateDeg(180.0F, Vec3f.Y_AXIS)
				.mulBack(curModelMatrix);
		
		OpenMatrix4f prevJointTf = this.owner.getArmature().getBoundTransformFor(prevPose, this.joint).mulFront(prvmodelTf);
		OpenMatrix4f middleJointTf = this.owner.getArmature().getBoundTransformFor(middlePose, this.joint).mulFront(middleModelTf);
		OpenMatrix4f currentJointTf = this.owner.getArmature().getBoundTransformFor(currentPose, this.joint).mulFront(curModelTf);
		Vec3 prevStartPos = OpenMatrix4f.transform(prevJointTf, trailInfo.start());
		Vec3 prevEndPos = OpenMatrix4f.transform(prevJointTf, trailInfo.end());
		Vec3 middleStartPos = OpenMatrix4f.transform(middleJointTf, trailInfo.start());
		Vec3 middleEndPos = OpenMatrix4f.transform(middleJointTf, trailInfo.end());
		Vec3 currentStartPos = OpenMatrix4f.transform(currentJointTf, trailInfo.start());
		Vec3 currentEndPos = OpenMatrix4f.transform(currentJointTf, trailInfo.end());
		
		List<Vec3> finalStartPositions;
		List<Vec3> finalEndPositions;
		boolean visibleTrail;
		
		if (isTrailInvisible) {
			finalStartPositions = Lists.newArrayList();
			finalEndPositions = Lists.newArrayList();
			finalStartPositions.add(prevStartPos);
			finalStartPositions.add(middleStartPos);
			finalEndPositions.add(prevEndPos);
			finalEndPositions.add(middleEndPos);
			
			this.invisibleTrailEdges.clear();
			visibleTrail = false;
		} else {
			List<Vec3> startPosList = Lists.newArrayList();
			List<Vec3> endPosList = Lists.newArrayList();
			TrailEdge edge1;
			TrailEdge edge2;
			
			if (isFirstTrail) {
				int lastIdx = this.invisibleTrailEdges.size() - 1;
				edge1 = this.invisibleTrailEdges.get(lastIdx);
				edge2 = new TrailEdge(prevStartPos, prevEndPos, -1);
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
			
			visibleTrail = true;
		}
		
		this.makeTrailEdges(finalStartPositions, finalEndPositions, visibleTrail ? this.trailEdges : this.invisibleTrailEdges);

		if (Provider.DBG.add("v" + this.hashCode()) && !this.trailEdges.isEmpty()) {
			yesman.epicfight.EpicFight.LOGGER.info("[trail-provider] visible edges={} invisible={} anim={} elapsed={}", this.trailEdges.size(), this.invisibleTrailEdges.size(), this.animation, animPlayer.getElapsedTime());
		}
	}
	
	public static class Provider implements ParticleProvider<SimpleParticleType> {
		static final java.util.Set<String> DBG = new java.util.HashSet<>();
		private static void dbg(String key, String msg, Object... args) {
			if (DBG.add(key)) yesman.epicfight.EpicFight.LOGGER.info("[trail-provider] " + msg, args);
		}

		@Override
		public Particle createParticle(SimpleParticleType typeIn, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
			int eid = (int)Double.doubleToRawLongBits(x);
			int animid = (int)Double.doubleToRawLongBits(z);
			int jointId = (int)Double.doubleToRawLongBits(xSpeed);
			int idx = (int)Double.doubleToRawLongBits(ySpeed);
			Entity entity = level.getEntity(eid);

			if (entity == null) {
				dbg("e" + eid, "entity {} not in client level", eid);
				return null;
			}

			LivingEntityPatch<?> entitypatch = EpicFightCapabilities.getEntityPatch(entity, LivingEntityPatch.class);

			if (entitypatch == null) {
				dbg("p" + eid, "no entitypatch for {}", entity);
				return null;
			}

			AnimationAccessor<? extends StaticAnimation> animation = AnimationManager.byId(animid);

			if (animation == null) {
				dbg("a" + animid, "no animation accessor for id {}", animid);
				return null;
			}

			Optional<List<TrailInfo>> trailInfo = animation.get().getProperty(ClientAnimationProperties.TRAIL_EFFECT);

			if (trailInfo.isEmpty()) {
				dbg("t" + animid, "no TRAIL_EFFECT on {}", animation.get().getRegistryName());
				return null;
			}
			
			TrailInfo result = trailInfo.get().get(idx);
			

			net.minecraft.world.InteractionHand resolvedHand = result.hand();
			if (resolvedHand != null && entitypatch.isMirrorMode() && resolvedHand == net.minecraft.world.InteractionHand.MAIN_HAND) {
				resolvedHand = net.minecraft.world.InteractionHand.OFF_HAND;
			}

			if (resolvedHand != null) {
				ItemStack stack = entitypatch.getOriginal().getItemInHand(resolvedHand);
				RenderItemBase renderItemBase = RenderEngine.getInstance().getItemRenderer(stack);

				if (renderItemBase != null && renderItemBase.trailInfo() != null) {
					result = renderItemBase.trailInfo().overwrite(result);
				}
			}

			result = entitypatch.getEntityDecorations().getModifiedTrailInfo(result, resolvedHand == null ? CapabilityItem.EMPTY : entitypatch.getAdvancedHoldingItemCapability(resolvedHand));
			
			if (result.playable()) {

				Joint trailJoint = entitypatch.getArmature().searchJointById(jointId);
				if (entitypatch.isMirrorMode()) {
					Joint mirroredJoint = entitypatch.getArmature().searchJointByName(yesman.epicfight.api.animation.PoseMirror.mirrorJointName(trailJoint.getName()));
					if (mirroredJoint != null) trailJoint = mirroredJoint;
				}
				dbg("c" + animid, "created trail particle anim={} joint={}", animation.get().getRegistryName(), trailJoint.getName());
				return new AnimationTrailParticle(level, entitypatch, trailJoint, animation, result);
			} else {
				dbg("np" + animid, "overwritten trail not playable on {}", animation.get().getRegistryName());
				return null;
			}
		}
	}
}