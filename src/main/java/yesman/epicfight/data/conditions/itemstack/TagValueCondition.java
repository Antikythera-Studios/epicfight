package yesman.epicfight.data.conditions.itemstack;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.datapack.widgets.ResizableEditBox;
import yesman.epicfight.data.conditions.Condition.ItemStackCondition;

/// Ported from Epic Fight 1.20.1 (tag_value condition was dropped upstream in 21.x but
/// datapacks written against it still ship it — e.g. simplyswords relic awakening).
///
/// In 1.21.1 items no longer carry a raw NBT tag — "tag" keys now resolve against the
/// item's data components. A key segment containing ':' (e.g.
/// `SimplySwordsComponents.simplyswords:awakening`) is resolved as a
/// [DataComponentType] id and its value is string-compared. Any other key walks the
/// `minecraft:custom_data` compound with the legacy dot/`[]` path syntax.
public class TagValueCondition extends ItemStackCondition {
	private String key;
	private String value;

	@Override
	public TagValueCondition read(CompoundTag tag) {
		this.key = tag.getString("key");
		this.value = tag.get("value").getAsString();

		if (this.key == null) {
			throw new IllegalArgumentException("No key provided!");
		}

		if (this.value == null) {
			throw new IllegalArgumentException("No value provided!");
		}

		return this;
	}

	@Override
	public CompoundTag serializePredicate() {
		CompoundTag tag = new CompoundTag();

		tag.putString("key", this.key);
		tag.putString("value", this.value);

		return tag;
	}

	@Override
	public boolean predicate(ItemStack itemstack) {
		// Component form: <holderClassPrefix>.<namespace:path> or bare <namespace:path>
		for (String segment : this.key.split("[.]")) {
			if (segment.indexOf(':') < 0) {
				continue;
			}

			ResourceLocation componentId;
			try {
				componentId = ResourceLocation.parse(segment);
			} catch (Exception e) {
				continue;
			}

			// DATA_COMPONENT_TYPE is a defaulted registry — get() on a missing id
			// silently yields minecraft:custom_data rather than null.
			if (!BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(componentId)) {
				continue;
			}

			DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(componentId);

			if (type == null) {
				continue;
			}

			Object componentValue = getComponent(itemstack, type);

			if (componentValue != null && componentValueEquals(itemstack, type, componentValue, this.value)) {
				return true;
			}
		}

		// Legacy NBT form: walk the custom_data compound with dot/[] path syntax
		String[] keys = this.key.split("[.]");
		List<Tag> visitTags = List.of(itemstack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());

		for (String pathKey : keys) {
			visitTags = visitTags(pathKey, visitTags);
		}

		for (Tag tag : visitTags) {
			if (tag.getAsString().equals(this.value)) {
				return true;
			}
		}

		return false;
	}

	private static <T> Object getComponent(ItemStack stack, DataComponentType<T> type) {
		return stack.get(type);
	}

	/// Compares a component value against the datapack's expected string. Record-style
	/// components (e.g. simplyswords `AwakeningComponent[level]`) don't toString to the
	/// scalar the pack expects, so encode the component through its codec first — the
	/// JSON form matches what the NBT era produced (int -> "8", string -> "ns:path").
	private static <T> boolean componentValueEquals(ItemStack stack, DataComponentType<T> type, Object componentValue, String expected) {
		if (String.valueOf(componentValue).equals(expected)) {
			return true;
		}

		try {
			@SuppressWarnings("unchecked")
			T typed = (T) componentValue;
			JsonElement encoded = type.codec().encodeStart(JsonOps.INSTANCE, typed).result().orElse(null);

			if (encoded == null) {
				return false;
			}

			if (encoded.isJsonPrimitive()) {
				return encoded.getAsString().equals(expected);
			}
		} catch (Throwable ignored) {
		}

		return false;
	}

	@Override
	@Environment(EnvType.CLIENT)
	public List<ParameterEditor> getAcceptingParameters(Screen screen) {
		ResizableEditBox keyEditBox = new ResizableEditBox(net.minecraft.client.Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("key"), null, null);
		ResizableEditBox valueEditBox = new ResizableEditBox(net.minecraft.client.Minecraft.getInstance().font, 0, 0, 0, 0, Component.literal("value"), null, null);
		Function<Object, Tag> stringParser = (value) -> StringTag.valueOf(value.toString());
		Function<Tag, Object> stringGetter = (tag) -> ParseUtil.nullOrToString(tag, Tag::getAsString);

		return List.of(ParameterEditor.of(stringParser, stringGetter, keyEditBox), ParameterEditor.of(stringParser, stringGetter, valueEditBox));
	}

	private static List<Tag> visitTags(String key, List<Tag> compoundTag) {
		Pattern pattern = Pattern.compile("\\[[0-9]*\\]", Pattern.MULTILINE);
		Matcher matcher = pattern.matcher(key);
		List<Tag> childs = Lists.newArrayList();

		if (matcher.find()) {
			String sIndex = matcher.group().replaceAll("[\\[\\]]", "");
			String arrayKey = matcher.replaceAll("");

			if (sIndex.isEmpty()) {
				for (Tag tag : compoundTag) {
					if (tag instanceof CompoundTag compTag && compTag.contains(arrayKey)) {
						ListTag listTag = (ListTag) compTag.get(arrayKey);

						for (Tag listTagElement : listTag) {
							childs.add(listTagElement);
						}
					}
				}
			} else {
				int index = Integer.parseInt(sIndex);

				for (Tag tag : compoundTag) {
					if (tag instanceof CompoundTag compTag && compTag.contains(arrayKey)) {
						ListTag listTag = (ListTag) compTag.get(arrayKey);
						childs.add(listTag.get(index));
					}
				}
			}
		} else {
			for (Tag tag : compoundTag) {
				if (tag instanceof CompoundTag compTag && compTag.contains(key)) {
					childs.add(compTag.get(key));
				}
			}
		}

		return childs;
	}
}
