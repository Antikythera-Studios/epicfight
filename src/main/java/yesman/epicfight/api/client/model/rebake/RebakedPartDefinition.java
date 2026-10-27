package yesman.epicfight.api.client.model.rebake;

import java.util.function.Supplier;

import javax.annotation.Nullable;

import yesman.epicfight.api.client.model.Mesh;
import yesman.epicfight.api.client.model.MeshPartDefinition;
import yesman.epicfight.api.utils.math.OpenMatrix4f;

public final class RebakedPartDefinition implements MeshPartDefinition {
	private final String partName;

	@Nullable
	private final Mesh.RenderProperties renderProperties;

	@Nullable
	private final Supplier<OpenMatrix4f> liveTransform;

	public RebakedPartDefinition(String partName, @Nullable Mesh.RenderProperties renderProperties, @Nullable Supplier<OpenMatrix4f> liveTransform) {
		this.partName = partName;
		this.renderProperties = renderProperties;
		this.liveTransform = liveTransform;
	}

	@Override
	public String partName() {
		return this.partName;
	}

	@Override
	public Mesh.RenderProperties renderProperties() {
		return this.renderProperties;
	}

	@Override
	public Supplier<OpenMatrix4f> getModelPartAnimationProvider() {
		return this.liveTransform != null ? this.liveTransform : () -> null;
	}

	@Override
	public boolean equals(Object other) {
		return other instanceof MeshPartDefinition definition && this.partName.equals(definition.partName());
	}

	@Override
	public int hashCode() {
		return this.partName.hashCode();
	}

	@Override
	public String toString() {
		return this.partName;
	}
}
