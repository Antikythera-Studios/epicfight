package yesman.epicfight.skill;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.EpicFight;

import static yesman.epicfight.generated.LangKeys.*;

public enum SkillCategories implements SkillCategory {
    // TODO: BASIC_ATTACK -> COMBO_ATTACKS
	BASIC_ATTACK(false, false, false, SKILL_CATEGORY_COMBO_ATTACKS),
	DODGE(true, true, true, SKILL_CATEGORY_DODGE, EpicFight.identifier("skillbook_dodge")),
	PASSIVE(true, true, true, SKILL_CATEGORY_PASSIVE, EpicFight.identifier("skillbook_passive")),
	WEAPON_PASSIVE(false, false, false, SKILL_CATEGORY_WEAPON_PASSIVE),
	WEAPON_INNATE(false, true, false, SKILL_CATEGORY_WEAPON_INNATE),
	GUARD(true, true, true, SKILL_CATEGORY_GUARD, EpicFight.identifier("skillbook_guard")),
	KNOCKDOWN_WAKEUP(false, false, false, SKILL_CATEGORY_KNOCKDOWN_WAKEUP),
	MOVER(true, true, true, SKILL_CATEGORY_MOVER, EpicFight.identifier("skillbook_mover")),
	IDENTITY(true, true, true, SKILL_CATEGORY_IDENTITY, EpicFight.identifier("skillbook_identity")),
	EMPTY(false, false, false, SKILL_CATEGORY_EMPTY);

	final boolean shouldSave;
    final boolean shouldSyncronize;
    final boolean modifiable;
    final int id;
    final Component translationKey;
    final ResourceLocation bookIcon;

	SkillCategories(boolean shouldSave, boolean shouldSyncronizedAllPlayers, boolean modifiable, String translationKey) {
		this.shouldSave = shouldSave;
		this.shouldSyncronize = shouldSyncronizedAllPlayers;
		this.modifiable = modifiable;
        this.translationKey = Component.translatable(translationKey);
		this.id = SkillCategory.ENUM_MANAGER.assign(this);
        this.bookIcon = SkillCategory.DEFAULT_BOOK_ICON;
	}

	SkillCategories(boolean shouldSave, boolean shouldSyncronizedAllPlayers, boolean modifiable, String translationKey, ResourceLocation bookIcon) {
		this.shouldSave = shouldSave;
		this.shouldSyncronize = shouldSyncronizedAllPlayers;
		this.modifiable = modifiable;
        this.translationKey = Component.translatable(translationKey);
		this.id = SkillCategory.ENUM_MANAGER.assign(this);
		this.bookIcon = bookIcon;
	}

	@Override
	public boolean shouldSave() {
		return this.shouldSave;
	}

	@Override
	public boolean shouldSynchronize() {
		return this.shouldSyncronize;
	}

	@Override
	public boolean learnable() {
		return this.modifiable;
	}

	@Override
	public int universalOrdinal() {
		return this.id;
	}

    @Override
    public Component getTranslationKey() {
        return this.translationKey;
    }

	@Override
	public ResourceLocation bookIcon() {
		return this.bookIcon;
	}
}
