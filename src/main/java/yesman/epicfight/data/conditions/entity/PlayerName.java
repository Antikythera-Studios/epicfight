package yesman.epicfight.data.conditions.entity;
import net.minecraft.client.Minecraft;
import net.fabricmc.api.Environment;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition.EntityPatchCondition;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.List;

public class PlayerName extends EntityPatchCondition {
	private String name;
	
	@Override
	public PlayerName read(CompoundTag tag) {
		this.name = this.assertTag("identifier", "string", tag, StringTag.class, CompoundTag::getString);
		return this;
	}
	
	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();
		tag.putString("identifier", this.name);
		
		return tag;
	}
	
	@Override
	public boolean predicate(LivingEntityPatch<?> target) {
		if (target instanceof PlayerPatch<?> playerpatch) {
			return playerpatch.getOriginal().getName().getString().equals(this.name);
		}
		
		return false;
	}

    @Override @ClientOnly
    @Environment(EnvType.CLIENT) // TODO: Remove @Environment annotation and completely decouple the widget provider code
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox editbox = new ResizableEditBox(Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("identifier"), null, null);
		return List.of(ParameterEditor.of((name) -> StringTag.valueOf(name.toString()), (tag) -> ParseUtil.nullOrToString(tag, Tag::getAsString), editbox));
	}
}