package yesman.epicfight.api.client.physics.cloth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nullable;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.animation.Joint;
import yesman.epicfight.api.client.model.CompositeMesh;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPart;
import yesman.epicfight.api.client.model.SoftBodyTranslatable;
import yesman.epicfight.api.client.model.VertexBuilder;
import yesman.epicfight.api.client.physics.AbstractSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObjectBuilder;
import yesman.epicfight.api.collider.OBBCollider;
import yesman.epicfight.api.model.Armature;
import yesman.epicfight.api.physics.SimulationObject;
import yesman.epicfight.api.utils.math.MathUtils;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.EpicFight;
import yesman.epicfight.main.EpicFightSharedConstants;

/**
 * Referred to Matthias Müller's Ten minuates physics tutorial video number 14, 15
 * 
 * https://matthias-research.github.io/pages/tenMinutePhysics/index.html
 * 
 * https://www.youtube.com/@TenMinutePhysics
 **/
public class ClothSimulator extends AbstractSimulator<ResourceLocation, ClothObjectBuilder, SoftBodyTranslatable, ClothSimulatable, ClothSimulator.ClothObject> {
    public static final ResourceLocation PLAYER_CLOAK = EpicFight.identifier("ingame_cloak");
    public static final ResourceLocation MODELPREVIEWER_CLOAK = EpicFight.identifier("previewer_cloak");
	private static final float SPATIAL_HASH_SPACING = 0.05F;
	
	public static class ClothObjectBuilder extends SimulationObject.SimulationObjectBuilder {
		List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders = Lists.newArrayList();
		Joint joint;
		
		public ClothObjectBuilder addEntry(Function<ClothSimulatable, OpenMatrix4f> obbTransformer, ClothOBBCollider clothOBBCollider) {
			this.clothColliders.add(Pair.of(obbTransformer, clothOBBCollider));
			return this;
		}
		
		public ClothObjectBuilder putAll(List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothOBBColliders) {
			this.clothColliders.addAll(clothOBBColliders);
			return this;
		}
		
		public ClothObjectBuilder parentJoint(Joint joint) {
			this.joint = joint;
			return this;
		}
		
		public static ClothObjectBuilder create() {
			return new ClothObjectBuilder();
		}
	}
	
	// Developer configurations
	private static boolean DRAW_MESH_COLLIDERS = false;
	private static boolean DRAW_NORMAL_OFFSET = true;
	private static boolean DRAW_OUTLINES = false;
	
	public static void drawMeshColliders(boolean flag) {
		if (!EpicFightSharedConstants.IS_DEV_ENV) {
			throw new IllegalStateException("Can't switch developer configuration in product environment.");
		}
		
		DRAW_MESH_COLLIDERS = flag;
	}
	
	public static void drawNormalOffset(boolean flag) {
		if (!EpicFightSharedConstants.IS_DEV_ENV) {
			throw new IllegalStateException("Can't switch developer configuration in product environment.");
		}
		
		DRAW_NORMAL_OFFSET = flag;
	}
	
	public static void drawOutlines(boolean flag) {
		if (!EpicFightSharedConstants.IS_DEV_ENV) {
			throw new IllegalStateException("Can't switch developer configuration in product environment.");
		}
		
		DRAW_OUTLINES = flag;
	}
	
	public static class ClothObject implements SimulationObject<ClothObjectBuilder, SoftBodyTranslatable, ClothSimulatable>, Mesh {
		private final SoftBodyTranslatable provider;
		private final Map<String, ClothPart> parts;
		
		private final Map<Integer, Particle> particles;
		private final Map<Integer, ClothPart.OffsetParticle> normalOffsetParticles;
		private final List<Map<Integer, Vec3f>> particleNormals;
		
		private final Quaternionf rotationO = new Quaternionf();
		private final Vec3f centrifugalO = new Vec3f();
		
		@Nullable
		protected List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders;
		protected final Joint parentJoint;
		
		//Storage vectors
		private static final Vec3f TRASNFORMED = new Vec3f();
		private static final Vector4f POSITION = new Vector4f();
		private static final Vector3f NORMAL = new Vector3f();
		
		public ClothObject(ClothObjectBuilder builder, SoftBodyTranslatable provider, Map<String, MeshPart> parts, float[] positions) {
			this.clothColliders = builder.clothColliders;
			this.parentJoint = builder.joint;
			
			this.provider = provider;
			this.particles = Maps.newHashMap();
			this.normalOffsetParticles = Maps.newHashMap();
			this.particleNormals = Lists.newArrayList();
			
			for (int i = 0; i < positions.length / 3; i++) {
				this.particleNormals.add(Maps.newHashMap());
			}
			
			for (Map.Entry<String, MeshPart> meshPart : parts.entrySet()) {
				for (VertexBuilder vb : meshPart.getValue().getVertices()) {
					Map<Integer, Vec3f> posNormals = this.particleNormals.get(vb.position);
					
					if (!posNormals.containsKey(vb.normal)) {
						provider.getOriginalMesh().getVertexNormal(vb.normal, NORMAL);
						posNormals.put(vb.normal, new Vec3f(NORMAL.x, NORMAL.y, NORMAL.z));
					}
				}
			}
			
			ImmutableMap.Builder<String, ClothPart> partBuilder = ImmutableMap.builder();
			
			for (Map.Entry<String, SoftBodyTranslatable.ClothSimulationInfo> entry : provider.getSoftBodySimulationInfo().entrySet()) {
				partBuilder.put(entry.getKey(), new ClothPart(entry.getValue(), positions));
			}
			
			this.parts = partBuilder.build();
		}
		
		private ClothObject(ClothObject copyTarget) {
			this.provider = copyTarget.provider;
			this.parts = copyTarget.parts;
			
			this.particles = new HashMap<> ();
			this.normalOffsetParticles = new HashMap<> ();
			
			for (Map.Entry<Integer, Particle> entry : copyTarget.particles.entrySet()) {
				this.particles.put(entry.getKey(), entry.getValue().copy());
			}
			
			for (Map.Entry<Integer, ClothPart.OffsetParticle> entry : copyTarget.normalOffsetParticles.entrySet()) {
				this.normalOffsetParticles.put(entry.getKey(), entry.getValue().copy());
			}
			
			for (Map.Entry<Integer, ClothPart.OffsetParticle> entry : copyTarget.normalOffsetParticles.entrySet()) {
				this.normalOffsetParticles.put(entry.getKey(), entry.getValue().copy());
			}
			
			this.particleNormals = ImmutableList.copyOf(copyTarget.particleNormals);
			this.parentJoint = copyTarget.parentJoint;
		}
		
		public ClothObject captureMyself() {
			return new ClothObject(this);
		}
		
		private static final int SUB_STEPS = 6;
		private static final Vec3f EXTERNAL_FORCE = new Vec3f();
		private static final Vec3f OFFSET = new Vec3f();
		private static final Vec3f CENTRIFUGAL = new Vec3f();
		private static final Vec3f CIRCULAR = new Vec3f();
		
		private static final OpenMatrix4f[] BOUND_ANIMATION_TRANSFORM = OpenMatrix4f.allocateMatrixArray(EpicFightSharedConstants.MAX_JOINTS);
		private static final OpenMatrix4f COLLIDER_TRANSFORM = new OpenMatrix4f();
		private static final OpenMatrix4f TO_CENTRIFUGAL = new OpenMatrix4f();
		private static final OpenMatrix4f OBJECT_TRANSFORM = new OpenMatrix4f();
		private static final OpenMatrix4f INVERTED = new OpenMatrix4f();
		private static final Quaternionf ROTATOR = new Quaternionf();
		
		/**
		 * This method needs be called before drawing simulated cloth
		 */
		public void tick(ClothSimulatable simulatableObj, Function<Float, OpenMatrix4f> colliderTransformGetter, float partialTick, @Nullable Armature armature, @Nullable OpenMatrix4f[] poses) {
			// Configure developer options
			//drawMeshColliders(true);
			//drawNormalOffset(false);
			//drawOutlines(true);
			
			// Revert
			//drawMeshColliders(false);
			//drawNormalOffset(true);
			//drawOutlines(false);
			
			if (!Minecraft.getInstance().isPaused()) {
				boolean skinned = poses != null && armature != null;
				
				for (int j = 0; j < armature.getJointNumber(); j++) {
					if (skinned) {
						BOUND_ANIMATION_TRANSFORM[j].load(poses[j]);
						BOUND_ANIMATION_TRANSFORM[j].mulBack(armature.searchJointById(j).getToOrigin());
						BOUND_ANIMATION_TRANSFORM[j].removeScale();
					}
				}
				
				float deltaFrameTime = Minecraft.getInstance().getTimer().getGameTimeDeltaTicks();
				float subStebInvert = 1.0F / SUB_STEPS;
				float subSteppingDeltaTime = deltaFrameTime * subStebInvert;
				float gravity = simulatableObj.getGravity() * subSteppingDeltaTime * EpicFightSharedConstants.A_TICK;
				
				// Update circular force
				float yRot = Mth.wrapDegrees(Mth.rotLerp(partialTick, Mth.wrapDegrees(simulatableObj.getYRotO()), Mth.wrapDegrees(simulatableObj.getYRot())));
				
				TO_CENTRIFUGAL.load(BOUND_ANIMATION_TRANSFORM[this.parentJoint.getId()]);
				TO_CENTRIFUGAL.mulFront(OpenMatrix4f.createRotatorDeg(-yRot + 180.0F, Vec3f.Y_AXIS));
				TO_CENTRIFUGAL.toQuaternion(ROTATOR);
				
				Vec3 velocity = simulatableObj.getObjectVelocity();
				float delta = MathUtils.wrapRadian(MathUtils.getAngleBetween(this.rotationO, ROTATOR));
				float speed = Math.min((float)velocity.length() * deltaFrameTime, 0.2F);
				float rotationForce = Math.abs(delta);
				
				this.rotationO.set(ROTATOR);
				
				OpenMatrix4f.transform3v(TO_CENTRIFUGAL, Vec3f.Z_AXIS, CENTRIFUGAL);
				int deltaSign = Math.abs(delta) < 0.02D ? 0 : MathUtils.getSign(delta);
				
				if (deltaSign == 0) {
					CIRCULAR.set(Vec3f.ZERO);
				} else {
					Vec3f.sub(CENTRIFUGAL, this.centrifugalO, CIRCULAR);
					CIRCULAR.normalize();
				}
				
				this.centrifugalO.set(CENTRIFUGAL);
				
				CENTRIFUGAL.scale(rotationForce * (1.0F + speed * 50.0F));
				CIRCULAR.scale(rotationForce * (1.0F + speed * 50.0F));
				velocity = velocity.scale(rotationForce);
				
				Vec3f.add(CIRCULAR, CENTRIFUGAL, EXTERNAL_FORCE);
				EXTERNAL_FORCE.add(velocity);
				
				// Reset normal vectors
				this.particleNormals.forEach((poseNormals) -> poseNormals.values().forEach((vec3f) -> vec3f.set(0.0F, 0.0F, 0.0F)));
				
				Vec3 pos = simulatableObj.getAccuratePartialLocation(partialTick);
				float yRotLerp = simulatableObj.getAccurateYRot(partialTick);
				OpenMatrix4f objectTransform = OpenMatrix4f.ofTranslation((float)pos.x, (float)pos.y, (float)pos.z, OBJECT_TRANSFORM).rotateDeg(180.0F - yRotLerp, Vec3f.Y_AXIS);
				OpenMatrix4f.invert(objectTransform, INVERTED);
				
				for (ClothPart part : this.parts.values()) {
					part.tick(objectTransform, EXTERNAL_FORCE, skinned ? BOUND_ANIMATION_TRANSFORM : null);
				}
				
				for (int i = 0; i < SUB_STEPS; i++) {
					float substepPartialTick = partialTick - deltaFrameTime + subSteppingDeltaTime * (i + 1);
					
					if (this.clothColliders != null) {
						simulatableObj.getArmature().setPose(simulatableObj.getSimulatableAnimator().getPose(Mth.clamp(substepPartialTick, 0.0F, 1.0F)));
						OpenMatrix4f colliderTransform = colliderTransformGetter.apply(substepPartialTick);
						
						for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
							entry.getSecond().transform(OpenMatrix4f.mul(colliderTransform, entry.getFirst().apply(simulatableObj), COLLIDER_TRANSFORM));
						}
					}
					
					for (ClothPart part : this.parts.values()) {
						part.substepTick(gravity, subSteppingDeltaTime, i + 1, this.clothColliders);
					}
				}
			}
			
			this.updateNormal(false);
			
			// Update normals & offset particles
			if (!this.normalOffsetParticles.isEmpty()) {
				for (ClothPart.OffsetParticle offsetParticle : this.normalOffsetParticles.values()) {
					// Update offset positions
					Particle rootParticle = offsetParticle.rootParticle();
					Map<Integer, Vec3f> rootNormalMap = this.particleNormals.get(rootParticle.meshVertexId);
					OFFSET.set(0.0F, 0.0F, 0.0F);
					
					for (Integer normIdx : offsetParticle.positionNormalMembers()) {
						OFFSET.add(rootNormalMap.get(normIdx).normalize());
					}
					
					OFFSET.scale(offsetParticle.length / OFFSET.length());
					
					offsetParticle.position.set(
						  rootParticle.position.x - OFFSET.x
						, rootParticle.position.y - OFFSET.y
						, rootParticle.position.z - OFFSET.z
					);
				}
			}
			
			this.updateNormal(true);
			this.captureModelPosition(INVERTED);
		}
		
		private static final Vec3f TO_P2 = new Vec3f();
		private static final Vec3f TO_P3 = new Vec3f();
		private static final Vec3f CROSS = new Vec3f();
		
		// Calculate vertex normals
		private void updateNormal(boolean updateOffsetParticles) {
			SoftBodyTranslatable softBodyMesh = this.provider;
			
			for (MeshPart modelPart : softBodyMesh.getOriginalMesh().getAllParts()) {
				for (int i = 0; i < modelPart.getVertices().size() / 3; i++) {
					VertexBuilder triP1 = modelPart.getVertices().get(i * 3);
					VertexBuilder triP2 = modelPart.getVertices().get(i * 3 + 1);
					VertexBuilder triP3 = modelPart.getVertices().get(i * 3 + 2);
					
					if (!this.particles.containsKey(triP1.position) || !this.particles.containsKey(triP2.position) || !this.particles.containsKey(triP3.position)) {
						if (!updateOffsetParticles) {
							continue;
						}
					} else {
						if (updateOffsetParticles) {
							continue;
						}
					}
					
					Vec3f p1Pos = this.getParticlePosition(triP1.position);
					Vec3f p2Pos = this.getParticlePosition(triP2.position);
					Vec3f p3Pos = this.getParticlePosition(triP3.position);
					
					Vec3f.cross(Vec3f.sub(p2Pos, p1Pos, TO_P2), Vec3f.sub(p3Pos, p1Pos, TO_P3), CROSS);
					CROSS.normalize();
					
					Map<Integer, Vec3f> triP1Normals = particleNormals.get(triP1.position);
					Map<Integer, Vec3f> triP2Normals = particleNormals.get(triP2.position);
					Map<Integer, Vec3f> triP3Normals = particleNormals.get(triP3.position);
					
					triP1Normals.get(triP1.normal).add(CROSS);
					triP2Normals.get(triP2.normal).add(CROSS);
					triP3Normals.get(triP3.normal).add(CROSS);
				}
			}
		}
		
		private static final Vec3f SCALE = new Vec3f();
		
		public void scaleFromPose(PoseStack poseStack, OpenMatrix4f[] poses) {
			OpenMatrix4f poseMat = poses[this.parentJoint.getId()];
			poseMat.toScaleVector(SCALE);
			
			poseStack.translate(poseMat.m30, poseMat.m31, poseMat.m32);
			poseStack.scale(SCALE.x, SCALE.y, SCALE.z);
			poseStack.translate(-poseMat.m30, -poseMat.m31, -poseMat.m32);
		}
		
		@Override
		public void draw(PoseStack poseStack, VertexConsumer bufferBuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			if (DRAW_OUTLINES) {
				this.drawOutline(poseStack, Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()), Mesh.DrawingFunction.POSITION_COLOR_NORMAL, r, g, b, a);
				//part.drawNormals(poseStack, Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()), Mesh.DrawingFunction.POSITION_COLOR_NORMAL, r, g, b, a);
			} else {
				this.drawParts(poseStack, bufferBuilder, drawingFunction, packedLight, r, g, b, a, overlay);
			}
			
			if (this.provider instanceof CompositeMesh compositeMesh) {
				poseStack.popPose();
				compositeMesh.getStaticMesh().draw(poseStack, bufferBuilder, drawingFunction, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay);
				poseStack.pushPose();
			}
		}
		
		private static final Vector3f SCALER = new Vector3f();
		
		@Override
		public void drawPosed(PoseStack poseStack, VertexConsumer bufferBuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay, Armature armature, OpenMatrix4f[] poses) {
			if (DRAW_OUTLINES) {
				this.drawOutline(poseStack, Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()), Mesh.DrawingFunction.POSITION_COLOR_NORMAL, r, g, b, a);
				//part.drawNormals(poseStack, Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines()), Mesh.DrawingFunction.POSITION_COLOR_NORMAL, r, g, b, a);
			} else {
				this.drawParts(poseStack, bufferBuilder, drawingFunction, packedLight, r, g, b, a, overlay);
			}
			
			if (DRAW_MESH_COLLIDERS && this.clothColliders != null) {
				for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : this.clothColliders) {
					entry.getSecond().draw(poseStack, Minecraft.getInstance().renderBuffers().bufferSource(), 0xFFFFFFFF);
				}
			}
			
			// Remove entity inverted world translation while keeping the scale
			poseStack.last().pose().getScale(SCALER);
			float scaleX = SCALER.x;
			float scaleY = SCALER.y;
			float scaleZ = SCALER.z;
			
			poseStack.popPose();
			poseStack.last().pose().getScale(SCALER);
			float xDiv = scaleX / SCALER.x;
			float yDiv = scaleY / SCALER.y;
			float zDiv = scaleZ / SCALER.z;
			
			poseStack.scale(xDiv, yDiv, zDiv);
			
			if (this.provider instanceof CompositeMesh compositeMesh) {
				compositeMesh.getStaticMesh().drawPosed(poseStack, bufferBuilder, drawingFunction, packedLight, 1.0F, 1.0F, 1.0F, 1.0F, overlay, armature, poses);
			}
			
			poseStack.pushPose();
		}
		
		public Vec3f getParticlePosition(int idx) {
			if (this.particles.containsKey(idx)) {
				return this.particles.get(idx).position;
			} else {
				return this.normalOffsetParticles.get(idx).position;
			}
		}
		
		private void captureModelPosition(OpenMatrix4f objectTranslformInv) {
			for (Particle p : this.particles.values()) {
				OpenMatrix4f.transform3v(objectTranslformInv, p.position, p.modelPosition);
			}
		}
		
		public void drawParts(PoseStack poseStack, VertexConsumer bufferBuilder, Mesh.DrawingFunction drawingFunction, int packedLight, float r, float g, float b, float a, int overlay) {
			SoftBodyTranslatable softBodyMesh = ClothObject.this.provider;
			float[] uvs = softBodyMesh.getOriginalMesh().uvs();
			
			for (MeshPart meshPart : softBodyMesh.getOriginalMesh().getAllParts()) {
				if (meshPart.isHidden()) {
					continue;
				}
				
				Vector4f color = meshPart.getColor(r, g, b, a);
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				for (int i = 0; i < meshPart.getVertices().size(); i++) {
					if (!DRAW_NORMAL_OFFSET && i % 3 == 0) {
						if (i + 1 == meshPart.getVertices().size() || i + 2 == meshPart.getVertices().size()) {
							
						} else {
							VertexBuilder v1 = meshPart.getVertices().get(i);
							VertexBuilder v2 = meshPart.getVertices().get(i + 1);
							VertexBuilder v3 = meshPart.getVertices().get(i + 2);
							
							if ((!this.particles.containsKey(v1.position) || !this.particles.containsKey(v2.position) || !this.particles.containsKey(v3.position))) {
								i += 2;
								continue;
							}
						}
					}
					
					VertexBuilder vb = meshPart.getVertices().get(i);
					Vec3f particlePosition = this.getParticlePosition(vb.position);
					Vec3f poseNormal = this.particleNormals.get(vb.position).get(vb.normal);
					poseNormal.normalize();
					
					POSITION.set(particlePosition.x, particlePosition.y, particlePosition.z);
					NORMAL.set(poseNormal.x, poseNormal.y, poseNormal.z);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					
					drawingFunction.draw(bufferBuilder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), packedLight, color.x, color.y, color.z, color.w, uvs[vb.uv * 2], uvs[vb.uv * 2 + 1], overlay);
				}
			}
		}
		
		public void drawOutline(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, float r, float g, float b, float a) {
			SoftBodyTranslatable softBodyMesh = ClothObject.this.provider;
			
			for (MeshPart meshPart : softBodyMesh.getOriginalMesh().getAllParts()) {
				if (meshPart.isHidden()) {
					continue;
				}
				
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				for (int i = 0; i < meshPart.getVertices().size() / 3; i++) {
					VertexBuilder v1 = meshPart.getVertices().get(i * 3);
					VertexBuilder v2 = meshPart.getVertices().get(i * 3 + 1);
					VertexBuilder v3 = meshPart.getVertices().get(i * 3 + 2);
					
					if (!DRAW_NORMAL_OFFSET && (!this.particles.containsKey(v1.position) || !this.particles.containsKey(v2.position) || !this.particles.containsKey(v3.position))) {
						continue;
					}
					
					Vec3f pos1 = this.getParticlePosition(v1.position);
					Vec3f pos2 = this.getParticlePosition(v2.position);
					Vec3f pos3 = this.getParticlePosition(v3.position);
					
					POSITION.set(pos1.x, pos1.y, pos1.z);
					NORMAL.set(pos2.x - pos1.x, pos2.x - pos1.x, pos2.x - pos1.x);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					POSITION.set(pos2.x, pos2.y, pos2.z);
					POSITION.mul(matrix4f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					
					POSITION.set(pos2.x, pos2.y, pos2.z);
					NORMAL.set(pos3.x - pos2.x, pos3.x - pos2.x, pos3.x - pos2.x);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					POSITION.set(pos3.x, pos3.y, pos3.z);
					POSITION.mul(matrix4f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					
					POSITION.set(pos3.x, pos3.y, pos3.z);
					NORMAL.set(pos1.x - pos3.x, pos1.x - pos3.x, pos1.x - pos3.x);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					POSITION.set(pos1.x, pos1.y, pos1.z);
					POSITION.mul(matrix4f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
				}
			}
		}
		
		public void drawNormals(PoseStack poseStack, VertexConsumer builder, Mesh.DrawingFunction drawingFunction, float r, float g, float b, float a) {
			if (!this.normalOffsetParticles.isEmpty()) {
				Matrix4f matrix4f = poseStack.last().pose();
				Matrix3f matrix3f = poseStack.last().normal();
				
				for (ClothPart.OffsetParticle offsetParticle : this.normalOffsetParticles.values()) {
					// Update offset positions
					Particle rootParticle = offsetParticle.rootParticle();
					Map<Integer, Vec3f> rootNormalMap = this.particleNormals.get(rootParticle.meshVertexId);
					
					if (rootNormalMap.size() < 2) {
						continue;
					}
					
					OFFSET.set(0.0F, 0.0F, 0.0F);
					
					for (Integer normIdx : offsetParticle.positionNormalMembers()) {
						OFFSET.add(rootNormalMap.get(normIdx).normalize());
					}
					
					OFFSET.scale(offsetParticle.length / OFFSET.length());
					
					Vec3f rootpos = this.getParticlePosition(rootParticle.meshVertexId);
					
					POSITION.set(rootpos.x, rootpos.y, rootpos.z);
					NORMAL.set(-OFFSET.x, -OFFSET.x, -OFFSET.x);
					POSITION.mul(matrix4f);
					NORMAL.mul(matrix3f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
					POSITION.set(rootpos.x - OFFSET.x, rootpos.y - OFFSET.y, rootpos.z - OFFSET.z);
					POSITION.mul(matrix4f);
					drawingFunction.draw(builder, POSITION.x, POSITION.y, POSITION.z, NORMAL.x(), NORMAL.y(), NORMAL.z(), -1, r, g, b, a, 0, 0, 0);
				}
			}
		}
		
		@Override
		public void initialize() {
		}
		
		class Particle {
			final Vec3f position;
			final Vec3f modelPosition;
			final Vec3f velocity = new Vec3f();
			
			final float influence;
			final float rootDistance;
			final int meshVertexId;
			boolean collided;
			
			Particle(Vec3f position, float influence, float rootDistance, int meshVertexId) {
				this.position = position;
				this.modelPosition = position.copy();
				this.influence = influence;
				this.rootDistance = rootDistance;
				this.meshVertexId = meshVertexId;
				this.collided = false;
			}
			
			Particle copy() {
				return new Particle(this.position.copy(), this.influence, this.rootDistance, this.meshVertexId);
			}
		}
		
		public class ClothPart {
			final List<Particle> particleList;
			final List<ConstraintList> constraints;
			final Multimap<Integer, Particle> spatialHash;
			final float selfCollision;
			final float particleMass;
			final int hashTableSize;
			
			private static final Vec3f AVERAGE = new Vec3f();
			
			ClothPart(SoftBodyTranslatable.ClothSimulationInfo clothInfo, float[] positions) {
				this.particleList = Lists.newArrayList();
				ImmutableList.Builder<ConstraintList> constraintsBuilder = ImmutableList.builder();
				
				this.selfCollision = clothInfo.selfCollision();
				this.particleMass = clothInfo.particleMass();
				
				/**
				 * AddEntity particles
				 */
				for (int i = 0; i < clothInfo.particles().length / 2; i++) {
					int positionIndex = clothInfo.particles()[i * 2];
					int weightIndex = clothInfo.particles()[i * 2 + 1];
					float influence = clothInfo.weights()[weightIndex];
					float rootDistance = clothInfo.rootDistance()[i];
					float x = positions[positionIndex * 3];
					float y = positions[positionIndex * 3 + 1];
					float z = positions[positionIndex * 3 + 2];
					
					Particle particle = new Particle(new Vec3f(x, y, z), influence, rootDistance, positionIndex);
					ClothObject.this.particles.put(positionIndex, particle);
					this.particleList.add(particle);
				}
				
				this.hashTableSize = this.particleList.size() * 2;
				this.spatialHash = HashMultimap.create(this.hashTableSize, 2);
				int idx = 0;
				
				/**
				 * AddEntity constraints
				 */
				for (int[] constraints : clothInfo.constraints()) {
					float compliance = clothInfo.compliances()[idx];
					ConstraintType constraintType = clothInfo.constraintTypes()[idx];
					List<Constraint> constraintList;
					idx++;
					
					switch(constraintType) {
					case STRETCHING -> {
						constraintList = new ArrayList<> (constraints.length / 2);
						
						for (int i = 0; i < constraints.length / 2; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							
							constraintList.add(new StretchingConstraint(ClothObject.this.particles.get(idx1), ClothObject.this.particles.get(idx2)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintType, constraintList));
					}
					case SHAPING -> {
						constraintList = new ArrayList<> (constraints.length / 2);
						
						for (int i = 0; i < constraints.length / 2; i++) {
							int idx1 = constraints[i * 2];
							int idx2 = constraints[i * 2 + 1];
							
							constraintList.add(new ShapingConstraint(ClothObject.this.particles.get(idx1), ClothObject.this.particles.get(idx2)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintType, constraintList));
					}
					case BENDING -> {
						constraintList = new ArrayList<> (constraints.length / 4);
						
						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 4];
							int idx2 = constraints[i * 4 + 1];
							int idx3 = constraints[i * 4 + 2];
							int idx4 = constraints[i * 4 + 3];
							
							constraintList.add(new BendingConstraint(ClothObject.this.particles.get(idx1), ClothObject.this.particles.get(idx2), ClothObject.this.particles.get(idx3), ClothObject.this.particles.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintType, constraintList));
					}
					case VOLUME -> {
						constraintList = new ArrayList<> (constraints.length / 4);
						
						for (int i = 0; i < constraints.length / 4; i++) {
							int idx1 = constraints[i * 4];
							int idx2 = constraints[i * 4 + 1];
							int idx3 = constraints[i * 4 + 2];
							int idx4 = constraints[i * 4 + 3];
							
							constraintList.add(new VolumeConstraint(ClothObject.this.particles.get(idx1), ClothObject.this.particles.get(idx2), ClothObject.this.particles.get(idx3), ClothObject.this.particles.get(idx4)));
						}
						
						constraintsBuilder.add(new ConstraintList(compliance, constraintType, constraintList));
					}
					}
				}
				
				this.constraints = constraintsBuilder.build();
				
				/**
				 * Setup normal offsets
				 */
				if (clothInfo.normalOffsetMapping() != null) {
					for (int i = 0; i < clothInfo.normalOffsetMapping().length / 2; i++) {
						int rootParticle = clothInfo.normalOffsetMapping()[i * 2];
						int offsetParticleIdx = clothInfo.normalOffsetMapping()[i * 2 + 1];
						Vec3f offsetDirection = new Vec3f( positions[offsetParticleIdx * 3] - positions[rootParticle * 3]
														 , positions[offsetParticleIdx * 3 + 1] - positions[rootParticle * 3 + 1]
														 , positions[offsetParticleIdx * 3 + 2] - positions[rootParticle * 3 + 2]);
						
						List<Integer> positionNormalMembers = Lists.newArrayList();
						List<Integer> inverseNormals = Lists.newArrayList();
						OffsetParticle offsetParticle = new OffsetParticle(offsetParticleIdx, offsetDirection.length(), ClothObject.this.particles.get(rootParticle), new Vec3f(), positionNormalMembers, inverseNormals);
						offsetDirection.normalize();
						
						Map<Integer, Vec3f> rootNormalMap = particleNormals.get(rootParticle);
						List<Vec3f> rootNormals = new ArrayList<> (rootNormalMap.values());
						List<Set<Integer>> normalSubsets = new ArrayList<> (MathUtils.getSubset(IntStream.rangeClosed(0, rootNormals.size() - 1).boxed().toList()));
						int candidate = -1;
						int loopIdx = 0;
						float maxDot = -10000.0F;
						
						for (Set<Integer> subset : normalSubsets) {
							Set<Vec3f> rootNormal = subset.stream().map((normIdx) -> rootNormals.get(normIdx)).collect(Collectors.toSet());
							Vec3f.average(rootNormal, AVERAGE);
							AVERAGE.scale(-1.0F);
							AVERAGE.normalize();
							
							float dot = Vec3f.dot(offsetDirection, AVERAGE);
							if (maxDot < dot) {
								maxDot = dot;
								candidate = loopIdx;
							}
							
							loopIdx++;
						}
						
						normalSubsets.get(candidate).forEach((orderIdx) -> {
							int iterCount = 0;
							Iterator<Map.Entry<Integer, Vec3f>> iter = rootNormalMap.entrySet().iterator();
							
							while (iter.hasNext()) {
								Map.Entry<Integer, Vec3f> entry = iter.next();
								
								if (orderIdx == iterCount) {
									positionNormalMembers.add(entry.getKey());
									break;
								}
								
								iterCount++;
							}
						});
						
						normalOffsetParticles.put(offsetParticleIdx, offsetParticle);
						
						for (Vec3f normal : particleNormals.get(offsetParticleIdx).values()) {
							int leastDotIdx = MathUtils.getLeastAngleVectorIdx(normal, rootNormals.toArray(new Vec3f[0]));
							int iterCount = 0;
							Iterator<Map.Entry<Integer, Vec3f>> iter = rootNormalMap.entrySet().iterator();
							
							while (iter.hasNext()) {
								Map.Entry<Integer, Vec3f> entry = iter.next();
								
								if (leastDotIdx == iterCount) {
									inverseNormals.add(entry.getKey());
									break;
								}
								
								iterCount++;
							}
						}
					}
				}
			}
			
			public void buildSpatialHash() {
				this.spatialHash.clear();
				
				// Create spatial hash map
				for (Particle p : this.particleList) {
					int hash = this.getHash(p.position.x, p.position.y, p.position.z);
					this.spatialHash.put(hash, p);
				}
			}
			
			// Storage vectors
			private static final Vec3f VEC3F = new Vec3f();
			private static final Vector4f POSITION = new Vector4f(0.0F, 0.0F, 0.0F, 1.0F);
			private static final Vec3f DIFF = new Vec3f();
			
			// Setup root particles transform
			public void tick(OpenMatrix4f objectTransform, Vec3f externalForce, OpenMatrix4f[] poses) {
				for (Particle p : this.particleList) {
					p.velocity.scale(0.92F);
					
					p.velocity.add(
						  externalForce.x * p.rootDistance * p.influence * this.particleMass
						, externalForce.y * p.rootDistance * p.influence * this.particleMass
						, externalForce.z * p.rootDistance * p.influence * this.particleMass
					);
					
					if (p.collided) {
						VEC3F.set(p.modelPosition);
						OpenMatrix4f.transform3v(objectTransform, VEC3F, TRASNFORMED);
						p.position.set(TRASNFORMED);
					} else {
						float influenceInv = 1.0F - p.influence;
						
						// Apply animation transform
						if (influenceInv > 0.0F) {
							ClothObject.this.provider.getOriginalMesh().getVertexPosition(p.meshVertexId, POSITION, poses);
							VEC3F.set(POSITION.x, POSITION.y, POSITION.z);
							OpenMatrix4f.transform3v(objectTransform, VEC3F, TRASNFORMED);
							Vec3f.interpolate(p.position, TRASNFORMED, influenceInv, TRASNFORMED);
							p.position.set(TRASNFORMED);
						}
					}
				}
			}
			
			private static final Vec3f PARTIAL_VELOCITY = new Vec3f();
			
			// Apply external forces, constraints, self collision, and mesh collision
			public void substepTick(float substepGravity, float substepDeltaTime, int stepCount, List<Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider>> clothColliders) {
				for (Particle p : this.particleList) {
					p.position.y -= substepGravity * this.particleMass * p.influence;
					p.position.add(Vec3f.scale(p.velocity, PARTIAL_VELOCITY, 1.0F / SUB_STEPS));
				}
				
				for (ConstraintList constraintsBundle : this.constraints) {
					float alpha = constraintsBundle.compliance() / (substepDeltaTime * substepDeltaTime);
					
					for (Constraint c : constraintsBundle.constraints()) {
						c.solve(alpha, stepCount);
					}
				}
				
				if (stepCount == 1) {
					this.buildSpatialHash();
				}
				
				// Detect self collision
				for (Particle p1 : this.particleList) {
					int hash = this.getHash(p1.position.x, p1.position.y, p1.position.z);
					
					for (Particle p2 : this.spatialHash.get(hash)) {
						if (p1 == p2) {
							continue;
						}
						
						float influenceSum = p1.influence + p2.influence;
						
						if (influenceSum == 0.0F) {
							continue;
						}
						
						Vec3f.sub(p1.position, p2.position, VEC3F);
						float length = VEC3F.length();
						
						if (length < this.selfCollision) {
							float scale = (this.selfCollision - length) / this.selfCollision;
							float p1Move = p1.influence / influenceSum;
							float p2Move = p2.influence / influenceSum;
							VEC3F.scale(scale);
							
							p1.position.add(VEC3F.x * p1Move, VEC3F.y * p1Move, VEC3F.z * p1Move);
							p2.position.sub(VEC3F.x * p2Move, VEC3F.y * p2Move, VEC3F.z * p2Move);
						}
					}
				}
				
				// Detect collision with mesh collider
				if (clothColliders != null) {
					for (ConstraintList constraintList : this.constraints) {
						if (constraintList.constraintType() == ConstraintType.SHAPING) {
							@SuppressWarnings("unchecked")
							List<ShapingConstraint> constraints = (List<ShapingConstraint>)constraintList.constraints();
							List<ClothSimulator.ClothOBBCollider> colliders = Lists.newArrayList();
							List<Vec3f> destinations = Lists.newArrayList();
							
							for (ShapingConstraint constraint : constraints) {
								if (constraint.p1.influence == 0.0F && constraint.p2.influence == 0.0F) {
									continue;
								}
								
								for (Pair<Function<ClothSimulatable, OpenMatrix4f>, ClothSimulator.ClothOBBCollider> entry : clothColliders) {
									ClothSimulator.ClothOBBCollider clothCollider = entry.getSecond();
									
									if (clothCollider.getOuterAABB(this.selfCollision * 0.5F).contains(constraint.p2.position.x, constraint.p2.position.y, constraint.p2.position.z)) {
										if (!clothCollider.doesPointCollide(constraint.p1.position.toDoubleVector(), this.selfCollision * 0.5F)) {
											colliders.add(entry.getSecond());
										}
									}
								}
								
								for (ClothSimulator.ClothOBBCollider collider : colliders) {
									collider.pushIfPointInside(constraint.p2.position, constraint.p1.position, this.selfCollision * 0.5F, destinations, colliders);
									//collider.pushIfEdgeCollidesCircular(constraint, this.selfCollision * 0.5F, destinations, colliders);
								}
								/**
								int idx = Vec3f.getMostSimilar(constraint.p1.position, constraint.p2.position, destinations);
								
								if (idx != -1) {
									Vec3f mostSimilar = destinations.get(idx);
									constraint.p2.position.set(mostSimilar);
								}
								**/
								int i = Vec3f.getNearest(constraint.p2.position, destinations);
								constraint.p2.collided = i != -1;
								
								if (i != -1) {
									Vec3f nearest = destinations.get(i);
									Vec3f.sub(nearest, constraint.p2.position, DIFF);
									
									//constraint.p2.velocity.add(DIFF);
									constraint.p2.position.set(nearest);
								}
								
								colliders.clear();
								destinations.clear();
							}
						}
					}
				}
			}
			
			private int getHash(double x, double y, double z) {
				int xi = (int)Math.floor(x / SPATIAL_HASH_SPACING);
				int yi = (int)Math.floor(y / SPATIAL_HASH_SPACING);
				int zi = (int)Math.floor(z / SPATIAL_HASH_SPACING);
				int hash = (xi * 92837111) ^ (yi * 689287499) ^ (zi * 283923481);
				
				return Math.abs(hash) % this.hashTableSize;
			}
			
			public enum ConstraintType {
				STRETCHING, SHAPING, BENDING, VOLUME
			}
			
			public static record ConstraintList(float compliance, ConstraintType constraintType, List<? extends Constraint> constraints) {
			}
			
			public static record OffsetParticle(int offsetVertexId, float length, Particle rootParticle, Vec3f position, List<Integer> positionNormalMembers, List<Integer> inverseNormal) {
				public OffsetParticle copy() {
					return new OffsetParticle(this.offsetVertexId, this.length, this.rootParticle, this.position.copy(), this.positionNormalMembers, this.inverseNormal);
				}
			}
			
			abstract class Constraint {
				abstract void solve(float alpha, int stepcount);
			}
			
			/**
			 * A constraint that restricts stretching of two particles
			 */
			class StretchingConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final float restLength;
				
				// Storage vector
				static final Vec3f GRADIENT = new Vec3f();
				
				StretchingConstraint(Particle p1, Particle p2) {
					this.p1 = p1;
					this.p2 = p2;
					this.restLength = p1.position.distance(p2.position);
				}
				
				@Override
				void solve(float alpha, int stepcount) {
					float p1Influence = this.p1.influence;
					float p2Influence = this.p2.influence;
				    float influenceSum = p1Influence + p2Influence;
				    
				    if (influenceSum < 1E-8) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, GRADIENT);
				    float currentLength = GRADIENT.length();
				    
				    if (currentLength < 1E-8) {
				        return;
				    }
				    
				    // Normalize
				    GRADIENT.scale(1.0F / currentLength);
				    
				    float constraint = currentLength - this.restLength;
				    float force = constraint / (influenceSum + alpha);
				    float p1Move = force * p1Influence;
				    float p2Move = -force * p2Influence;
				    
				    this.p1.position.add(GRADIENT.x * p1Move, GRADIENT.y * p1Move, GRADIENT.z * p1Move);
				    this.p2.position.add(GRADIENT.x * p2Move, GRADIENT.y * p2Move, GRADIENT.z * p2Move);
				}
			}
			
			/**
			 * A constraint that restricts stretching of two particles, and doesn't allow stretching over the rest length.
			 * 
			 * Be used to prevent streching too much in gravity direction in low fps
			 */
			class ShapingConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final float restLength;
				
				// Storage vector
				static final Vec3f TOWARD = new Vec3f();
				
				ShapingConstraint(Particle p1, Particle p2) {
					this.p1 = p1;
					this.p2 = p2;
					this.restLength = p1.position.distance(p2.position);
				}
				
				@Override
				void solve(float alpha, int stepcount) {
					float p1Influence = (stepcount == SUB_STEPS && !this.p1.collided) ? 0.0F : this.p1.influence;
					float p2Influence = this.p2.influence;
					
				    float influenceSum = p1Influence + p2Influence;
				    
				    if (influenceSum < 1E-5) {
				        return;
				    }
				    
				    Vec3f.sub(this.p2.position, this.p1.position, TOWARD);
				    float distanceLength = TOWARD.length();
				    
				    if (distanceLength == 0.0F) {
				        return;
				    }
				    
				    //Normalize
				    TOWARD.scale(1.0F / distanceLength);
				    float distanceGap = distanceLength - this.restLength;
				    float force = distanceGap / (influenceSum + alpha);
				    float p1Move = force * p1Influence;
				    float p2Move = -force * p2Influence;
				    
				    this.p1.position.add(TOWARD.x * p1Move, TOWARD.y * p1Move, TOWARD.z * p1Move);
				    this.p2.position.add(TOWARD.x * p2Move, TOWARD.y * p2Move, TOWARD.z * p2Move);
				}
			}
			
			/**
			 * A constraint that restricts bending of member particles. p2, p3 are adjacent edge particles, and p1, p4 are opponent each other
			 */
			class BendingConstraint extends Constraint {
				final Particle p1;
				final Particle p2;
				final Particle p3;
				final Particle p4;
				final float restAngle;
				final float oppositeDistance;
				
				// Storage vector
				static final Vec3f[] GRADIENTS = { new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f() };
				static final Vec3f NORMAL_SUM = new Vec3f();
				static float STIFFNESS = 1.0F;
				
				BendingConstraint(Particle p1, Particle p2, Particle p3, Particle p4) {
					this.p1 = p1;
					this.p2 = p2;
					this.p3 = p3;
					this.p4 = p4;
					this.restAngle = this.getDihedralAngle();
					this.oppositeDistance = Vec3f.sub(this.p1.position, this.p4.position, null).lengthSqr(); 
				}
				
				@Override
				void solve(float alpha, int stepcount) {
				    float influenceSum = this.p1.influence + this.p2.influence + this.p3.influence + this.p4.influence;
				    
				    if (influenceSum < 1E-8) {
				        return;
				    }
				    
					float currentAngle = this.getDihedralAngle();
				    float constraint = (this.restAngle - currentAngle);
				    
				    while (constraint > Math.PI) {
				    	constraint -= Math.PI * 2;
				    }
				    
				    while (constraint < -Math.PI) {
				    	constraint += Math.PI * 2;
				    }
				    
				    // radian angle * diameter
				    constraint = this.oppositeDistance * constraint;
				    
				    float edgeLength = EDGE.length();
				    
				    CROSS1.scale(edgeLength);
				    CROSS2.scale(edgeLength);
				    GRADIENTS[0].set(CROSS1);
				    GRADIENTS[3].set(CROSS2);
				    
				    Vec3f.add(CROSS1, CROSS2, NORMAL_SUM);
				    NORMAL_SUM.scale(-0.5F);
				    GRADIENTS[1].set(NORMAL_SUM);
				    GRADIENTS[2].set(NORMAL_SUM);
				    
				    float weight = this.p1.influence * GRADIENTS[0].lengthSqr()
				    				+ this.p2.influence * GRADIENTS[1].lengthSqr()
				    				+ this.p3.influence * GRADIENTS[2].lengthSqr()
				    				+ this.p4.influence * GRADIENTS[3].lengthSqr();
				    
				    if (weight < 1E-8) {
						return;
					}
				    
				    float force = (-constraint * STIFFNESS) / (influenceSum + alpha);
				    
				    GRADIENTS[0].scale(force * this.p1.influence);
				    GRADIENTS[1].scale(force * this.p2.influence);
				    GRADIENTS[2].scale(force * this.p3.influence);
				    GRADIENTS[3].scale(force * this.p4.influence);
				    
				    Vec3f.add(this.p1.position, GRADIENTS[0], this.p1.position);
				    Vec3f.add(this.p2.position, GRADIENTS[1], this.p2.position);
				    Vec3f.add(this.p3.position, GRADIENTS[2], this.p3.position);
				    Vec3f.add(this.p4.position, GRADIENTS[3], this.p4.position);
				}
				
				static final Vec3f P2P1 = new Vec3f();
				static final Vec3f P3P1 = new Vec3f();
				static final Vec3f P4P2 = new Vec3f();
				static final Vec3f P4P3 = new Vec3f();
				static final Vec3f EDGE = new Vec3f();
				static final Vec3f EDGE_NORM = new Vec3f();
				
				static final Vec3f CROSS1 = new Vec3f();
				static final Vec3f CROSS2 = new Vec3f();
				static final Vec3f CROSS3 = new Vec3f();
				
				public float getDihedralAngle() {
					Vec3f.sub(this.p1.position, this.p2.position, P2P1);
					Vec3f.sub(this.p1.position, this.p3.position, P3P1);
					Vec3f.sub(this.p4.position, this.p2.position, P4P2);
					Vec3f.sub(this.p4.position, this.p3.position, P4P3);
					Vec3f.sub(this.p3.position, this.p2.position, EDGE);
					
					Vec3f.cross(P2P1, P3P1, CROSS1);
					Vec3f.cross(P4P3, P4P2, CROSS2);
					CROSS1.normalize();
					CROSS2.normalize();
					Vec3f.normalize(EDGE, EDGE_NORM);
					
					float cos = Vec3f.dot(CROSS1, CROSS2);
					float sin = Vec3f.dot(Vec3f.cross(CROSS1, CROSS2, CROSS3), EDGE_NORM);
					
					return (float)Math.atan2(sin, cos);
				}
			}
			
			/**
			 * A constraint that resists squashing of tetrahedral.
			 * 
			 * Note: This constraint is expensive. Consider using NormalMappedParticle instead.
			 */
			class VolumeConstraint extends Constraint {
				final Particle[] particles;
				final float restVolume;
				
				static final float SUBDIVISION = 1.0F / 6.0F;
				static final int[][] VOLUME_ORDER = { {1, 3, 2}, {0, 2, 3}, {0, 3, 1}, {0, 1, 2} };
				static final Vec3f[] SHRINK_DIRECTIONS = { new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f() };
				
				// Storage vectors
				static final Vec3f P1_TO_P2 = new Vec3f();
				static final Vec3f P1_TO_P3 = new Vec3f();
				static final Vec3f P1_TO_P4 = new Vec3f();
				static final Vec3f TET_CROSS = new Vec3f();
				
				VolumeConstraint(Particle p1, Particle p2, Particle p3, Particle p4) {
					this.particles = new Particle[4];
					this.particles[0] = p1;
					this.particles[1] = p2;
					this.particles[2] = p3;
					this.particles[3] = p4;
					this.restVolume = this.getTetrahedralVolume();
				}

				@Override
				void solve(float alpha, int stepcount) {
					float weight = 0.0F;
					
					for (int i = 0; i < 4; i++) {
						Particle p1 = this.particles[VOLUME_ORDER[i][0]];
						Particle p2 = this.particles[VOLUME_ORDER[i][1]];
						Particle p3 = this.particles[VOLUME_ORDER[i][2]];
						
						Vec3f.sub(p2.position, p1.position, P1_TO_P2);
						Vec3f.sub(p3.position, p1.position, P1_TO_P3);
						Vec3f.cross(P1_TO_P2, P1_TO_P3, SHRINK_DIRECTIONS[i]);
						SHRINK_DIRECTIONS[i].scale(SUBDIVISION);
						
						weight += this.particles[i].influence * SHRINK_DIRECTIONS[i].lengthSqr();
					}
					
					if (weight < 1E-8) {
						return;
					}
					
					float constraint = this.restVolume - this.getTetrahedralVolume();
					float force = constraint / (weight + alpha);
					
					for (int i = 0; i < 4; i++) {
						SHRINK_DIRECTIONS[i].scale(force * this.particles[i].influence);
						Vec3f.add(this.particles[i].position, SHRINK_DIRECTIONS[i], this.particles[i].position);
					}
				}
				
				float getTetrahedralVolume() {
					Vec3f.sub(this.particles[1].position, this.particles[0].position, P1_TO_P2);
					Vec3f.sub(this.particles[2].position, this.particles[0].position, P1_TO_P3);
					Vec3f.sub(this.particles[3].position, this.particles[0].position, P1_TO_P4);
					Vec3f.cross(P1_TO_P2, P1_TO_P3, TET_CROSS);
					
					return Vec3f.dot(TET_CROSS, P1_TO_P4) / 6.0F;
				}
			}
		}
	}
	
	public static class ClothOBBCollider extends OBBCollider {
		public ClothOBBCollider(double vertexX, double vertexY, double vertexZ, double centerX, double centerY, double centerZ) {
			super(vertexX, vertexY, vertexZ, centerX, centerY, centerZ);
		}
		
		public AABB getOuterAABB(float particleRadius) {
			double maxX = -1000000.0D;
			double maxY = -1000000.0D;
			double maxZ = -1000000.0D;
			
			for (Vec3 rotated : this.rotatedVertices) {
				double xdistance = Math.abs(rotated.x);
				
				if (xdistance > maxX) {
					maxX = xdistance;
				}
				
				double ydistance = Math.abs(rotated.y);
				
				if (ydistance > maxY) {
					maxY = ydistance;
				}
				
				double zdistance = Math.abs(rotated.z);
				
				if (zdistance > maxZ) {
					maxZ = zdistance;
				}
			}
			
			maxX += particleRadius;
			maxY += particleRadius;
			maxZ += particleRadius;
			
			return new AABB(-maxX, -maxY, -maxZ, maxX, maxY, maxZ).move(this.worldCenter);
		}
		
		private boolean doesPointCollide(Vec3 point, float radius) {
			Vec3 toOpponent = point.subtract(this.worldCenter);
			
			for (Vec3 seperateAxis : this.rotatedNormals) {
				Vec3 maxProj = null;
				double maxDot = -1000000.0D;
				
				if (seperateAxis.dot(toOpponent) < 0.0D) {
					seperateAxis = seperateAxis.scale(-1.0D);
				}
				
				for (Vec3 vertexVector : this.rotatedVertices) {
					Vec3 toVertex = seperateAxis.dot(vertexVector) > 0.0D ? vertexVector : vertexVector.scale(-1.0D);
					double dot = seperateAxis.dot(toVertex);
					
					if (dot > maxDot || maxProj == null) {
						maxDot = dot;
						maxProj = toVertex;
					}
				}
				
				Vec3 opponentProjection = MathUtils.projectVector(toOpponent, seperateAxis);
				Vec3 vertexProjection = MathUtils.projectVector(maxProj, seperateAxis);
				
				if (opponentProjection.length() > vertexProjection.length() + radius) {
					return false;
				}
			}
			
			return true;
		}
		/**
		public void pushIfEdgePenetrates(ClothSimulator.ClothObject.ClothPart.ShapingConstraint constraint, float particleRadius) {
			Vec3f.sub(constraint.p1.position, constraint.p2.position, UNIT);
			float restLength = UNIT.length();
			
			if (restLength == 0.0F) {
				return;
			}
			
			for (int i = 0; i < RECTANGLES.length; i++) {
				this.getPos(RECTANGLES[i][0], V1);
				this.getPos(RECTANGLES[i][1], V2);
				this.getPos(RECTANGLES[i][2], V3);
				this.getPos(RECTANGLES[i][3], V4);
				
				NORMAL.set(this.rotatedNormals[i / 2]);
				
				if (i % 2 == 1) {
					NORMAL.scale(-1.0F);
				}
				
				getLinePlaneIntersectPoint(constraint.p1.position, UNIT, restLength, NORMAL, INTERSECT);
				
				if (INTERSECT.validateValues()) {
					constraint.p2.position.set(INTERSECT);
				}
			}
		}
		**/
		private static final Vec3f WORLD_CENTER = new Vec3f();
		private static final Vec3f TO_OPPONENT = new Vec3f();
		private static final Vec3f SEP_AXIS = new Vec3f();
		private static final Vec3f TO_VERTEX = new Vec3f();
		private static final Vec3f MAX_PROJ = new Vec3f();
		private static final Vec3f TO_OPPONENT_PROJECTION = new Vec3f();
		private static final Vec3f VERTEX_PROJECTION = new Vec3f();
		private static final Vec3f PROJECTION1 = new Vec3f();
		private static final Vec3f PROJECTION2 = new Vec3f();
		private static final Vec3f PROJECTION3 = new Vec3f();
		private static final Vec3f TO_PLANE1 = new Vec3f();
		private static final Vec3f TO_PLANE2 = new Vec3f();
		private static final Vec3f TO_PLANE3 = new Vec3f();
		
		private final Vec3f[] destinations = { new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f() };
		
		/**
		 * Push back the second particle of shping constraint from OBB
		 * 
		 * @param constraint
		 * @param selfCollision
		 * @param destnations
		 * @param others
		 */
		public void pushIfPointInside(Vec3f point, Vec3f root, float selfCollision, List<Vec3f> destnations, List<ClothSimulator.ClothOBBCollider> others) {
			WORLD_CENTER.set(this.worldCenter);
			Vec3f.sub(point, WORLD_CENTER, TO_OPPONENT);
			
			int order = 0;
			
			for (Vec3 seperateAxis : this.rotatedNormals) {
				SEP_AXIS.set(seperateAxis);
				float maxDot = -10000.0F;
				
				if (Vec3f.dot(SEP_AXIS, TO_OPPONENT) < 0.0D) {
					SEP_AXIS.scale(-1.0F);
				}
				
				for (Vec3 vertexVector : this.rotatedVertices) {
					TO_VERTEX.set(vertexVector);
					
					if (Vec3f.dot(SEP_AXIS, TO_VERTEX) < 0.0D) {
						TO_VERTEX.scale(-1.0F);
					}
					
					float dot = Vec3f.dot(SEP_AXIS, TO_VERTEX);
					
					if (dot > maxDot) {
						maxDot = dot;
						MAX_PROJ.set(TO_VERTEX);
					}
				}
				
				MathUtils.projectVector(TO_OPPONENT, SEP_AXIS, TO_OPPONENT_PROJECTION);
				MathUtils.projectVector(MAX_PROJ, SEP_AXIS, VERTEX_PROJECTION);
				
				if (TO_OPPONENT_PROJECTION.length() > VERTEX_PROJECTION.length() + selfCollision) {
					return;
				} else {
					switch (order) {
					case 0 -> {
						PROJECTION1.set(TO_OPPONENT_PROJECTION);
						Vec3f.scale(VERTEX_PROJECTION, TO_PLANE1, (VERTEX_PROJECTION.length() + selfCollision) / VERTEX_PROJECTION.length());
					}
					case 1 -> {
						PROJECTION2.set(TO_OPPONENT_PROJECTION);
						Vec3f.scale(VERTEX_PROJECTION, TO_PLANE2, (VERTEX_PROJECTION.length() + selfCollision) / VERTEX_PROJECTION.length());
					}
					case 2 -> {
						PROJECTION3.set(TO_OPPONENT_PROJECTION);
						Vec3f.scale(VERTEX_PROJECTION, TO_PLANE3, (VERTEX_PROJECTION.length() + selfCollision) / VERTEX_PROJECTION.length());
					}
					}
				}
				
				order++;
			}
			
			this.destinations[0].set(0.0F, 0.0F, 0.0F).add(PROJECTION1).add(PROJECTION2).add(TO_PLANE3).add(this.worldCenter);
			this.destinations[1].set(0.0F, 0.0F, 0.0F).add(PROJECTION2).add(PROJECTION3).add(TO_PLANE1).add(this.worldCenter);
			this.destinations[2].set(0.0F, 0.0F, 0.0F).add(PROJECTION3).add(PROJECTION1).add(TO_PLANE2).add(this.worldCenter);
			this.destinations[3].set(0.0F, 0.0F, 0.0F).add(PROJECTION1).add(PROJECTION2).sub(TO_PLANE3).add(this.worldCenter);
			this.destinations[4].set(0.0F, 0.0F, 0.0F).add(PROJECTION2).add(PROJECTION3).sub(TO_PLANE1).add(this.worldCenter);
			this.destinations[5].set(0.0F, 0.0F, 0.0F).add(PROJECTION3).add(PROJECTION1).sub(TO_PLANE2).add(this.worldCenter);
			
			Loop1:
			for (Vec3f dest : this.destinations) {
				for (ClothOBBCollider other : others) {
					if (other == this) {
						continue;
					}
					
					if (other.doesPointCollide(dest.toDoubleVector(), selfCollision * 0.5F)) {
						dest.invalidate();
						continue Loop1;
					}
				}
			}
			
			for (Vec3f dest : this.destinations) {
				if (dest.validateValues()) {
					destnations.add(dest);
				}
			}
		}
		
		/**
		 * Util functions
		 */
		/**
		private static final Vec3f EDGE = new Vec3f();
		private static final Vec3f TO_POINT = new Vec3f();
		private static final Vec3f PREV_CROSS = new Vec3f();
		private static final Vec3f CROSS = new Vec3f();
		
		private static boolean isPointInRectangle(Vec3f point, Vec3f... points) {
			PREV_CROSS.invalidate();
			
			for (int i = 0; i < 4; i++) {
				Vec3f.sub(points[(i + 1) % 4], points[i], EDGE);
				Vec3f.sub(point, points[(i + 1) % 4], TO_POINT);
				Vec3f.cross(EDGE, TO_POINT, CROSS);
				
				if (!PREV_CROSS.validateValues()) {
					PREV_CROSS.set(CROSS);
					continue;
				} else {
					if (Vec3f.dot(PREV_CROSS, CROSS) <= 0.0F) {
						return false;
					}
					PREV_CROSS.set(CROSS);
				}
			}
			
			return true;
		}
		
		private static final int[][] RECTANGLES = { {0, 1, 7, 6}, {2, 3, 5, 4}, {0, 3, 2, 1}, {4, 5, 6, 7}, {1, 2, 4, 7}, {0, 6, 5, 3} };
		
		public boolean intersectLine(Vec3f p1, Vec3f p2) {
			Vec3f.sub(p1, p2, UNIT);
			float restLength = UNIT.length();
			
			if (restLength == 0.0F) {
				return false;
			}
			
			for (int i = 0; i < RECTANGLES.length; i++) {
				this.getPos(RECTANGLES[i][0], V1);
				this.getPos(RECTANGLES[i][1], V2);
				this.getPos(RECTANGLES[i][2], V3);
				this.getPos(RECTANGLES[i][3], V4);
				
				NORMAL.set(this.rotatedNormals[i / 2]);
				
				if (i % 2 == 1) {
					NORMAL.scale(-1.0F);
				}
				
				if (intersectLinePlane(p1, UNIT, restLength, NORMAL)) {
					return true;
				}
			}
			
			return false;
		}
		
		private static final Vec3f LINE_START_TO_PLANE_POINT = new Vec3f();
		
		private static boolean intersectLinePlane(Vec3f lineStart, Vec3f unitVector, float lineLength, Vec3f planeNormal) {
			float dot1 = Vec3f.dot(unitVector, planeNormal);
			
			// Inner -> outer intersection (dot1 > 0.0F)
			// Objects are parallel (dot1 == 0.0F)
			if (dot1 >= 0.0F) {
				return false;
			}
			
			Vec3f.sub(V1, lineStart, LINE_START_TO_PLANE_POINT);
			float dot3 = Vec3f.dot(LINE_START_TO_PLANE_POINT, planeNormal);
			
			// Objects are parallel
			if (dot3 == 0.0F) {
				return false;
			}
			
			float dot2 = dot3 / dot1;
			
			// intersecting point is outside of line
			if (dot2 < 0.0F || Math.abs(dot2) > lineLength) {
				return false;
			}
			
			unitVector.scale(dot2);
			Vec3f.add(lineStart, unitVector, INTERSECT);
			unitVector.scale(1.0F / dot2);
			
			if (!isPointInRectangle(INTERSECT, V1, V2, V3, V4)) {
				return false;
			}
			
			return true;
		}
		
		private static void getLinePlaneIntersectPoint(Vec3f lineStart, Vec3f unitVector, float lineLength, Vec3f planeNormal, Vec3f result) {
			float dot1 = Vec3f.dot(unitVector, planeNormal);
			
			// Inner -> outer intersection (dot1 > 0.0F)
			// Objects are parallel (dot1 == 0.0F)
			if (dot1 >= 0.0F) {
				result.invalidate();
				return;
			}
			
			Vec3f.sub(V1, lineStart, LINE_START_TO_PLANE_POINT);
			float dot3 = Vec3f.dot(LINE_START_TO_PLANE_POINT, planeNormal);
			
			// Objects are parallel
			if (dot3 == 0.0F) {
				result.invalidate();
				return;
			}
			
			float dot2 = dot3 / dot1;
			
			// intersecting point is outside of line
			if (dot2 < 0.0F || Math.abs(dot2) > lineLength) {
				result.invalidate();
				return;
			}
			
			unitVector.scale(dot2);
			Vec3f.add(lineStart, unitVector, result);
			unitVector.scale(1.0F / dot2);
			
			if (!isPointInRectangle(result, V1, V2, V3, V4)) {
				result.invalidate();
			}
		}
		
		private void getPos(int idx, Vec3f v) {
			if (idx >= this.rotatedVertices.length) {
				idx -= this.rotatedVertices.length;
				v.x = (float)(this.worldCenter.x - this.rotatedVertices[idx].x);
				v.y = (float)(this.worldCenter.y - this.rotatedVertices[idx].y);
				v.z = (float)(this.worldCenter.z - this.rotatedVertices[idx].z);
			} else {
				v.x = (float)(this.worldCenter.x + this.rotatedVertices[idx].x);
				v.y = (float)(this.worldCenter.y + this.rotatedVertices[idx].y);
				v.z = (float)(this.worldCenter.z + this.rotatedVertices[idx].z);
			}
		}
		
		private static final Vec3f UNIT_VEC = new Vec3f();
		private static final Vec3f INTERSECTING = new Vec3f();
		private static final Vec3f CIRCLE_CENTER_TO_INTERSECTING = new Vec3f();
		
		private static void circlepointTouchingEdge(Vec3f circleCenter, Vec3f circleNormal, float circleRadius, Vec3f edgeStart, Vec3f edgeEnd, Vec3f intersect, Vec3 worldCenter) {
			Vec3f.sub(edgeEnd, edgeStart, UNIT_VEC);
			float edgeLength = UNIT_VEC.length();
			UNIT_VEC.scale(1.0F / edgeLength);
			
			float dot1 = Vec3f.dot(UNIT_VEC, circleNormal);
			
			// Objects are parallel (dot1 == 0.0F)
			if (dot1 == 0.0F) {
				intersect.invalidate();
				return;
			}
			
			Vec3f.sub(circleCenter, edgeStart, LINE_START_TO_PLANE_POINT);
			float dot3 = Vec3f.dot(LINE_START_TO_PLANE_POINT, circleNormal);
			
			// Objects are parallel
			if (dot3 == 0.0F) {
				intersect.invalidate();
				return;
			}
			
			float dot2 = dot3 / dot1;
			
			// intersecting point is outside of line
			if (dot2 < 0.0F || Math.abs(dot2) > edgeLength) {
				intersect.invalidate();
				return;
			}
			
			UNIT_VEC.scale(dot2);
			Vec3f.add(edgeStart, UNIT_VEC, INTERSECTING);
			Vec3f.sub(INTERSECTING, circleCenter, CIRCLE_CENTER_TO_INTERSECTING);
			float len = CIRCLE_CENTER_TO_INTERSECTING.length();
			
			// Intersecting point is outside of circle
			if (len > circleRadius) {
				intersect.invalidate();
			} else {
				// Expand toward intersecting point edge as the circle radius
				CIRCLE_CENTER_TO_INTERSECTING.scale(circleRadius / len);
				Vec3f.add(circleCenter, CIRCLE_CENTER_TO_INTERSECTING, intersect);
			}
		}
		
		private static final Vec3f PLANE_TO_CIRCLE_CENTER = new Vec3f();
		private static final Vec3f NORM_SCALED = new Vec3f();
		private static final Vec3f PROJ_CENTER = new Vec3f();
		private static final Vec3f PERPENDICULAR_VECTOR = new Vec3f();
		private static final Vec3f INTERSECT_POINT = new Vec3f();
		
		private void intersectCirclePlane(Vec3f circleCenter, Vec3f circleNormal, float circleRadius, Vec3f planeNormal, Vec3f intersect1, Vec3f intersect2) {
		    // Compute signed distance from circle center to plane
			float d = Vec3f.dot(Vec3f.sub(circleCenter, V1, PLANE_TO_CIRCLE_CENTER), planeNormal);
			
			if (Math.abs(d) > circleRadius) { // No intersection
				intersect1.invalidate();
				intersect2.invalidate();
			} else if (Math.abs(d) == circleRadius) { // One intersection point
				Vec3f.sub(circleCenter, Vec3f.scale(planeNormal, NORM_SCALED, d), intersect1);
				
				if (!isPointInRectangle(intersect1, V1, V2, V3, V4)) {
					intersect1.invalidate();
				}
				
				intersect2.invalidate();
			} else { // Two intersection points
				Vec3f.sub(circleCenter, Vec3f.scale(planeNormal, NORM_SCALED, d), PROJ_CENTER);
				float chordRadius = (float)Math.sqrt(circleRadius * circleRadius - d * d);
				Vec3f.cross(circleNormal, planeNormal, PERPENDICULAR_VECTOR);
				
				float normLength = PERPENDICULAR_VECTOR.length();
				
				if (normLength == 0.0F) {
					return;
				}
				
				PERPENDICULAR_VECTOR.scale(chordRadius / normLength);
				
				// Compute a vector toward intersecting points
				
				Vec3f.add(circleCenter, PERPENDICULAR_VECTOR, intersect1);
				Vec3f.add(circleCenter, PERPENDICULAR_VECTOR.scale(-1.0F), intersect2);
				
				if (!isPointInRectangle(Vec3f.add(circleCenter, intersect1, INTERSECT_POINT), V1, V2, V3, V4)) {
					intersect1.invalidate();
				}
				
				if (!isPointInRectangle(Vec3f.add(circleCenter, intersect2, INTERSECT_POINT), V1, V2, V3, V4)) {
					intersect2.invalidate();
				}
				
				if (intersect1.validateValues() && intersect2.validateValues()) {
					double dist1 = this.worldCenter.distanceToSqr(intersect1.x, intersect1.y, intersect1.z);
					double dist2 = this.worldCenter.distanceToSqr(intersect2.x, intersect2.y, intersect2.z);
					
					if (dist1 > dist2) {
						intersect2.invalidate();
					} else {
						intersect1.invalidate();
					}
				}
			}
		}
		
		private static void intersectLinePlane(Vec3f lineStart, Vec3f unitVector, float lineLength, Vec3f planeNormal, Vec3f intersect) {
			float dot1 = Vec3f.dot(unitVector, planeNormal);
			
			// Inner -> outer intersection (dot1 > 0.0F)
			// Objects are parallel (dot1 == 0.0F)
			if (dot1 >= 0.0F) {
				intersect.invalidate();
				return;
			}
			
			Vec3f.sub(V1, lineStart, LINE_START_TO_PLANE_POINT);
			float dot3 = Vec3f.dot(LINE_START_TO_PLANE_POINT, planeNormal);
			
			// Objects are parallel
			if (dot3 == 0.0F) {
				intersect.invalidate();
				return;
			}
			
			float dot2 = dot3 / dot1;
			
			// intersecting point is outside of line
			if (dot2 < 0.0F || Math.abs(dot2) > lineLength) {
				intersect.invalidate();
				return;
			}
			
			unitVector.scale(dot2);
			Vec3f.add(lineStart, unitVector, intersect);
			unitVector.scale(1.0F / dot2);
			
			if (!isPointInRectangle(intersect, V1, V2, V3, V4)) {
				intersect.invalidate();
			}
		}
		
		private static final Quaternionf ROTATOR = new Quaternionf();
		private static final Vec3f PITCH_AXIS = new Vec3f();
		private static final Vec3f YAW_AXIS = new Vec3f();
		private static final Vec3f NORMALIZED_TO_INTERSECT = new Vec3f();
		
		private static final int[][] EDGES = { {0, 1}, {1, 2}, {2, 3}, {3, 0}, {0, 6}, {1, 7}, {2, 4}, {3, 5}, {4, 5}, {5, 6}, {6, 7}, {7, 4} };
		private static final List<Set<Integer>> EDGE_ADJACENT_PLANES = List.of(
			  Set.of(0, 2)
			, Set.of(2, 4)
			, Set.of(1, 2)
			, Set.of(2, 5)
			, Set.of(0, 5)
			, Set.of(0, 4)
			, Set.of(1, 4)
			, Set.of(1, 5)
			, Set.of(1, 3)
			, Set.of(3, 5)
			, Set.of(0, 3)
			, Set.of(3, 4)
		);
		
		private static final Vec3f V1 = new Vec3f();
		private static final Vec3f V2 = new Vec3f();
		private static final Vec3f V3 = new Vec3f();
		private static final Vec3f V4 = new Vec3f();
		private static final Vec3f NORMAL = new Vec3f();
		private static final Vec3f UNIT = new Vec3f();
		private static final Vec3f INTERSECT = new Vec3f();
		
		private final Vec3f[] intersects = {
			// Circle plane intersections
			  new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f(), new Vec3f(), new Vec3f()
			// Circle edge intersections
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
			, new Vec3f(), new Vec3f()
		};
		
		public void pushIfEdgeCollidesCircular(ClothSimulator.ClothObject.ClothPart.ShapingConstraint constraint, float selfCollision, List<Vec3f> destinations, List<ClothSimulator.ClothOBBCollider> others) {
			Vec3f.sub(constraint.p2.position, constraint.p1.position, UNIT);
			float restLength = constraint.restLength;//UNIT.length();
			
			if (restLength == 0.0F) {
				return;
			}
			
			// Get rotator of p1 -> p2
			Vec3f.getRotatorBetween(Vec3f.Z_AXIS, UNIT, ROTATOR);
			
			// Setup two normal vectors of circles
			PITCH_AXIS.set(1.0F, 0.0F, 0.0F);
			YAW_AXIS.set(0.0F, 1.0F, 0.0F);
			Vec3f.rotate(ROTATOR, PITCH_AXIS, PITCH_AXIS);
			Vec3f.rotate(ROTATOR, YAW_AXIS, YAW_AXIS);
			
			UNIT.normalize();
			//UNIT.scale(1.0F / restLength);
			
			int intersectRects = 0;
			
			for (int i = 0; i < RECTANGLES.length; i++) {
				this.getPos(RECTANGLES[i][0], V1);
				this.getPos(RECTANGLES[i][1], V2);
				this.getPos(RECTANGLES[i][2], V3);
				this.getPos(RECTANGLES[i][3], V4);
				
				NORMAL.set(this.rotatedNormals[i / 2]);
				
				if (i % 2 == 1) {
					NORMAL.scale(-1.0F);
				}
				
				intersectLinePlane(constraint.p1.position, UNIT, restLength, NORMAL, INTERSECT);
				
				if (INTERSECT.validateValues()) {
					// Check plane - circle intersections
					this.intersectCirclePlane(constraint.p1.position, PITCH_AXIS, restLength, NORMAL, this.intersects[i * 4], this.intersects[i * 4 + 1]);
					this.intersectCirclePlane(constraint.p1.position, YAW_AXIS, restLength, NORMAL, this.intersects[i * 4 + 2], this.intersects[i * 4 + 3]);
					intersectRects++;
				} else {
					this.intersects[i * 4].invalidate();
					this.intersects[i * 4 + 1].invalidate();
					this.intersects[i * 4 + 2].invalidate();
					this.intersects[i * 4 + 3].invalidate();
				}
			}
			
			// If no planes intersect with edge, do nothing.
			if (intersectRects == 0) {
				return;
			}
			
			for (int i = 0; i < EDGES.length; i++) {
				this.getPos(EDGES[i][0], V1);
				this.getPos(EDGES[i][1], V2);
				
				// Check edge - circle intersections
				circlepointTouchingEdge(constraint.p1.position, PITCH_AXIS, restLength, V1, V2, this.intersects[24 + i * 2], this.worldCenter);
				circlepointTouchingEdge(constraint.p1.position, YAW_AXIS, restLength, V1, V2, this.intersects[24 + i * 2 + 1], this.worldCenter);
			}
			
			// Exclude points that penetrates obb rectangle if root particle is outside of obb
			if (this.doesPointCollide(constraint.p1.position.toDoubleVector(), 0.0F)) {
				for (int i = 0; i < this.intersects.length; i++) {
					if (!this.intersects[i].validateValues()) {
						continue;
					} else {
						for (int j = 0; j < RECTANGLES.length; j++) {
							if (i > 23) {
								// Skip the planes that adjacent to collider edge
								if (EDGE_ADJACENT_PLANES.get((i - 24) / 2).contains(j)) {
									continue;
								}
							} else {
								// Skip the planes if this intersecting point created by itself
								if (i / 4 == j) {
									continue;
								}
							}
							
							this.getPos(RECTANGLES[j][0], V1);
							this.getPos(RECTANGLES[j][1], V2);
							this.getPos(RECTANGLES[j][2], V3);
							this.getPos(RECTANGLES[j][3], V4);
							
							NORMAL.set(this.rotatedNormals[j / 2]);
							
							if (j % 2 == 1) {
								NORMAL.scale(-1.0F);
							}
							
							intersectLinePlane(constraint.p1.position, Vec3f.normalize(this.intersects[i], NORMALIZED_TO_INTERSECT), restLength, NORMAL, INTERSECT);
							
							if (INTERSECT.validateValues()) {
								this.intersects[i].invalidate();
								break;
							}
						}
					}
				}
			}
			
			// Exclude points that intersects with other OBBs
			Loop1:
			for (Vec3f dest : this.destinations) {
				for (ClothOBBCollider other : others) {
					if (other == this) {
						continue;
					}
					
					if (other.doesPointCollide(dest.toDoubleVector(), selfCollision)) {
						dest.invalidate();
						continue Loop1;
					}
				}
			}
			
			for (Vec3f dest : this.intersects) {
				if (dest.validateValues()) {
					destinations.add(dest);
				}
			}
		}**/
	}
}
