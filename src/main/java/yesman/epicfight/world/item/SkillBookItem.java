package yesman.epicfight.world.item;

import java.util.List;
import java.util.Optional;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import yesman.epicfight.registry.entries.EpicFightDataComponentTypes;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;

public class SkillBookItem extends Item {
	public static void setContainingSkill(Holder<Skill> skill, ItemStack stack) {
		// DeferredHolderShim isn't a registry Reference — resolve the canonical
		// holder from the skill registry so the component survives NBT save.
		skill = skill.unwrapKey()
				.flatMap(yesman.epicfight.registry.EpicFightRegistries.SKILL::getHolder)
				.map(reference -> (Holder<Skill>) reference)
				.orElse(skill);
		stack.applyComponents(DataComponentPatch.builder().set(EpicFightDataComponentTypes.SKILL.get(), skill).build());
	}
	
	public static Optional<Holder<Skill>> getContainSkill(ItemStack stack) {
		return Optional.ofNullable(stack.get(EpicFightDataComponentTypes.SKILL.get()));
	}
	
	public SkillBookItem(Properties properties) {
		super(properties);
	}
	
	@Override
	public boolean isFoil(ItemStack stack) {
		return stack.has(EpicFightDataComponentTypes.SKILL.get());
	}
	
	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (stack.has(EpicFightDataComponentTypes.SKILL.get())) {
			Holder<Skill> skill = stack.get(EpicFightDataComponentTypes.SKILL.get());
			ResourceLocation registryName = skill.unwrapKey().get().location();
			tooltipComponents.add(Component.translatable(String.format("skill.%s.%s", registryName.getNamespace(), registryName.getPath())).withStyle(ChatFormatting.DARK_GRAY));
		}
	}
	
	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack itemstack = player.getItemInHand(hand);
		
		EpicFightCapabilities.getUnparameterizedEntityPatch(player, PlayerPatch.class).ifPresent(playerpatch -> {
			playerpatch.openSkillBook(itemstack, hand);
		});
		
		player.awardStat(Stats.ITEM_USED.get(this));
		
		return InteractionResultHolder.pass(itemstack);
	}
}