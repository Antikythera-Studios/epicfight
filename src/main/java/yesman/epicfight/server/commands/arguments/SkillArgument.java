package yesman.epicfight.server.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.registry.EpicFightRegistries;
import yesman.epicfight.skill.Skill;
import yesman.epicfight.skill.SkillCategory;
import yesman.epicfight.skill.SkillSlot;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SkillArgument implements ArgumentType<Holder<Skill>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("epicfight:dodge");
	private static final DynamicCommandExceptionType ERROR_UNKNOWN_SKILL = new DynamicCommandExceptionType((obj) -> {
		return Component.translatableEscape("epicfight.skillNotFound", obj);
	});

	private static final DynamicCommandExceptionType ERROR_INAPPROPRICATE_SKILL = new DynamicCommandExceptionType((obj) -> {
		return Component.translatableEscape("epicfight.invalid_skill", obj);
	});
	
	public static SkillArgument skill() {
		return new SkillArgument();
	}

	public Holder<Skill> parse(StringReader p_98428_) throws CommandSyntaxException {
		ResourceLocation resourcelocation = ResourceLocation.read(p_98428_);
		Optional<Holder.Reference<Skill>> skill = EpicFightRegistries.SKILL.getHolder(resourcelocation);
		
		if (skill.isEmpty()) {
			throw ERROR_UNKNOWN_SKILL.create(resourcelocation);
		}
		
		if (!skill.get().value().getCategory().learnable()) {
			throw ERROR_INAPPROPRICATE_SKILL.create(resourcelocation);
		}
		
		return skill.get();
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		final SkillCategory skillCategory = (commandContext.getNodes().size() > 5 && commandContext.getNodes().get(4).getNode() instanceof LiteralCommandNode<?> literalNode) ? nullParam(SkillSlot.ENUM_MANAGER.getOrThrow(literalNode.getLiteral())) : null;
		
		return SharedSuggestionProvider.suggestResource(
			EpicFightRegistries.SKILL.stream()
				.filter(skill -> skill.getCategory().learnable() && skill.getCategory().equals(skillCategory))
				.map(EpicFightRegistries.SKILL::getKey),
			suggestionsBuilder
		);
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
	
	private static SkillCategory nullParam(SkillSlot slot) {
		return slot == null ? null : slot.category();
	}
}