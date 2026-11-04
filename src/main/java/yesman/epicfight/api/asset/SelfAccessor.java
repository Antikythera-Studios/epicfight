package yesman.epicfight.api.asset;

import net.minecraft.resources.ResourceLocation;

public record SelfAccessor<A>(ResourceLocation registryName, A asset) implements AssetAccessor<A> {
	public static <A> SelfAccessor<A> create(ResourceLocation registryName, A asset) {
		return new SelfAccessor<> (registryName, asset);
	}
	
	@Override
	public A get() {
		return this.asset;
	}
	
	@Override
	public boolean inRegistry() {
		return false;
	}
}
