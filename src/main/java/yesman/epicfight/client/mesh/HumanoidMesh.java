package yesman.epicfight.client.mesh;

import java.util.List;
import java.util.Map;

import net.minecraft.world.entity.EquipmentSlot;
import yesman.epicfight.api.asset.AssetAccessor;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.client.model.Meshes;
import yesman.epicfight.api.client.model.SkinnedMesh;
import yesman.epicfight.api.client.model.VertexBuilder;

public class HumanoidMesh extends SkinnedMesh {
	public final SkinnedMeshPart head;
	public final SkinnedMeshPart torso;
	public final SkinnedMeshPart leftArm;
	public final SkinnedMeshPart rightArm;
	public final SkinnedMeshPart leftLeg;
	public final SkinnedMeshPart rightLeg;
	public final SkinnedMeshPart hat;
	public final SkinnedMeshPart jacket;
	public final SkinnedMeshPart leftSleeve;
	public final SkinnedMeshPart rightSleeve;
	public final SkinnedMeshPart leftPants;
	public final SkinnedMeshPart rightPants;
	
	public HumanoidMesh(Map<String, Number[]> arrayMap, Map<MeshPartDefinition, List<VertexBuilder>> parts, SkinnedMesh parent, RenderProperties properties) {
		super(arrayMap, parts, parent, properties);
		
		this.head = this.getOrLogException(this.parts, "head");
		this.torso = this.getOrLogException(this.parts, "torso");
		this.leftArm = this.getOrLogException(this.parts, "leftArm");
		this.rightArm = this.getOrLogException(this.parts, "rightArm");
		this.leftLeg = this.getOrLogException(this.parts, "leftLeg");
		this.rightLeg = this.getOrLogException(this.parts, "rightLeg");
		
		this.hat = this.getOrLogException(this.parts, "hat");
		this.jacket = this.getOrLogException(this.parts, "jacket");
		this.leftSleeve = this.getOrLogException(this.parts, "leftSleeve");
		this.rightSleeve = this.getOrLogException(this.parts, "rightSleeve");
		this.leftPants = this.getOrLogException(this.parts, "leftPants");
		this.rightPants = this.getOrLogException(this.parts, "rightPants");
	}
	
	public AssetAccessor<? extends SkinnedMesh> getHumanoidArmorModel(EquipmentSlot slot) {
		switch (slot) {
		case HEAD:
			return Meshes.HELMET;
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