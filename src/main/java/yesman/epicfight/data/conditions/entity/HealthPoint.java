package yesman.epicfight.data.conditions.entity;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Environment;

import io.netty.util.internal.StringUtil;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.util.List;
import java.util.Locale;

public class HealthPoint extends EntityPatchCondition {
	private double health;
	private Comparator comparator;
	
	public HealthPoint() {
		this.health = 0.0D;
	}
	
	public HealthPoint(double health, Comparator comparator) {
		this.health = health;
		this.comparator = comparator;
	}
	
	@Override
	public HealthPoint read(CompoundTag tag) {
		this.health = this.assertTag("health", "decimal", tag, NumericTag.class, CompoundTag::getDouble);
		this.comparator = this.assertEnumTag("comparator", Comparator.class, tag);
		
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();
		tag.putString("comparator", ParseUtil.toLowerCase(this.comparator.toString()));
		tag.putDouble("health", this.health);
		
		return tag;
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> target) {
		switch (this.comparator) {
		case LESS_ABSOLUTE:
			return this.health > target.getOriginal().getHealth();
		case GREATER_ABSOLUTE:
			return this.health < target.getOriginal().getHealth();
		case LESS_RATIO:
			return this.health > target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
		case GREATER_RATIO:
			return this.health < target.getOriginal().getHealth() / target.getOriginal().getMaxHealth();
		}
		
		return true;
	}
	
	@Override @ClientOnly
    @Environment(EnvType.CLIENT) // TODO: Remove @Environment annotation and completely decouple the widget provider code
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox editbox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("health"), null, null);
		AbstractWidget comboBox = new ComboBox<>(screen, Minecraft.getInstance().font, 0, 0, 0, 0, null, null, 4, Component.literal("comparator"), List.of(Comparator.values()), ParseUtil::snakeToSpacedCamel, null);
		
		editbox.setFilter((context) -> StringUtil.isNullOrEmpty(context) || ParseUtil.isParsable(context, Double::parseDouble));
		
		return List.of(
			ParameterEditor.of((value) -> ParseUtil.parseOrGet(value.toString(), (v) -> DoubleTag.valueOf(Double.parseDouble(value.toString())), StringTag.valueOf("")), (tag) -> ParseUtil.valueOfOmittingType(ParseUtil.nullOrToString(tag, Tag::getAsString)), editbox),
			ParameterEditor.of((value) -> StringTag.valueOf(value.toString().toLowerCase(Locale.ROOT)), (tag) -> ParseUtil.enumValueOfOrNull(Comparator.class, ParseUtil.nullOrToString(tag, Tag::getAsString)), comboBox)
		);
	}
	
	public enum Comparator {
		GREATER_ABSOLUTE, LESS_ABSOLUTE, GREATER_RATIO, LESS_RATIO
	}
}
