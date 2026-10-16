package yesman.epicfight.world.capabilities.skill;

import com.google.common.collect.HashMultimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillContainer;
import yesman.epicfight.skill.SkillSlot;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class PlayerSkills {
	public static final PlayerSkills EMPTY = new PlayerSkills(null);
	public final SkillContainer[] skillContainers;
	private final Map<Skill, SkillContainer> containersBySkill = new HashMap<>();
	private final HashMultimap<SkillCategory, SkillContainer> containersByCategory = HashMultimap.create();
	private final HashMultimap<SkillCategory, Skill> learnedSkills = HashMultimap.create();
	
	public PlayerSkills(PlayerPatch<?> playerpatch) {
		// Addons register their SkillSlot/SkillCategory enum classes during mod
		// init, but an entrypoint that runs after EpicFight's own loadEnum never
		// gets its enum class-initialized; the ordinal is then assigned lazily on
		// first access and lands past the end of this array. Loading again here
		// is a no-op for already-initialized enums and guarantees every
		// registered slot exists before the array is sized.
		SkillCategory.ENUM_MANAGER.loadEnum();
		SkillSlot.ENUM_MANAGER.loadEnum();
		Collection<SkillSlot> slots = SkillSlot.ENUM_MANAGER.universalValues();
		this.skillContainers = new SkillContainer[slots.size()];
		
		for (SkillSlot slot : slots) {
			SkillContainer skillContainer = new SkillContainer(playerpatch, slot);
			this.skillContainers[slot.universalOrdinal()] = skillContainer;
			this.containersByCategory.put(slot.category(), skillContainer);
		}
	}
	
	public void addLearnedSkill(Skill skill) {
		SkillCategory category = skill.getCategory();
		
		if (!this.learnedSkills.containsKey(category) || !this.learnedSkills.get(category).contains(skill)) {
			this.learnedSkills.put(category, skill);
		}
	}
	
	public boolean removeLearnedSkill(Skill skill) {
		SkillCategory category = skill.getCategory();
		
		if (this.learnedSkills.containsKey(category)) {
			if (this.learnedSkills.remove(category, skill)) {
				if (this.learnedSkills.get(category).isEmpty()) {
					this.learnedSkills.removeAll(category);
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	public boolean hasCategory(SkillCategory skillCategory) {
		return this.learnedSkills.containsKey(skillCategory);
	}
	
	public boolean hasEmptyContainer(SkillCategory skillCategory) {
		for (SkillContainer container : this.containersByCategory.get(skillCategory)) {
			if (container.isEmpty()) return true; 
		}
		
		return false;
	}
	
	/**
	 * @return null if there is not empty container
	 */
	@Nullable
	public SkillContainer getFirstEmptyContainer(SkillCategory skillCategory) {
		for (SkillContainer container : this.containersByCategory.get(skillCategory)) {
			if (container.isEmpty()) return container; 
		}
		
		return null;
	}
	
	public boolean isEquipping(Skill skill) {
		return this.containersBySkill.containsKey(skill);
	}
	
	public boolean hasLearned(Skill skill) {
		return this.learnedSkills.get(skill.getCategory()).contains(skill);
	}
	
	public Set<SkillContainer> getSkillContainersFor(SkillCategory skillCategory) {
		return this.containersByCategory.get(skillCategory);
	}
	
	public SkillContainer getSkillContainerFor(SkillSlot skillSlot) {
		return this.getSkillContainerFor(skillSlot.universalOrdinal());
	}
	
	public SkillContainer getSkillContainerFor(int slotIndex) {
		return this.skillContainers[slotIndex];
	}
	
	@ApiStatus.Internal
	public void setSkillToContainer(Skill skill, SkillContainer container) {
		this.containersBySkill.put(skill, container);
	}
	
	@ApiStatus.Internal
	public void removeSkillFromContainer(Skill skill) {
		this.containersBySkill.remove(skill);
	}
	
	public SkillContainer getSkillContainer(Skill skill) {
		return this.containersBySkill.get(skill);
	}
	
	public Stream<SkillContainer> listSkillContainers() {
		return Stream.of(this.skillContainers);
	}
	
	public Stream<Skill> listAcquiredSkills() {
		return this.learnedSkills.values().stream();
	}
	
	public void clearContainersAndLearnedSkills(boolean isLocalOrServerPlayer) {
		for (SkillContainer container : this.skillContainers) {
			if (container.getSlot().category().learnable()) {
				if (isLocalOrServerPlayer) {
                    container.setSkill(null);
                } else {
                    container.setSkillRemote(null);
                }

                container.setReplaceCooldown(0);
			}
		}
		
		this.learnedSkills.clear();
	}
	
	public void copyFrom(PlayerSkills capabilitySkill) {
		int i = 0;
		
		for (SkillContainer container : this.skillContainers) {
			Skill oldone = capabilitySkill.skillContainers[i].getSkill();
			
			if (oldone != null && oldone.getCategory().shouldSynchronize()) {
				container.setSkill(capabilitySkill.skillContainers[i].getSkill());
				container.setReplaceCooldown(capabilitySkill.skillContainers[i].getReplaceCooldown());
			}
			
			i++;
		}
		
		this.learnedSkills.putAll(capabilitySkill.learnedSkills);
	}

	public CompoundTag write(CompoundTag compound) {
		CompoundTag skillCompound = new CompoundTag();
		
		for (SkillContainer container : this.skillContainers) {
			if (container.getSkill() != null && container.getSkill().getCategory().shouldSave()) {
				skillCompound.putString(ParseUtil.toLowerCase(container.getSlot().toString()), container.getSkill().toString());
			}
		}
		
		for (Map.Entry<SkillCategory, Collection<Skill>> entry : this.learnedSkills.asMap().entrySet()) {
			CompoundTag learnedNBT = new CompoundTag();
			int i = 0;
			
			for (Skill skill : entry.getValue()) {
				learnedNBT.putString(String.valueOf(i++), skill.toString());
			}
			
			skillCompound.put("learned:" + ParseUtil.toLowerCase(entry.getKey().toString()), learnedNBT);
		}
		
		skillCompound.putString("playerMode", this.skillContainers[0].getExecutor().getPlayerMode().toString());
		compound.put("playerSkills", skillCompound);
		
		return compound;
	}
	
	public void read(CompoundTag compound) {
		CompoundTag skillCompound = compound.getCompound("playerSkills");
		
		for (SkillContainer container : this.skillContainers) {
			String key = ParseUtil.toLowerCase(container.getSlot().toString());
			
			if (skillCompound.contains(key)) {
				EpicFightRegistries.SKILL.getHolder(ResourceLocation.parse(skillCompound.getString(key))).ifPresent(skill -> {
					container.setSkill(skill.value());
					this.addLearnedSkill(skill.value());
				});
			}
		}
		
		for (SkillCategory category : SkillCategory.ENUM_MANAGER.universalValues()) {
			if (skillCompound.contains("learned:" + ParseUtil.toLowerCase(category.toString()))) {
				CompoundTag learnedNBT = skillCompound.getCompound("learned:" + ParseUtil.toLowerCase(category.toString()));
				
				for (String key : learnedNBT.getAllKeys()) {
					EpicFightRegistries.SKILL.getHolder(ResourceLocation.parse(learnedNBT.getString(key))).ifPresent(skill -> {
						this.addLearnedSkill(skill.value());
					});
				}
			}
		}
		
		if (skillCompound.contains("playerMode")) {
			this.skillContainers[0].getExecutor().toMode(PlayerPatch.PlayerMode.valueOf(ParseUtil.toUpperCase(skillCompound.getString("playerMode"))), true);
		} else {
			this.skillContainers[0].getExecutor().toEpicFightMode(true);
		}
	}
}