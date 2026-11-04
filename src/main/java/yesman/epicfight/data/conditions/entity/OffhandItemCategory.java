package yesman.epicfight.data.conditions.entity;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.datapack.widgets.ComboBox;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.item.WeaponCategory;

import java.util.List;
import java.util.Locale;

public class OffhandItemCategory extends EntityPatchCondition {
	private WeaponCategory category;
	
	@Override
	public OffhandItemCategory read(CompoundTag tag) {
		this.category = this.assertExtensibleEnumTag("category", WeaponCategory.ENUM_MANAGER, tag);
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();
		tag.putString("category", this.category.toString());
		
		return tag;
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> target) {
		return target.getHoldingItemCapability(InteractionHand.OFF_HAND).getWeaponCategory() == this.category;
	}

    @Override @ClientOnly
    @Environment(EnvType.CLIENT) // TODO: Remove @Environment annotation and completely decouple the widget provider code
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		AbstractWidget comboBox = new ComboBox<>(screen, Minecraft.getInstance().font, 0, 0, 0, 0, null, null, 4, Component.literal("category"), List.copyOf(WeaponCategory.ENUM_MANAGER.universalValues()), ParseUtil::snakeToSpacedCamel, null);
		return List.of(ParameterEditor.of((value) -> StringTag.valueOf(value.toString().toLowerCase(Locale.ROOT)), (tag) -> WeaponCategory.ENUM_MANAGER.get(ParseUtil.nullOrToString(tag, Tag::getAsString)), comboBox));
	}
}