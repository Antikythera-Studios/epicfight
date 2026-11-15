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
	
	/// Returns the data-key set for a skill class, deriving it on first access.
	/// Addon skills bound after [onBake] (late deferred binds, datapack-driven
	/// skills) never appear in the baked map — deriving here keeps their data
	/// keys available instead of silently registering none, which made
	/// [SkillDataManager#getDataValue] return null and crash addon listeners.
	public static Set<Holder<SkillDataKey<?>>> getSkillDataKeys(Class<? extends Skill> skillClass) {
		return INSTANCE.getDataKeysFor(skillClass);
	}
	
	public Set<Holder<SkillDataKey<?>>> getDataKeysFor(Class<? extends Skill> skillClass) {
		return CLASS_TO_DATA_KEYS.computeIfAbsent(skillClass, this::collectDataKeys);
	}
	
	private final HashMultimap<Class<? extends Skill>, Holder<SkillDataKey<?>>> dataKeysBySkillClasses = HashMultimap.create();
	
	/// Collects every data key declared on the class or any Skill superclass.
	@SuppressWarnings("unchecked")
	private Set<Holder<SkillDataKey<?>>> collectDataKeys(Class<? extends Skill> skillClass) {
		Set<Holder<SkillDataKey<?>>> dataKeySet = new HashSet<> ();
		Class<?> cls = skillClass;
		
		do {
			dataKeySet.addAll(this.dataKeysBySkillClasses.get((Class<? extends Skill>)cls));
			cls = cls.getSuperclass();
		} while (cls != null && Skill.class.isAssignableFrom(cls));
		
		return dataKeySet;
	}
	
	@Override
	public void onBake(Registry<SkillDataKey<?>> registry) {
		EpicFightRegistries.SKILL.holders().forEach(holder -> {
			Class<? extends Skill> skillClass = holder.value().getClass();
			Set<Holder<SkillDataKey<?>>> dataKeySet = CLASS_TO_DATA_KEYS.computeIfAbsent(skillClass, this::collectDataKeys);
			
			if (!dataKeySet.isEmpty()) {
				EpicFight.LOGGER.info("Data keys "  + dataKeySet.stream().map(Holder::getRegisteredName).toList() + " for " + holder.getRegisteredName());
			}
		});
    }
	
	@Override
	public void onAdd(Registry<SkillDataKey<?>> registry, int id, ResourceKey<SkillDataKey<?>> key, SkillDataKey<?> value) {
		addForClasses(value.referencingSkillClasses(), registry.getHolder(key).orElseThrow());
	}
	
	/// Binds a data-key holder to its referencing skill classes and drops any
	/// already-derived entries that must now include it — a late-bound addon key
	/// can arrive after its skill class was first collected, and the stale set
	/// would miss the key forever.
	void addForClasses(Set<Class<? extends Skill>> skillClasses, Holder<SkillDataKey<?>> holder) {
		skillClasses.forEach(cls -> {
			this.dataKeysBySkillClasses.put(cls, holder);
			CLASS_TO_DATA_KEYS.keySet().removeIf(cls::isAssignableFrom);
		});
	}

	@Override
	public void onClear(Registry<SkillDataKey<?>> registry, boolean full) {
		CLASS_TO_DATA_KEYS.clear();
	}
}
