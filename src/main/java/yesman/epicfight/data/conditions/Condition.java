package yesman.epicfight.data.conditions;
import net.fabricmc.api.Environment;

import com.google.gson.JsonElement;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.fabricmc.api.EnvType;

import yesman.epicfight.api.utils.ExtensibleEnum;
import yesman.epicfight.api.utils.ExtensibleEnumManager;
import yesman.epicfight.api.utils.side.ClientOnly;
import yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface Condition<T> {
	default Condition<T> read(JsonElement json) throws CommandSyntaxException {
		return this.read(TagParser.parseTag(json.toString()));
	}
	
	Condition<T> read(CompoundTag tag) throws IllegalArgumentException;
	CompoundTag serializePredicate();
	boolean predicate(T target);
	
	default <O> O assertTag(String key, String tagFormatMessage, CompoundTag compound, Class<? extends Tag> tagType, BiFunction<CompoundTag, String, O> getter) throws IllegalArgumentException {
		if (!compound.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		Tag tag = compound.get(key);
		
		if (!tagType.isAssignableFrom(tag.getClass())) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: the {1} value must be a {2} format", this.getClass().getSimpleName(), key, tagFormatMessage));
		}
		
		return getter.apply(compound, key);
	}
	
	default <E extends Enum<E>> E assertEnumTag(String key, Class<E> enumCls, CompoundTag compound) throws IllegalArgumentException {
		if (!compound.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		String enumString = this.assertTag(key, "string", compound, StringTag.class, CompoundTag::getString).toUpperCase(Locale.ROOT);
		
		try {
			return Enum.valueOf(enumCls, enumString);
		} catch (IllegalArgumentException e) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: invalid enum for {1}: {2}", this.getClass().getSimpleName(), key, enumString));
		}
	}
	
	default <E extends ExtensibleEnum> E assertExtensibleEnumTag(String key, ExtensibleEnumManager<E> extendableEnumManager, CompoundTag compound) throws IllegalArgumentException, NoSuchElementException {
		if (!compound.contains(key)) {
			throw new IllegalArgumentException(MessageFormat.format("{0} condition error: {1} not specified!", this.getClass().getSimpleName(), key));
		}
		
		String enumString = this.assertTag(key, "string", compound, StringTag.class, CompoundTag::getString).toLowerCase(Locale.ROOT);
		
		try {
			return extendableEnumManager.getOrThrow(enumString);
		} catch (NoSuchElementException ex) {
			throw new NoSuchElementException(MessageFormat.format("{0} condition error: {1}", this.getClass().getSimpleName(), ex.getMessage()));
		}
	}

    // TODO: Remove @Environment annotation and completely decouple the widget provider code
    @ClientOnly
    @Environment(EnvType.CLIENT)
	List<ParameterEditor> getAcceptingParameters(Screen screen);
	
	abstract class EntityPatchCondition implements Condition<LivingEntityPatch<?>> {
	}
	
	abstract class EntityCondition implements Condition<Entity> {
	}
	
	abstract class ItemStackCondition implements Condition<ItemStack> {
	}

    @ClientOnly
	class ParameterEditor {
		public static ParameterEditor of(Function<Object, Tag> toTag, Function<Tag, Object> fromTag, AbstractWidget editWidget) {
			return new ParameterEditor(toTag, fromTag, editWidget);
		}
		
		public final Function<Object, Tag> toTag;
		public final Function<Tag, Object> fromTag;
		public final AbstractWidget editWidget;
		
		private ParameterEditor(Function<Object, Tag> toTag, Function<Tag, Object> fromTag, AbstractWidget editWidget) {
			this.toTag = toTag;
			this.fromTag = fromTag;
			this.editWidget = editWidget;
		}
	}
}