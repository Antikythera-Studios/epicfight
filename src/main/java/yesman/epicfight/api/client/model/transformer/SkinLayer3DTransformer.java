package yesman.epicfight.api.client.model.transformer;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.vertex.PoseStack;

import dev.tr7zw.skinlayers.SkinLayersModBase;
import dev.tr7zw.skinlayers.api.LayerFeatureTransformerAPI;
import dev.tr7zw.skinlayers.api.MeshTransformer;
import dev.tr7zw.skinlayers.api.SkinLayersAPI;
import dev.tr7zw.skinlayers.versionless.render.CustomModelPart;
import dev.tr7zw.skinlayers.versionless.render.CustomizableCube;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelPart.Cube;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.entity.player.PlayerModelPart;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.SingleGroupVertexBuilder;
import yesman.epicfight.api.client.model.transformer.HumanoidModelTransformer.PartTransformer;
import yesman.epicfight.api.client.model.transformer.VanillaModelTransformer.VanillaMeshPartDefinition;
import yesman.epicfight.api.utils.math.QuaternionUtils;
import yesman.epicfight.api.utils.math.Vec2f;
import yesman.epicfight.api.utils.math.Vec3f;
import yesman.epicfight.mixin.skinlayers.MixinCustomModelPart;
import yesman.epicfight.mixin.skinlayers.MixinCustomizableCubeWrapper.SkinLayer3DMixinCustomModelCube;

public class SkinLayer3DTransformer extends CustomizableCube {
	private SkinLayer3DTransformer() {
		super(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, false, 0, 0, new dev.tr7zw.skinlayers.versionless.util.Direction[] {}, new dev.tr7zw.skinlayers.versionless.util.Direction[][] {{}});
	}
	
	static final PartTransformer<CustomizableCube> HEAD = new SimpleTransformer(9);
	static final PartTransformer<CustomizableCube> LEFT_FEET = new SimpleTransformer(5);
	static final PartTransformer<CustomizableCube> RIGHT_FEET = new SimpleTransformer(2);
	static final PartTransformer<CustomizableCube> LEFT_ARM = new LimbPartTransformer(16, 17, 19, 19.0F, false);
	static final PartTransformer<CustomizableCube> RIGHT_ARM = new LimbPartTransformer(11, 12, 14, 19.0F, false);
	static final PartTransformer<CustomizableCube> LEFT_LEG = new LimbPartTransformer(4, 5, 6, 6.0F, true);
	static final PartTransformer<CustomizableCube> RIGHT_LEG = new LimbPartTransformer(1, 2, 3, 6.0F, true);
	static final PartTransformer<CustomizableCube> CHEST = new ChestPartTransformer(18.0F);
	
	static class ModelPartition {
		final PartTransformer<ModelPart.Cube> vanillaPartTransformer;
		final PartTransformer<CustomizableCube> partTransformer;
		final String partName;
		final CustomModelPart skinlayerModelPart;
		final ModelPart vanillaModelPart;
		
		final List<Cube> vanillaCubes;
		final List<CustomizableCube> customizableCubes;
		final Consumer<PoseStack> transformFunction;
		
		private ModelPartition(PartTransformer<ModelPart.Cube> vanillaPartTransformer, PartTransformer<CustomizableCube> partTransformer, String partName, CustomModelPart skinlayerModelPart, ModelPart vanillaModelPart, List<Cube> vanillaCubes, List<CustomizableCube> customCubes, Consumer<PoseStack> transformFunction) {
			this.vanillaPartTransformer = vanillaPartTransformer;
			this.partTransformer = partTransformer;
			this.partName = partName;
			this.skinlayerModelPart = skinlayerModelPart;
			this.vanillaModelPart = vanillaModelPart;
			
			this.vanillaCubes = vanillaCubes;
			this.customizableCubes = customCubes;
			this.transformFunction = transformFunction;
		}
	}
	
	public static SkinnedMesh transformMesh(AbstractClientPlayer abstractClientPlayer, CustomModelPart skinlayerModelPart, ModelPart vanillaModelPart, PlayerModelPart modelPart, List<Cube> vanillaCubes, List<CustomizableCube> cubes) {
		List<ModelPartition> partitions = Lists.newArrayList();
		
		float widthScale = SkinLayersModBase.config.baseVoxelSize;
		float heightScale = 1.035F;
		
		switch (modelPart) {
		case JACKET:
			partitions.add(new ModelPartition(VanillaModelTransformer.CHEST, CHEST, "jacket", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				poseStack.scale(SkinLayersModBase.config.bodyVoxelWidthSize, heightScale, widthScale);
			}));
			break;
		case LEFT_SLEEVE:
			partitions.add(new ModelPartition(VanillaModelTransformer.LEFT_ARM, LEFT_ARM, "leftSleeve", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				poseStack.scale(widthScale, heightScale, widthScale);
			}));
			break;
		case RIGHT_SLEEVE:
			partitions.add(new ModelPartition(VanillaModelTransformer.RIGHT_ARM, RIGHT_ARM, "rightSleeve", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				poseStack.scale(widthScale, heightScale, widthScale);
			}));
			break;
		case LEFT_PANTS_LEG:
			partitions.add(new ModelPartition(VanillaModelTransformer.LEFT_LEG, LEFT_LEG, "leftPantsLeg", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				poseStack.scale(widthScale, heightScale, widthScale);
			}));
			break;
		case RIGHT_PANTS_LEG:
			partitions.add(new ModelPartition(VanillaModelTransformer.RIGHT_LEG, RIGHT_LEG, "rightPantsLeg", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				poseStack.scale(widthScale, heightScale, widthScale);
			}));
			break;
		case HAT:
			partitions.add(new ModelPartition(VanillaModelTransformer.HEAD, HEAD, "hat", skinlayerModelPart, vanillaModelPart, vanillaCubes, cubes, (poseStack) -> {
				float headsize = SkinLayersModBase.config.headVoxelSize;
				poseStack.translate(0.0D, -0.25D * 24.0D, 0.0D);
				poseStack.scale(headsize, headsize, headsize);
                poseStack.translate(0.0D, 0.25D * 24.0D, 0.0D);
                poseStack.translate(0.0D, -0.04D * 24.0D, 0.0D);
			}));
			break;
		default:
			return null;
		}
		
		return bakeMeshFromCubes(abstractClientPlayer, partitions);
	}
	
	private static SkinnedMesh bakeMeshFromCubes(AbstractClientPlayer abstractClientPlayer, List<ModelPartition> partitions) {
		List<SingleGroupVertexBuilder> vertices = Lists.newArrayList();
		Map<MeshPartDefinition, IntList> indices = Maps.newHashMap();
		PoseStack poseStack = new PoseStack();
		PartTransformer.IndexCounter indexCounter = new PartTransformer.IndexCounter();
		
		poseStack.mulPose(QuaternionUtils.YP.rotationDegrees(180.0F));
		poseStack.mulPose(QuaternionUtils.XP.rotationDegrees(180.0F));
		poseStack.translate(0, -24F, 0);
		
		for (ModelPartition modelpartition : partitions) {
			bake(abstractClientPlayer, poseStack, modelpartition, vertices, indices, indexCounter);
		}
		
		return SingleGroupVertexBuilder.loadVertexInformation(vertices, indices);
	}
	
	private static void bake(AbstractClientPlayer abstractClientPlayer, PoseStack poseStack, ModelPartition modelpartition, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
		modelpartition.vanillaModelPart.loadPose(modelpartition.vanillaModelPart.getInitialPose());
		ModelPart part = modelpartition.vanillaModelPart;
		
		poseStack.pushPose();
		poseStack.translate(part.x, part.y, part.z);
		
		if (part.xRot != 0.0F || part.yRot != 0.0F || part.zRot != 0.0F) {
			poseStack.mulPose(new Quaternionf().rotationZYX(part.zRot, part.yRot, part.xRot));
		}
		
		if (part.xScale != 1.0F || part.yScale != 1.0F || part.zScale != 1.0F) {
			poseStack.scale(part.xScale, part.yScale, part.zScale);
		}
		
		MeshTransformer transformer = SkinLayersAPI.getMeshTransformerProvider().prepareTransformer(modelpartition.vanillaModelPart);
		LayerFeatureTransformerAPI.getTransformer().transform(abstractClientPlayer, poseStack, modelpartition.vanillaModelPart);
		modelpartition.transformFunction.accept(poseStack);
		
		MixinCustomModelPart customModelPart = (MixinCustomModelPart)modelpartition.skinlayerModelPart;
		poseStack.translate(customModelPart.getX(), customModelPart.getY(), customModelPart.getZ());
		
        if (customModelPart.getXRot() != 0.0F || customModelPart.getYRot() != 0.0F || customModelPart.getZRot() != 0.0F) {
            poseStack.mulPose(new Quaternionf().rotationZYX(customModelPart.getXRot(), customModelPart.getYRot(), customModelPart.getZRot()));
        }
		
		for (ModelPart.Cube cube : modelpartition.vanillaCubes) {
			transformer.transform(cube);
			modelpartition.vanillaPartTransformer.bakeCube(poseStack, VanillaMeshPartDefinition.of(modelpartition.partName), cube, vertices, indices, indexCounter);
		}
		
		for (CustomizableCube cube : modelpartition.customizableCubes) {
			modelpartition.partTransformer.bakeCube(poseStack, VanillaMeshPartDefinition.of(modelpartition.partName), cube, vertices, indices, indexCounter);
		}
		
		poseStack.popPose();
	}
	
	static class SimpleTransformer extends PartTransformer<CustomizableCube> {
		final int jointId;
		
		public SimpleTransformer(int jointId) {
			this.jointId = jointId;
		}
		
		@Override
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, CustomizableCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			CustomizableCube.Polygon[] polygons = ((SkinLayer3DMixinCustomModelCube)cube).getPolygons();
			
			for (CustomizableCube.Polygon polygon : polygons) {
				if (polygon == null) {
					continue;
				}
				
				Vector3f norm = new Vector3f(polygon.normal.x, polygon.normal.y, polygon.normal.z);
				norm.mul(poseStack.last().normal());
				
				for (CustomizableCube.Vertex vertex : polygon.vertices) {
					Vector4f pos = new Vector4f(vertex.pos.x, vertex.pos.y, vertex.pos.z, 1.0F);
					pos.mul(poseStack.last().pose());
					vertices.add(new SingleGroupVertexBuilder()
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z()).scale(0.0625F))
						.setNormal(new Vec3f(norm.x(), norm.y(), norm.z()))
						.setTextureCoordinate(new Vec2f(vertex.u, vertex.v))
						.setEffectiveJointIDs(new Vec3f(this.jointId, 0, 0))
						.setEffectiveJointWeights(new Vec3f(1.0F, 0.0F, 0.0F))
						.setEffectiveJointNumber(1)
					);
				}
				
				triangluatePolygon(indices, partName, indexCounter);
			}
		}
	}
	
	static class ChestPartTransformer extends PartTransformer<CustomizableCube> {
		static final float X_PLANE = 0.0F;
		static final VertexWeight[] WEIGHT_ALONG_Y = { new VertexWeight(13.6666F, 0.230F, 0.770F), new VertexWeight(15.8333F, 0.254F, 0.746F), new VertexWeight(18.0F, 0.5F, 0.5F), new VertexWeight(20.1666F, 0.744F, 0.256F), new VertexWeight(22.3333F, 0.770F, 0.230F)};
		final float yClipCoord;
		
		public ChestPartTransformer(float yBasis) {
			this.yClipCoord = yBasis;
		}
		
		@Override
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, CustomizableCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			List<AnimatedPolygon> xClipPolygons = Lists.<AnimatedPolygon>newArrayList();
			List<AnimatedPolygon> xyClipPolygons = Lists.<AnimatedPolygon>newArrayList();
			CustomizableCube.Polygon[] polygons = ((SkinLayer3DMixinCustomModelCube)cube).getPolygons();
			
			for (CustomizableCube.Polygon polygon : polygons) {
				if (polygon == null) {
					continue;
				}
				
				Matrix4f matrix = poseStack.last().pose();
				ModelPart.Vertex pos0 = getTranslatedVertex(polygon.vertices[0], matrix);
				ModelPart.Vertex pos1 = getTranslatedVertex(polygon.vertices[1], matrix);
				ModelPart.Vertex pos2 = getTranslatedVertex(polygon.vertices[2], matrix);
				ModelPart.Vertex pos3 = getTranslatedVertex(polygon.vertices[3], matrix);
				Direction direction = getDirectionFromVector(polygon.normal.x, polygon.normal.y, polygon.normal.z);
				
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
				Direction direction = getDirectionFromVector(polygon.normal.x, polygon.normal.y, polygon.normal.z);
				List<VertexWeight> vertexWeights = getMiddleYClipWeights(pos1.pos.y(), pos2.pos.y());
				List<AnimatedVertex> animatedVertices = Lists.<AnimatedVertex>newArrayList();
				animatedVertices.add(pos0);
				animatedVertices.add(pos1);
				
				if (vertexWeights.size() > 0) {
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
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z()).scale(0.0625F))
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
			
			for (int i = 0; i < WEIGHT_ALONG_Y.length - 1; i++) {
				VertexWeight lower = WEIGHT_ALONG_Y[i];
				VertexWeight upper = WEIGHT_ALONG_Y[i + 1];
				
				if (y >= lower.yClipCoord && y <= upper.yClipCoord) {
					float span = upper.yClipCoord - lower.yClipCoord;
					float t = span > 1.0E-5F ? (y - lower.yClipCoord) / span : 0.0F;
					
					return new VertexWeight(y,
						lower.chestWeight + (upper.chestWeight - lower.chestWeight) * t,
						lower.torsoWeight + (upper.torsoWeight - lower.torsoWeight) * t);
				}
			}
			
			return new VertexWeight(y, 1.0F, 0.0F);
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
		
		static List<VertexWeight> getMiddleYClipWeights(float minY, float maxY) {
			List<VertexWeight> cutYs = Lists.<VertexWeight>newArrayList();
			for (VertexWeight vertexWeight : WEIGHT_ALONG_Y) {
				if (vertexWeight.yClipCoord > minY && maxY >= vertexWeight.yClipCoord) {
					cutYs.add(vertexWeight);
				}
			}
			return cutYs;
		}
	}
	
	static class LimbPartTransformer extends PartTransformer<CustomizableCube> {
		final int upperJoint;
		final int lowerJoint;
		final int middleJoint;
		final boolean bendInFront;
		final float yClipCoord;
		
		public LimbPartTransformer(int upperJoint, int lowerJoint, int middleJoint, float yClipCoord, boolean bendInFront) {
			this.upperJoint = upperJoint;
			this.lowerJoint = lowerJoint;
			this.middleJoint = middleJoint;
			this.bendInFront = bendInFront;
			this.yClipCoord = yClipCoord;
		}
		
		@Override
		public void bakeCube(PoseStack poseStack, MeshPartDefinition partName, CustomizableCube cube, List<SingleGroupVertexBuilder> vertices, Map<MeshPartDefinition, IntList> indices, PartTransformer.IndexCounter indexCounter) {
			List<AnimatedPolygon> animatedPolygons = Lists.<AnimatedPolygon>newArrayList();
			CustomizableCube.Polygon[] polygons = ((SkinLayer3DMixinCustomModelCube)cube).getPolygons();
			
			for (CustomizableCube.Polygon polygon : polygons) {
				if (polygon == null) {
					continue;
				}
				
				Matrix4f matrix = poseStack.last().pose();
				ModelPart.Vertex pos0 = getTranslatedVertex(polygon.vertices[0], matrix);
				ModelPart.Vertex pos1 = getTranslatedVertex(polygon.vertices[1], matrix);
				ModelPart.Vertex pos2 = getTranslatedVertex(polygon.vertices[2], matrix);
				ModelPart.Vertex pos3 = getTranslatedVertex(polygon.vertices[3], matrix);
				Direction direction = getDirectionFromVector(polygon.normal.x, polygon.normal.y, polygon.normal.z);
				
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
					
					animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, upperId), new AnimatedVertex(pos1, upperId),
						new AnimatedVertex(pos5, upperId), new AnimatedVertex(pos4, upperId)
					}, direction));
					animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos4, lowerId), new AnimatedVertex(pos5, lowerId),
						new AnimatedVertex(pos2, lowerId), new AnimatedVertex(pos3, lowerId)
					}, direction));
					
					boolean hasSameZ = pos4.pos.z() < 0.0F == pos5.pos.z() < 0.0F;
					boolean isFront = hasSameZ && (pos4.pos.z() < 0.0F == this.bendInFront);
					
					if (isFront) {
						animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, this.middleJoint), new AnimatedVertex(pos5, this.middleJoint),
							new AnimatedVertex(pos5, this.upperJoint), new AnimatedVertex(pos4, this.upperJoint)
						}, 0.001F, direction));
						animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
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
						
						animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, firstJoint), new AnimatedVertex(pos5, secondJoint),
							new AnimatedVertex(pos5, thirdJoint), new AnimatedVertex(pos4, fourthJoint)
						}, 0.001F, direction));
						animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
							new AnimatedVertex(pos4, fourthJoint), new AnimatedVertex(pos5, thirdJoint),
							new AnimatedVertex(pos5, fifthJoint), new AnimatedVertex(pos4, sixthJoint)
						}, 0.001F, direction));
					}
				} else {
					int jointId = pos0.pos.y() > this.yClipCoord ? this.upperJoint : this.lowerJoint;
					animatedPolygons.add(new AnimatedPolygon(new AnimatedVertex[] {
						new AnimatedVertex(pos0, jointId), new AnimatedVertex(pos1, jointId),
						new AnimatedVertex(pos2, jointId), new AnimatedVertex(pos3, jointId)
					}, direction));
				}
			}
			
			for (AnimatedPolygon quad : animatedPolygons) {
				Vector3f norm = new Vector3f(quad.normal);
				norm.mul(poseStack.last().normal());
				
				for (AnimatedVertex vertex : quad.animatedVertexPositions) {
					Vector4f pos = new Vector4f(vertex.pos, 1.0F);
					vertices.add(new SingleGroupVertexBuilder()
						.setPosition(new Vec3f(pos.x(), pos.y(), pos.z()).scale(0.0625F))
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
	
	static Direction getDirectionFromVector(float x, float y, float z) {
		for (Direction direction : Direction.values()) {
			Vector3f direcVec = new Vector3f(Float.compare(x, -0.0F) == 0 ? 0.0F : x, y, z);
			
			if (direcVec.equals(direction.step())) {
				return direction;
			}
		}
		
		return null;
	}
	
	static Vector3f getClipPoint(Vector3f pos1, Vector3f pos2, float yClip) {
		Vector3f direct = new Vector3f(pos2);
		direct.sub(pos1);
		direct.mul((yClip - pos1.y()) / (pos2.y() - pos1.y()));
		
		Vector3f clipPoint = new Vector3f(pos1);
		clipPoint.add(direct);
		
		return clipPoint;
	}
	
	static ModelPart.Vertex getTranslatedVertex(CustomizableCube.Vertex original, Matrix4f matrix) {
		Vector4f translatedPosition = new Vector4f(original.pos.x, original.pos.y, original.pos.z, 1.0F);
		translatedPosition.mul(matrix);
		
		return new ModelPart.Vertex(translatedPosition.x(), translatedPosition.y(), translatedPosition.z(), original.u, original.v);
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
}
