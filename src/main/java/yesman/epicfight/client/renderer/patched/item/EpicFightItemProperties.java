package yesman.epicfight.client.renderer.patched.item;

import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.EpicFight;
import yesman.epicfight.registry.entries.EpicFightItems;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.item.SkillBookItem;

public class EpicFightItemProperties {
	public static void registerItemProperties() {
		ItemProperties.register(EpicFightItems.SKILLBOOK.get(), EpicFight.identifier("skill"), (itemstack, level, entity, i) -> {
            Holder<Skill> skill = SkillBookItem.getContainSkill(itemstack).orElse(null);

            if (skill != null) {
                return skill.value().getCategory().universalOrdinal();
            }

            return Float.NEGATIVE_INFINITY;
        });
	}
}