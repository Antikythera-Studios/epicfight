package yesman.epicfight.skill;
import yesman.epicfight.EpicFight;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.utils.ExtensibleEnum;
import yesman.epicfight.api.utils.ExtensibleEnumManager;
import yesman.epicfight.api.utils.ParseUtil;
import yesman.epicfight.client.gui.screen.SkillEditScreen;

public interface SkillCategory extends ExtensibleEnum {
    ResourceLocation DEFAULT_BOOK_ICON = EpicFight.identifier("skillbook");
	
	ExtensibleEnumManager<SkillCategory> ENUM_MANAGER = new ExtensibleEnumManager<> ("skill_category");
	
	/// Determines if the skill should be saved in NBT
	boolean shouldSave();
	
	/// Determines if the skill should be synched to clients
	boolean shouldSynchronize();
	
	/// Determines if the skill is modifiable by player, through skill books, commands, or [SkillEditScreen]
	boolean learnable();

    /// Returns a translation key
    /// For backward compatibility, this method is implemented as default
    default Component getTranslationKey() {
        return Component.translatable(String.format("skill.%s.category.%s", EpicFight.MODID, ParseUtil.toLowerCase(this.toString())));
    }

	/// Texture location of the Skill book icon in inventory UI
	default ResourceLocation bookIcon() {
		return DEFAULT_BOOK_ICON;
	}
}