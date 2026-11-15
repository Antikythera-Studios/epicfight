package yesman.epicfight.server.commands.arguments;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import yesman.epicfight.api.animation.AnimationManager;
import yesman.epicfight.api.animation.AnimationManager.AnimationAccessor;
import yesman.epicfight.api.animation.types.StaticAnimation;

public class AnimationArgument implements ArgumentType<AnimationAccessor<? extends StaticAnimation>> {
	private static final Collection<String> EXAMPLES = Arrays.asList("epicfight:biped_idle");
	
	private static final DynamicCommandExceptionType ERROR_UNKNOWN_SKILL = new DynamicCommandExceptionType((obj) -> {
		return Component.translatableEscape("epicfight.animationNotFound", obj);
	});
	
	public static AnimationArgument animation() {
		return new AnimationArgument();
	}
	
	@SuppressWarnings("unchecked")
	public static AnimationAccessor<? extends StaticAnimation> getAnimation(CommandContext<CommandSourceStack> commandContext, String name) {
		return commandContext.getArgument(name, AnimationAccessor.class);
	}
	
	public AnimationAccessor<? extends StaticAnimation> parse(StringReader p_98428_) throws CommandSyntaxException {
		ResourceLocation resourcelocation = ResourceLocation.read(p_98428_);
		AnimationAccessor<? extends StaticAnimation> animation = AnimationManager.byKey(resourcelocation);
		
		return Optional.ofNullable(animation).orElseThrow(() -> {
			return ERROR_UNKNOWN_SKILL.create(resourcelocation);
		});
	}
	
	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		return SharedSuggestionProvider.suggestResource(AnimationManager.getInstance().getAnimations((animation) -> animation.registryName() != null).entrySet().stream().map((e) -> e.getValue().registryName()), suggestionsBuilder);
	}
	
	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}