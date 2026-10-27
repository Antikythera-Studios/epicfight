package yesman.epicfight.api.client.model.transformer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;

import it.unimi.dsi.fastutil.ints.IntList;
import mod.azure.azurelib.animatable.client.RenderProvider;
import mod.azure.azurelib.cache.object.GeoBone;
import mod.azure.azurelib.cache.object.GeoCube;
import mod.azure.azurelib.cache.object.GeoQuad;
import mod.azure.azurelib.cache.object.GeoVertex;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.core.state.BoneSnapshot;
import mod.azure.azurelib.renderer.GeoArmorRenderer;
import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.forgeevent.AnimatedArmorTextureEvent;
import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.SingleGroupVertexBuilder;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.transformer.GeoModelTransformer.GeoMeshPartDefinition;
import yesman.epicfight.api.utils.math.OpenMatrix4f;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.api.utils.math.Vec3f;

public class AzureModelTransformer extends HumanoidModelTransformer {
	static final PartTransformer<GeoCube> HEAD = new SimpleTransformer(9);
	static final PartTransformer<GeoCube> LEFT_FEET = new SimpleTransformer(5);
	static final PartTransformer<GeoCube> RIGHT_FEET = new SimpleTransformer(2);
	static final PartTransformer<GeoCube> LEFT_ARM = new LimbPartTransformer(16, 17, 19, 1.125F, false, AABB.ofSize(new Vec3(-0.375D, 1.125D, 0), 0.5D, 0.85D, 0.5D));
	static final PartTransformer<GeoCube> RIGHT_ARM = new LimbPartTransformer(11, 12, 14, 1.125F, false, AABB.ofSize(new Vec3(0.375D, 1.125D, 0), 0.5D, 0.85D, 0.5D));
	static final PartTransformer<GeoCube> LEFT_LEG = new LimbPartTransformer(4, 5, 6, 0.375F, true, AABB.ofSize(new Vec3(-0.15D, 0.375D, 0), 0.5D, 0.85D, 0.5D));
	static final PartTransformer<GeoCube> RIGHT_LEG = new LimbPartTransformer(1, 2, 3, 0.375F, true, AABB.ofSize(new Vec3(0.15D, 0.375D, 0), 0.5D, 0.85D, 0.5D));
	static final PartTransformer<GeoCube> CHEST = new ChestPartTransformer(8, 7, 1.125F, AABB.ofSize(new Vec3(0, 1.125D, 0), 0.9D, 0.85D, 0.45D));
	
	static class GeoModelPartition {
		final PartTransformer<GeoCube> partTransformer;
		final GeoBone geoBone;
		
		private GeoModelPartition(PartTransformer<GeoCube> partTransformer, GeoBone geoBone) {
			this.partTransformer = partTransformer;
			this.geoBone = geoBone;
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void getGeoArmorTexturePath(AnimatedArmorTextureEvent event) {
		RenderProvider customRenderProperties = RenderProvider.of(event.getItemstack());
		
		if (customRenderProperties != null) {
			HumanoidModel<?> extensionRenderer = customRenderProperties.getHumanoidArmorModel(event.getLivingEntity(), event.getItemstack(), event.getEquipmentSlot(), (HumanoidModel<LivingEntity>) event.getOriginalModel());
			
			if (extensionRenderer instanceof GeoArmorRenderer geoArmorRenderer && event.getItemstack().getItem() instanceof GeoAnimatable geoAnimatable) {
				event.setResultLocation(geoArmorRenderer.getTextureLocation(geoAnimatable));
			}
		}
	}	
	
	@Override
	public SkinnedMesh transformArmorModel(HumanoidModel<?> humanoidModel) {
		if (!(humanoidModel instanceof GeoArmorRenderer<?> geoModel)) {
			return null;
		}
		
		List<GeoModelPartition> boxes = Lists.newArrayList();
		
		GeoBone headBone = geoModel.getHeadBone();
		GeoBone bodyBone = geoModel.getBodyBone();
		GeoBone rightArmBone = geoModel.getRightArmBone();
		GeoBone leftArmBone = geoModel.getLeftArmBone();
		GeoBone rightLegBone = geoModel.getRightLegBone();
		GeoBone leftLegBone = geoModel.getLeftLegBone();
		GeoBone rightBootBone = geoModel.getRightBootBone();
		GeoBone leftBootBone = geoModel.getLeftBootBone();
		
		if (headBone != null) {
			headBone.setRotX(0);
			headBone.setRotY(0);
			headBone.setRotZ(0);
		}
		
		if (bodyBone != null) {
			bodyBone.setRotX(0);
			bodyBone.setRotY(0);
			bodyBone.setRotZ(0);
		}
		
		if (rightArmBone != null) {
			rightArmBone.setRotX(0);
			rightArmBone.setRotY(0);
			rightArmBone.setRotZ(0);
		}
		
		if (leftArmBone != null) {
			leftArmBone.setRotX(0);
			leftArmBone.setRotY(0);
			leftArmBone.setRotZ(0);
		}
		
		if (rightLegBone != null) {
			rightLegBone.setRotX(0);
			rightLegBone.setRotY(0);
			rightLegBone.setRotZ(0);
		}
		
		if (leftLegBone != null) {
			leftLegBone.setRotX(0);
			leftLegBone.setRotY(0);
			leftLegBone.setRotZ(0);
		}
		
		if (rightBootBone != null) {
			rightBootBone.setRotX(0);
			rightBootBone.setRotY(0);
			rightBootBone.setRotZ(0);
		}
		
		if (leftBootBone != null) {
			leftBootBone.setRotX(0);
			leftBootBone.setRotY(0);
			leftBootBone.setRotZ(0);
		}
		
		boxes.add(new GeoModelPartition(HEAD, headBone));
		boxes.add(new GeoModelPartition(CHEST, bodyBone));
		boxes.add(new GeoModelPartition(RIGHT_ARM, rightArmBone));
		boxes.add(new GeoModelPartition(LEFT_ARM, leftArmBone));
		boxes.add(new GeoModelPartition(LEFT_LEG, leftLegBone));
		boxes.add(new GeoModelPartition(RIGHT_LEG, rightLegBone));
		boxes.add(new GeoModelPartition(LEFT_FEET, leftBootBone));
		boxes.add(new GeoModelPartition(RIGHT_FEET, rightBootBone));
		
		return bakeMeshFromCubes(boxes);
	}
	
	private static SkinnedMesh bakeMeshFromCubes(List<GeoModelPartition> partitions) {
		List<SingleGroupVertexBuilder> vertices = Lists.newArrayList();
		Map<MeshPartDefinition, IntList> indices = Maps.newHashMap();
		PoseStack poseStack = new PoseStack();
		PartTransformer.IndexCounter indexCounter = new PartTransformer.IndexCounter();
		
		for (GeoModelPartition modelpartition : partitions) {
			bake(poseStack, modelpartition, modelpartition.geoBone.getName(), modelpartition.geoBone, vertices, indices, Lists.newArrayList(), indexCounter, false);
		}
		
		return SingleGroupVertexBuilder.loadVertexInformation(vertices, indices);
	}
	
	private static void bake(PoseStack poseStack, GeoModelPartition modelpartition, String partName, GeoBone geoBone, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, List<String> path, PartTransformer.IndexCounter indexCounter, boolean bindPartAnimation) {
		if (geoBone == null) {
			return;
		}
		
		poseStack.pushPose();
		
		RenderUtils.prepMatrixForBone(poseStack, geoBone);
		
		List<String> newList = new ArrayList<>(path);
		
		if (bindPartAnimation) {
			newList.add(partName);
		}
		
		if (!geoBone.isHidden()) {
			for (GeoCube cube : geoBone.getCubes()) {
				poseStack.pushPose();
				
				RenderUtils.translateToPivotPoint(poseStack, cube);
				RenderUtils.rotateMatrixAroundCube(poseStack, cube);
				RenderUtils.translateAwayFromPivotPoint(poseStack, cube);
				MeshPartDefinition partDefinition = GeoMeshPartDefinition.of(partName);
				
				if (bindPartAnimation) {
					OpenMatrix4f invertedParentTransform = OpenMatrix4f.importFromMojangMatrix(poseStack.last().pose());
					invertedParentTransform.m30 *= 0.0625F;
					invertedParentTransform.m31 *= 0.0625F;
					invertedParentTransform.m32 *= 0.0625F;
					invertedParentTransform.invert();
					partDefinition = AzureMeshPartDefinition.of(partName, newList, invertedParentTransform, modelpartition.geoBone);
				}
				
				modelpartition.partTransformer.bakeCube(poseStack, partDefinition, cube, vertices, indices, indexCounter);
				poseStack.popPose();
			}
		}
		
		if (!geoBone.isHidingChildren()) {
			for (GeoBone childBone : geoBone.getChildBones()) {
				bake(poseStack, modelpartition, partName, childBone, vertices, indices, newList, indexCounter, true);
			}
		}
		
		poseStack.popPose();
	}
	
	static class SimpleTransformer extends PartTransformer<GeoCube> {
		final int jointId;
		
		public SimpleTransformer(int jointId) {
			this.jointId = jointId;
		}
		
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, GeoCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			for (GeoQuad quad : cube.quads()) {
				if (quad == null) {
					continue;
				}
				
				Vector3f norm = new Vector3f(quad.normal());
				norm.mul(poseStack.last().normal());
				
				for (GeoVertex vertex : quad.vertices()) {
					Vector4f pos = new Vector4f(vertex.position(), 1.0F);
					pos.mul(poseStack.last().pose());
					
					vertices.add(new SingleGroupVertexBuilder()
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z())/*.scale(0.0625F)*/)
						.setNormal(new Vec3f(norm.x(), norm.y(), norm.z()))
						.setTextureCoordinate(new Vec2f(vertex.texU(), vertex.texV()))
						.setEffectiveJointIDs(new Vec3f(this.jointId, 0, 0))
						.setEffectiveJointWeights(new Vec3f(1.0F, 0.0F, 0.0F))
						.setEffectiveJointNumber(1)
					);
				}
				
				triangluatePolygon(indices, partName, indexCounter);
			}
		}
	}
	
	static class ChestPartTransformer extends PartTransformer<GeoCube> {
		static final float X_PLANE = 0.0F;
		static final VertexWeight[] WEIGHT_ALONG_Y = { new VertexWeight(13.6666F, 0.230F, 0.770F), new VertexWeight(15.8333F, 0.254F, 0.746F), new VertexWeight(18.0F, 0.5F, 0.5F), new VertexWeight(20.1666F, 0.744F, 0.256F), new VertexWeight(22.3333F, 0.770F, 0.230F)};
		
		final SimpleTransformer upperAttachmentTransformer;
		final SimpleTransformer lowerAttachmentTransformer;
		final AABB noneAttachmentArea;
		final float yClipCoord;
		
		public ChestPartTransformer(int upperJoint, int lowerJoint, float yBasis, AABB noneAttachmentArea) {
			this.noneAttachmentArea = noneAttachmentArea;
			this.upperAttachmentTransformer = new SimpleTransformer(upperJoint);
			this.lowerAttachmentTransformer = new SimpleTransformer(lowerJoint);
			this.yClipCoord = yBasis;
		}
		
		@Override
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, GeoCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			Vec3 centerOfCube = getCenterOfCube(poseStack, cube);
			
			if (!this.noneAttachmentArea.contains(centerOfCube)) {
				if (centerOfCube.y < this.yClipCoord) {
					this.lowerAttachmentTransformer.bakeCube(poseStack, partName, cube, vertices, indices, indexCounter);
				} else {
					this.upperAttachmentTransformer.bakeCube(poseStack, partName, cube, vertices, indices, indexCounter);
				}
				
				return;
			}
			
			List<AnimatedPolygon> xClipPolygons = Lists.newArrayList();
			List<AnimatedPolygon> xyClipPolygons = Lists.newArrayList();
			
			for (GeoQuad polygon : cube.quads()) {
				Matrix4f matrix = poseStack.last().pose();
				
				ModelPart.Vertex pos0 = getTranslatedVertex(polygon.vertices()[0], matrix);
				ModelPart.Vertex pos1 = getTranslatedVertex(polygon.vertices()[1], matrix);
				ModelPart.Vertex pos2 = getTranslatedVertex(polygon.vertices()[2], matrix);
				ModelPart.Vertex pos3 = getTranslatedVertex(polygon.vertices()[3], matrix);
				Direction direction = getDirectionFromVector(polygon.normal());
				VertexWeight pos0Weight = getYClipWeight(pos0.pos.y());
				VertexWeight pos1Weight = getYClipWeight(pos1.pos.y());
				VertexWeight pos2Weight = getYClipWeight(pos2.pos.y());
				VertexWeight pos3Weight = getYClipWeight(pos3.pos.y());
				
				if (pos1.pos.x() > X_PLANE != pos2.pos.x() > X_PLANE) {
					float distance = pos2.pos.x() - pos1.pos.x();
					float textureU = pos1.u + (pos2.u - pos1.u) * ((X_PLANE - pos1.pos.x()) / distance);
					ModelPart.Vertex pos4 = new ModelPart.Vertex(X_PLANE, pos0.pos.y(), pos0.pos.z(), textureU, pos0.v);
					ModelPart.Vertex pos5 = new ModelPart.Vertex(X_PLANE, pos1.pos.y(), pos1.pos.z(), textureU, pos1.v);
					
					xClipPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, 8, 7, 0, pos0Weight.chestWeight, pos0Weight.torsoWeight, 0),
						new AnimatedVertex(pos4, 8, 7, 0, pos0Weight.chestWeight, pos0Weight.torsoWeight, 0),
						new AnimatedVertex(pos5, 8, 7, 0, pos1Weight.chestWeight, pos1Weight.torsoWeight, 0),
						new AnimatedVertex(pos3, 8, 7, 0, pos3Weight.chestWeight, pos3Weight.torsoWeight, 0)
					}, direction));
					xClipPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos4, 8, 7, 0, pos0Weight.chestWeight, pos0Weight.torsoWeight, 0),
						new AnimatedVertex(pos1, 8, 7, 0, pos1Weight.chestWeight, pos1Weight.torsoWeight, 0),
						new AnimatedVertex(pos2, 8, 7, 0, pos2Weight.chestWeight, pos2Weight.torsoWeight, 0),
						new AnimatedVertex(pos5, 8, 7, 0, pos1Weight.chestWeight, pos1Weight.torsoWeight, 0)
					}, direction));
				} else {
					xClipPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, 8, 7, 0, pos0Weight.chestWeight, pos0Weight.torsoWeight, 0),
						new AnimatedVertex(pos1, 8, 7, 0, pos1Weight.chestWeight, pos1Weight.torsoWeight, 0),
						new AnimatedVertex(pos2, 8, 7, 0, pos2Weight.chestWeight, pos2Weight.torsoWeight, 0),
						new AnimatedVertex(pos3, 8, 7, 0, pos3Weight.chestWeight, pos3Weight.torsoWeight, 0)
					}, direction));
				}
			}
			
			for (AnimatedPolygon polygon : xClipPolygons) {
				boolean upsideDown = polygon.animatedVertexPositions[1].pos.y() > polygon.animatedVertexPositions[2].pos.y();
				AnimatedVertex pos0 = upsideDown ? polygon.animatedVertexPositions[2] : polygon.animatedVertexPositions[0];
				AnimatedVertex pos1 = upsideDown ? polygon.animatedVertexPositions[3] : polygon.animatedVertexPositions[1];
				AnimatedVertex pos2 = upsideDown ? polygon.animatedVertexPositions[0] : polygon.animatedVertexPositions[2];
				AnimatedVertex pos3 = upsideDown ? polygon.animatedVertexPositions[1] : polygon.animatedVertexPositions[3];
				Direction direction = getDirectionFromVector(polygon.normal);
				List<VertexWeight> vertexWeights = getMiddleYClipWeights(pos1.pos.y(), pos2.pos.y());
				List<AnimatedVertex> animatedVertices = Lists.newArrayList();
				animatedVertices.add(pos0);
				animatedVertices.add(pos1);
				
				if (!vertexWeights.isEmpty()) {
					for (VertexWeight vertexWeight : vertexWeights) {
						float distance = pos2.pos.y() - pos1.pos.y();
						float textureV = pos1.v + (pos2.v - pos1.v) * ((vertexWeight.yClipCoord - pos1.pos.y()) / distance);
						Vector3f clipPos1 = getClipPoint(pos1.pos, pos2.pos, vertexWeight.yClipCoord);
						Vector3f clipPos2 = getClipPoint(pos0.pos, pos3.pos, vertexWeight.yClipCoord);
						ModelPart.Vertex pos4 = new ModelPart.Vertex(clipPos2, pos0.u, textureV);
						ModelPart.Vertex pos5 = new ModelPart.Vertex(clipPos1, pos1.u, textureV);
						animatedVertices.add(new AnimatedVertex(pos4, 8, 7, 0, vertexWeight.chestWeight, vertexWeight.torsoWeight, 0));
						animatedVertices.add(new AnimatedVertex(pos5, 8, 7, 0, vertexWeight.chestWeight, vertexWeight.torsoWeight, 0));
					}
				}
				
				animatedVertices.add(pos3);
				animatedVertices.add(pos2);
				
				for (int i = 0; i < (animatedVertices.size() - 2) / 2; i++) {
					int start = i*2;
					AnimatedVertex p0 = animatedVertices.get(start);
					AnimatedVertex p1 = animatedVertices.get(start + 1);
					AnimatedVertex p2 = animatedVertices.get(start + 3);
					AnimatedVertex p3 = animatedVertices.get(start + 2);
					xyClipPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(p0, 8, 7, 0, p0.weight.x, p0.weight.y, 0),
						new AnimatedVertex(p1, 8, 7, 0, p1.weight.x, p1.weight.y, 0),
						new AnimatedVertex(p2, 8, 7, 0, p2.weight.x, p2.weight.y, 0),
						new AnimatedVertex(p3, 8, 7, 0, p3.weight.x, p3.weight.y, 0)
					}, direction));
				}
			}
			
			for (AnimatedPolygon polygon : xyClipPolygons) {
				Vector3f norm = new Vector3f(polygon.normal);
				norm.mul(poseStack.last().normal());
				
				for (AnimatedVertex vertex : polygon.animatedVertexPositions) {
					Vector4f pos = new Vector4f(vertex.pos, 1.0F);
					float weight1 = vertex.weight.x;
					float weight2 = vertex.weight.y;
					int joint1 = vertex.jointId.getX();
					int joint2 = vertex.jointId.getY();
					int count = weight1 > 0.0F && weight2 > 0.0F ? 2 : 1;
					
					if (weight1 <= 0.0F) {
						joint1 = joint2;
						weight1 = weight2;
					}
					
					vertices.add(new SingleGroupVertexBuilder()
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z()))
						.setNormal(new Vec3f(norm.x(), norm.y(), norm.z()))
						.setTextureCoordinate(new Vec2f(vertex.u, vertex.v))
						.setEffectiveJointIDs(new Vec3f(joint1, joint2, 0))
						.setEffectiveJointWeights(new Vec3f(weight1, weight2, 0.0F))
						.setEffectiveJointNumber(count)
					);
				}
				
				triangluatePolygon(indices, partName, indexCounter);
			}
		}
		
		static VertexWeight getYClipWeight(float y) {
			if (y < WEIGHT_ALONG_Y[0].yClipCoord) {
				return new VertexWeight(y, 0.0F, 1.0F);
			}
			
			int index = -1;
			for (int i = 0; i < WEIGHT_ALONG_Y.length; i++) {
				
			}
			
			if (index > 0) {
				VertexWeight pair = WEIGHT_ALONG_Y[index];
				return new VertexWeight(y, pair.chestWeight, pair.torsoWeight);
			}
			
			return new VertexWeight(y, 1.0F, 0.0F);
		}
		
		static List<VertexWeight> getMiddleYClipWeights(float minY, float maxY) {
			List<VertexWeight> cutYs = Lists.newArrayList();
			for (VertexWeight vertexWeight : WEIGHT_ALONG_Y) {
				if (vertexWeight.yClipCoord > minY && maxY >= vertexWeight.yClipCoord) {
					cutYs.add(vertexWeight);
				}
			}
			return cutYs;
		}
		
		static class VertexWeight {
			final float yClipCoord;
			final float chestWeight;
			final float torsoWeight;
			
			public VertexWeight(float yClipCoord, float chestWeight, float torsoWeight) {
				this.yClipCoord = yClipCoord;
				this.chestWeight = chestWeight;
				this.torsoWeight = torsoWeight;
			}
		}
	}
	
	static class LimbPartTransformer extends PartTransformer<GeoCube> {
		final int upperJoint;
		final int lowerJoint;
		final int middleJoint;
		final boolean bendInFront;
		final SimpleTransformer upperAttachmentTransformer;
		final SimpleTransformer lowerAttachmentTransformer;
		final AABB noneAttachmentArea;
		final float yClipCoord;
		
		public LimbPartTransformer(int upperJoint, int lowerJoint, int middleJoint, float yClipCoord, boolean bendInFront, AABB noneAttachmentArea) {
			this.upperJoint = upperJoint;
			this.lowerJoint = lowerJoint;
			this.middleJoint = middleJoint;
			this.bendInFront = bendInFront;
			this.upperAttachmentTransformer = new SimpleTransformer(upperJoint);
			this.lowerAttachmentTransformer = new SimpleTransformer(lowerJoint);
			this.noneAttachmentArea = noneAttachmentArea;
			this.yClipCoord = yClipCoord;
		}
		
		@Override
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, GeoCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			Vec3 centerOfCube = getCenterOfCube(poseStack, cube);
			
			if (!this.noneAttachmentArea.contains(centerOfCube)) {
				if (centerOfCube.y < this.yClipCoord) {
					this.lowerAttachmentTransformer.bakeCube(poseStack, partName, cube, vertices, indices, indexCounter);
				} else {
					this.upperAttachmentTransformer.bakeCube(poseStack, partName, cube, vertices, indices, indexCounter);
				}
				
				return;
			}
			
			List<AnimatedPolygon> polygons = Lists.newArrayList();
			
			for (GeoQuad quad : cube.quads()) {
				Matrix4f matrix = poseStack.last().pose();
				ModelPart.Vertex pos0 = getTranslatedVertex(quad.vertices()[0], matrix);
				ModelPart.Vertex pos1 = getTranslatedVertex(quad.vertices()[1], matrix);
				ModelPart.Vertex pos2 = getTranslatedVertex(quad.vertices()[2], matrix);
				ModelPart.Vertex pos3 = getTranslatedVertex(quad.vertices()[3], matrix);
				Direction direction = getDirectionFromVector(quad.normal());
				
				if (pos1.pos.y() > this.yClipCoord != pos2.pos.y() > this.yClipCoord) {
					float distance = pos2.pos.y() - pos1.pos.y();
					float textureV = pos1.v + (pos2.v - pos1.v) * ((this.yClipCoord - pos1.pos.y()) / distance);
					Vector3f clipPos1 = getClipPoint(pos1.pos, pos2.pos, this.yClipCoord);
					Vector3f clipPos2 = getClipPoint(pos0.pos, pos3.pos, this.yClipCoord);
					ModelPart.Vertex pos4 = new ModelPart.Vertex(clipPos2, pos0.u, textureV);
					ModelPart.Vertex pos5 = new ModelPart.Vertex(clipPos1, pos1.u, textureV);
					
					int upperId, lowerId;
					if (distance > 0) {
						upperId = this.lowerJoint;
						lowerId = this.upperJoint;
					} else {
						upperId = this.upperJoint;
						lowerId = this.lowerJoint;
					}
					
					polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, upperId), new AnimatedVertex(pos1, upperId),
						new AnimatedVertex(pos5, upperId), new AnimatedVertex(pos4, upperId)
					}, direction));
					polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos4, lowerId), new AnimatedVertex(pos5, lowerId),
						new AnimatedVertex(pos2, lowerId), new AnimatedVertex(pos3, lowerId)
					}, direction));
					
					boolean hasSameZ = pos4.pos.z() < 0.0F == pos5.pos.z() < 0.0F;
					boolean isFront = hasSameZ && (pos4.pos.z() < 0.0F == this.bendInFront);
					
					if (isFront) {
						polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, this.middleJoint), new AnimatedVertex(pos5, this.middleJoint),
							new AnimatedVertex(pos5, this.upperJoint), new AnimatedVertex(pos4, this.upperJoint)
						}, 0.001F, direction));
						polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, this.lowerJoint), new AnimatedVertex(pos5, this.lowerJoint),
							new AnimatedVertex(pos5, this.middleJoint), new AnimatedVertex(pos4, this.middleJoint)
						}, 0.001F, direction));
					} else if (!hasSameZ) {
						boolean startFront = pos4.pos.z() > 0;
						int firstJoint = this.lowerJoint;
						int secondJoint = this.lowerJoint;
						int thirdJoint = startFront ? this.upperJoint : this.middleJoint;
						int fourthJoint = startFront ? this.middleJoint : this.upperJoint;
						int fifthJoint = this.upperJoint;
						int sixthJoint = this.upperJoint;
						
						polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, firstJoint), new AnimatedVertex(pos5, secondJoint),
							new AnimatedVertex(pos5, thirdJoint), new AnimatedVertex(pos4, fourthJoint)
						}, 0.001F, direction));
						polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, fourthJoint), new AnimatedVertex(pos5, thirdJoint),
							new AnimatedVertex(pos5, fifthJoint), new AnimatedVertex(pos4, sixthJoint)
						}, 0.001F, direction));
					}
				} else {
					int jointId = pos0.pos.y() > this.yClipCoord ? this.upperJoint : this.lowerJoint;
					polygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, jointId), new AnimatedVertex(pos1, jointId),
						new AnimatedVertex(pos2, jointId), new AnimatedVertex(pos3, jointId)
					}, direction));
				}
			}
			
			for (AnimatedPolygon quad : polygons) {
				Vector3f norm = new Vector3f(quad.normal);
				norm.mul(poseStack.last().normal());
				
				for (AnimatedVertex vertex : quad.animatedVertexPositions) {
					Vector4f pos = new Vector4f(vertex.pos, 1.0F);
					
					vertices.add(new SingleGroupVertexBuilder()
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z()))
						.setNormal(new Vec3f(norm.x(), norm.y(), norm.z()))
						.setTextureCoordinate(new Vec2f(vertex.u, vertex.v))
						.setEffectiveJointIDs(new Vec3f(vertex.jointId.getX(), 0, 0))
						.setEffectiveJointWeights(new Vec3f(1.0F, 0.0F, 0.0F))
						.setEffectiveJointNumber(1)
					);
				}
				
				triangluatePolygon(indices, partName, indexCounter);
			}
		}
	}
	
	static Direction getDirectionFromVector(Vector3f directionVec) {
		for (Direction direction : Direction.values()) {
			Vector3f direcVec = new Vector3f(Float.compare(directionVec.x(), -0.0F) == 0 ? 0.0F : directionVec.x(), directionVec.y(), directionVec.z());
			if (direcVec.equals(direction.step())) {
				return direction;
			}
		}
		
		return null;
	}
	
	static Vec3 getCenterOfCube(PoseStack poseStack, GeoCube cube) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;
		double maxZ = Double.MIN_VALUE;
		
		Matrix4f matrix = poseStack.last().pose();
		
		for (GeoQuad quad : cube.quads()) {
			for (GeoVertex v : quad.vertices()) {
				Vector4f translatedPosition = new Vector4f(v.position(), 1.0F);
				translatedPosition.mul(matrix);
				
				if (minX > translatedPosition.x()) {
					minX = translatedPosition.x();
				}
				
				if (minY > translatedPosition.y()) {
					minY = translatedPosition.y();
				}
				
				if (minZ > translatedPosition.z()) {
					minZ = translatedPosition.z();
				}
				
				if (maxX < translatedPosition.x()) {
					maxX = translatedPosition.x();
				}
				
				if (maxY < translatedPosition.y()) {
					maxY = translatedPosition.y();
				}
				
				if (maxZ < translatedPosition.z()) {
					maxZ = translatedPosition.z();
				}
			}
		}
		
		return new Vec3(minX + (maxX - minX) * 0.5D, minY + (maxY - minY) * 0.5D, minZ + (maxZ - minZ) * 0.5D);
	}
	
	static Vector3f getClipPoint(Vector3f pos1, Vector3f pos2, float yClip) {
		Vector3f direct = new Vector3f(pos2);
		direct.sub(pos1);
		direct.mul((yClip - pos1.y()) / (pos2.y() - pos1.y()));
		
		Vector3f clipPoint = new Vector3f(pos1);
		clipPoint.add(direct);
		
		return clipPoint;
	}
	
	static ModelPart.Vertex getTranslatedVertex(GeoVertex original, Matrix4f matrix) {
		Vector4f translatedPosition = new Vector4f(original.position(), 1.0F);
		translatedPosition.mul(matrix);
		
		return new ModelPart.Vertex(translatedPosition.x(), translatedPosition.y(), translatedPosition.z(), original.texU(), original.texV());
	}
	
	static class AnimatedVertex extends ModelPart.Vertex {
		final Vec3i jointId;
		final Vec3f weight;
		
		public AnimatedVertex(ModelPart.Vertex posTexVertx, int jointId) {
			this(posTexVertx, jointId, 0, 0, 1.0F, 0.0F, 0.0F);
		}
		
		public AnimatedVertex(ModelPart.Vertex posTexVertx, int jointId1, int jointId2, int jointId3, float weight1, float weight2, float weight3) {
			this(posTexVertx, new Vec3i(jointId1, jointId2, jointId3), new Vec3f(weight1, weight2, weight3));
		}
		
		public AnimatedVertex(ModelPart.Vertex posTexVertx, Vec3i ids, Vec3f weights) {
			this(posTexVertx, posTexVertx.u, posTexVertx.v, ids, weights);
		}
		
		public AnimatedVertex(ModelPart.Vertex posTexVertx, float u, float v, Vec3i ids, Vec3f weights) {
			super(posTexVertx.pos.x(), posTexVertx.pos.y(), posTexVertx.pos.z(), u, v);
			this.jointId = ids;
			this.weight = weights;
		}
	}
	
	static class AnimatedPolygon {
		public final AnimatedVertex[] animatedVertexPositions;
		public final Vector3f normal;
		
		public AnimatedPolygon(AnimatedVertex[] positionsIn, Direction directionIn) {
			this.animatedVertexPositions = positionsIn;
			this.normal = directionIn.step();
		}
		
		public AnimatedPolygon(AnimatedVertex[] positionsIn, float cor, Direction directionIn) {
			this.animatedVertexPositions = positionsIn;
			positionsIn[0] = new AnimatedVertex(positionsIn[0], positionsIn[0].u, positionsIn[0].v + cor, positionsIn[0].jointId, positionsIn[0].weight);
			positionsIn[1] = new AnimatedVertex(positionsIn[1], positionsIn[1].u, positionsIn[1].v + cor, positionsIn[1].jointId, positionsIn[1].weight);
			positionsIn[2] = new AnimatedVertex(positionsIn[2], positionsIn[2].u, positionsIn[2].v - cor, positionsIn[2].jointId, positionsIn[2].weight);
			positionsIn[3] = new AnimatedVertex(positionsIn[3], positionsIn[3].u, positionsIn[3].v - cor, positionsIn[3].jointId, positionsIn[3].weight);
			this.normal = directionIn.step();
		}
	}
	
	public record AzureMeshPartDefinition(String partName, List<String> path, OpenMatrix4f invertedParentTransform, GeoBone root) implements MeshPartDefinition {
		public static MeshPartDefinition of(String partName) {
			return new AzureMeshPartDefinition(partName, null, null, null);
		}
		
		public static MeshPartDefinition of(String partName, List<String> path, OpenMatrix4f invertedParentTransform, GeoBone root) {
			return new AzureMeshPartDefinition(partName, path, invertedParentTransform, root);
		}
		
		public static GeoBone getChildBone(GeoBone bone, String boneName) {
			for (GeoBone childBone : bone.getChildBones()) {
				if (childBone.getName().equals(boneName)) {
					return childBone;
				}
			}
			
			return null;
		}
		
		public Supplier<OpenMatrix4f> getModelPartAnimationProvider() {
			return this.root == null ? () -> null : () -> {
				PoseStack poseStack = new PoseStack();
				this.progress(this.root, poseStack, false);
				GeoBone bone = this.root;
				int idx = 0;
				
				for (String childPartName : this.path) {
					bone = getChildBone(bone, childPartName);
					
					if (bone == null) {
						return null;
					}
					
					idx++;
					this.progress(bone, poseStack, idx == this.path.size());
				}
				
				OpenMatrix4f parentTransform = OpenMatrix4f.importFromMojangMatrix(poseStack.last().pose());
				GeoBone lastBone = bone;
				BoneSnapshot boneSnapshot = bone.getInitialSnapshot();
				OpenMatrix4f partAnimation = OpenMatrix4f.mulMatrices(parentTransform,
																	  new OpenMatrix4f().mulBack(OpenMatrix4f.fromQuaternion(new Quaternionf().rotationZYX(boneSnapshot.getRotZ(), boneSnapshot.getRotY(), boneSnapshot.getRotX())).transpose().invert())
																	  					.translate(new Vec3f(lastBone.getPosX() - boneSnapshot.getOffsetX(), lastBone.getPosY() - boneSnapshot.getOffsetY(), lastBone.getPosZ() - boneSnapshot.getOffsetZ()).scale(0.0625F))
																						.mulBack(OpenMatrix4f.fromQuaternion(new Quaternionf().rotationZYX(boneSnapshot.getRotZ(), boneSnapshot.getRotY(), boneSnapshot.getRotX())).transpose())
																						.mulBack(OpenMatrix4f.fromQuaternion(new Quaternionf().rotationZYX(boneSnapshot.getRotZ() - lastBone.getRotZ(), boneSnapshot.getRotY() - lastBone.getRotY(), boneSnapshot.getRotX() - lastBone.getRotX())))
																						.scale(new Vec3f(lastBone.getScaleX(), lastBone.getScaleY(), lastBone.getScaleZ())),
																	  this.invertedParentTransform);
				
				return partAnimation;
			};
		}
		
		public Mesh.RenderProperties renderProperties() {
			return null;
		}
		
		private void progress(GeoBone bone, PoseStack poseStack, boolean last) {
			BoneSnapshot boneSnapshot = bone.getInitialSnapshot();
			
			if (last) {
				poseStack.translate(boneSnapshot.getOffsetX(), boneSnapshot.getOffsetY(), boneSnapshot.getOffsetZ());
				poseStack.mulPose(new Quaternionf().rotationZYX(boneSnapshot.getRotZ(), boneSnapshot.getRotY(), boneSnapshot.getRotX()));
			} else {
				poseStack.translate(bone.getPosX(), bone.getPosY(), bone.getPosZ());
				poseStack.mulPose(new Quaternionf().rotationZYX(bone.getRotZ(), bone.getRotY(), bone.getRotX()));
				poseStack.scale(bone.getScaleX(), bone.getScaleY(), bone.getScaleZ());
			}
		}
		
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			} else if (o instanceof MeshPartDefinition comparision) {
				return this.partName.equals(comparision.partName());
			}
			
			return false;
		}
		
		public int hashCode() {
			return this.partName.hashCode();
		}
	}
}
