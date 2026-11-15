package yesman.epicfight.data.conditions.entity;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Environment;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.function.Function;

public class TargetInDistance extends EntityPatchCondition {
	private double min;
	private double max;
	
	public TargetInDistance() {
	}
	
	public TargetInDistance(double min, double max) {
		this.min = min;
		this.max = max;
	}
	
	@Override
	public TargetInDistance read(CompoundTag tag) {
		this.min = this.assertTag("min", "decimal", tag, NumericTag.class, CompoundTag::getDouble);
		this.max = this.assertTag("max", "decimal", tag, NumericTag.class, CompoundTag::getDouble);
		
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();
		tag.putDouble("min", this.min);
		tag.putDouble("max", this.max);
		
		return tag;
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> target) {
		double distanceSqr = target.getOriginal().distanceToSqr(target.getTarget());
		return this.min * this.min < distanceSqr && distanceSqr < this.max * this.max;
	}
	
	@Override @ClientOnly
    @Environment(EnvType.CLIENT) // TODO: Remove @Environment annotation and completely decouple the widget provider code
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox minEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("min"), null, null);
		ResizableEditBox maxEditBox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("max"), null, null);
		minEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
		maxEditBox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
		Function<Object, Tag> doubleParser = (value) -> DoubleTag.valueOf(Double.valueOf(value.toString()));
		Function<Tag, Object> doubleGetter = (tag) -> ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(tag, Tag::getAsString));
		
		return List.of(ParameterEditor.of(doubleParser, doubleGetter, minEditBox), ParameterEditor.of(doubleParser, doubleGetter, maxEditBox));
	}
}
