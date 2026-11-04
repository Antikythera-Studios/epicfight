package yesman.epicfight.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.geom.ModelPart;

@Mixin(value = AgeableListModel.class)
public interface MixinAgeableListModel {
	
	@Invoker("headParts")
	public Iterable<ModelPart> invoke_headParts();
	
	@Invoker("bodyParts")
	public Iterable<ModelPart> invoke_bodyParts();
}