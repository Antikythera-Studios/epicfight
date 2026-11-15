package yesman.epicfight.client.mesh;

import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.EquipmentSlot;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;

public class PiglinMesh extends HumanoidMesh {
	public PiglinMesh(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
	}
	
	@Override
	public AssetAccessor<? extends SkinnedMesh> getHumanoidArmorModel(EquipmentSlot slot) {
		switch (slot) {
		case HEAD:
			return Meshes.HELMET_PIGLIN;
		case CHEST:
			return Meshes.CHESTPLATE;
		case LEGS:
			return Meshes.LEGGINS;
		case FEET:
			return Meshes.BOOTS;
		default:
			return null;
		}
	}
}