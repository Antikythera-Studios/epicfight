package yesman.epicfight.api.client.model;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.common.collect.ImmutableList;

import net.minecraft.world.phys.Vec3;
import yesman.epicfight.api.client.physics.cloth.ClothSimulatable;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator;
import yesman.epicfight.api.client.physics.cloth.ClothSimulator.ClothObject;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public abstract class StaticMesh<P extends MeshPart> implements Mesh, SoftBodyTranslatable {
	protected final float[] positions;
	protected final float[] normals;
	protected final float[] uvs;
	
	protected final int vertexCount;
	protected final Mesh.RenderProperties renderProperties;
	protected final Map<String, P> parts;
	protected final List<Vec3> normalList;
	
	private Map<String, ClothSimulationInfo> softBodySimulationInfo;
	
	/**
	 * @param arrayMap Null if parent is not null
	 * @param partBuilders Null if parent is not null
	 * @param parent Null if arrayMap and parts are not null
	 * @param renderProperties
	 */
	public StaticMesh(@Nullable Map<String, Number[]> arrayMap, @Nullable Map<MeshPartDefinition, List<VertexBuilder>> partBuilders, @Nullable StaticMesh<P> parent, Mesh.RenderProperties renderProperties) {
		this.positions = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("positions")) : parent.positions;
		this.normals = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("normals")) : parent.normals;
		this.uvs = (parent == null) ? ParseUtil.unwrapFloatWrapperArray(arrayMap.get("uvs")) : parent.uvs;
		this.parts = (parent == null) ? this.createModelPart(partBuilders) : parent.parts;
		this.renderProperties = renderProperties;
		
		int totalV = 0;
		
		for (MeshPart modelpart : this.parts.values()) {
			totalV += modelpart.getVertices().size();
		}
		
		this.vertexCount = totalV;
		
		if (this.canStartSoftBodySimulation()) {
			ImmutableList.Builder<Vec3> normalBuilder = ImmutableList.builder();
			
			for (int i = 0; i < this.normals.length / 3; i++) {
				normalBuilder.add(new Vec3(this.normals[i * 3], this.normals[i * 3 + 1], this.normals[i * 3 + 2]));
			}
			
			this.normalList = normalBuilder.build();
		} else {
			this.normalList = null;
		}
	}
	
	protected abstract Map<String, P> createModelPart(Map<MeshPartDefinition, List<VertexBuilder>> partBuilders);
	protected abstract P getOrLogException(Map<String, P> parts, String name);
	
	public boolean hasPart(String part) {
		return this.parts.containsKey(part);
	}
	
	public MeshPart getPart(String part) {
		return this.parts.get(part);
	}
	
	public Collection<P> getAllParts() {
		return this.parts.values();
	}
	
	public Set<Map.Entry<String, P>> getPartEntry() {
		return this.parts.entrySet();
	}
	
	public void putSoftBodySimulationInfo(Map<String, ClothSimulationInfo> sofyBodySimulationInfo) {
		this.softBodySimulationInfo = sofyBodySimulationInfo;
	}
	
	public Map<String, ClothSimulationInfo> getSoftBodySimulationInfo() {
		return this.softBodySimulationInfo;
	}
	
	public Mesh.RenderProperties getRenderProperties() {
		return this.renderProperties;
	}
	
	public void getVertexPosition(int positionIndex, Vector4f dest) {
		int index = positionIndex * 3;
		dest.set(this.positions[index], this.positions[index + 1], this.positions[index + 2], 1.0F);
	}
	
	public void getVertexNormal(int normalIndex, Vector3f dest) {
		int index = normalIndex * 3;
		dest.set(this.normals[index], this.normals[index + 1], this.normals[index + 2]);
	}
	
	public void getVertexPosition(int positionIndex, Vector4f dest, @Nullable OpenMatrix4f[] poses) {
		this.getVertexPosition(positionIndex, dest);
	}
	
	public void getVertexNormal(int positionIndex, int normalIndex, Vector3f dest, @Nullable OpenMatrix4f[] poses) {
		this.getVertexNormal(normalIndex, dest);
	}
	
	public float[] positions() {
		return this.positions;
	}
	
	public float[] normals() {
		return this.normals;
	}
	
	public float[] uvs() {
		return this.uvs;
	}
	
	@Nullable
	public List<Vec3> normalList() {
		return this.normalList;
	}
	
	@Override
	public void initialize() {
		this.parts.values().forEach((part) -> part.setHidden(false));
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public ClothSimulator.ClothObject createSimulationData(@Nullable SoftBodyTranslatable provider, ClothSimulatable simObject, ClothSimulator.ClothObjectBuilder simBuilder) {
		return new ClothObject(simBuilder, provider == null ? this : provider, (Map<String, MeshPart>)this.parts, this.positions);
	}
}