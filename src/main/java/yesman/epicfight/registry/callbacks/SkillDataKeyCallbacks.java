package yesman.epicfight.registry.callbacks;
import yesman.epicfight.EpicFight;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;



import yesman.epicfight.main.EpicFightMod;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillDataKey;

public class SkillDataKeyCallbacks implements RegistryCallback.AddCallback<SkillDataKey<?>>, RegistryCallback.BakeCallback<SkillDataKey<?>>, RegistryCallback.ClearCallback<SkillDataKey<?>> {
	private static final SkillDataKeyCallbacks INSTANCE = new SkillDataKeyCallbacks();
	
	public static SkillDataKeyCallbacks getRegistryCallback() {
		return SkillDataKeyCallbacks.INSTANCE;
	}
	
	private static final Map<Class<? extends Skill>, Set<Holder<SkillDataKey<?>>>> CLASS_TO_DATA_KEYS = Maps.newHashMap();
	
	public static Map<Class<? extends Skill>, Set<Holder<SkillDataKey<?>>>> getSkillDataKeyMap() {
		return CLASS_TO_DATA_KEYS;
	}
	
	private final HashMultimap<Class<? extends Skill>, Holder<SkillDataKey<?>>> dataKeysBySkillClasses = HashMultimap.create();
	
	@SuppressWarnings("unchecked")
	@Override
	public void onBake(Registry<SkillDataKey<?>> registry) {
		EpicFightRegistries.SKILL.holders().forEach(holder -> {
			Class<? extends Skill> skillClass = holder.value().getClass();
			Set<Holder<SkillDataKey<?>>> dataKeySet = CLASS_TO_DATA_KEYS.computeIfAbsent(skillClass, k -> new HashSet<> ());
			
			do {
				if (this.dataKeysBySkillClasses.containsKey(skillClass)) {
					dataKeySet.addAll(this.dataKeysBySkillClasses.get(skillClass));
				}
				
				skillClass = (Class<? extends Skill>) skillClass.getSuperclass();
			} while (Skill.class.isAssignableFrom(skillClass));
			
			if (!dataKeySet.isEmpty()) {
				EpicFight.LOGGER.info("Data keys "  + dataKeySet.stream().map(Holder::getRegisteredName).toList() + " for " + holder.getRegisteredName());
			}
		});
    }
	
	@Override
	public void onAdd(Registry<SkillDataKey<?>> registry, int id, ResourceKey<SkillDataKey<?>> key, SkillDataKey<?> value) {
		value.referencingSkillClasses().forEach(cls -> this.dataKeysBySkillClasses.put(cls, registry.getHolder(key).orElseThrow()));
	}

	@Override
	public void onClear(Registry<SkillDataKey<?>> registry, boolean full) {
		CLASS_TO_DATA_KEYS.clear();
	}
}
