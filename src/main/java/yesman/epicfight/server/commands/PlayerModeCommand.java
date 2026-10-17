package yesman.epicfight.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import yesman.epicfight.world.capabilities.EpicFightCapabilities;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch;
import yesman.epicfight.world.capabilities.entitypatch.player.PlayerPatch.PlayerMode;
import yesman.epicfight.world.capabilities.entitypatch.player.ServerPlayerPatch;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;

public class PlayerModeCommand {
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.literal("mode").requires((commandSourceStack) -> commandSourceStack.hasPermission(2));
		
		for (PlayerMode mode : PlayerPatch.PlayerMode.values()) {
			builder.then(Commands.literal(mode.name().toLowerCase(Locale.ROOT)).executes((command) -> {
				return setMode(command, Collections.singleton(command.getSource().getPlayerOrException()), mode);
			}).then(Commands.argument("target", EntityArgument.players()).executes((p_137728_) -> {
	            return setMode(p_137728_, EntityArgument.getPlayers(p_137728_, "target"), mode);
	        })));
		}
		
		dispatcher.register(Commands.literal("epicfight").then(builder));
	}
	
	private static int setMode(CommandContext<CommandSourceStack> command, Collection<ServerPlayer> players, PlayerPatch.PlayerMode playerMode) {
		int i = 0;
		
		for (ServerPlayer serverplayer : players) {
			ServerPlayerPatch playerpatch = EpicFightCapabilities.getEntityPatch(serverplayer, ServerPlayerPatch.class);
			
			if (playerpatch != null) {
				logGamemodeChange(command.getSource(), serverplayer, playerMode);
				playerpatch.toMode(playerMode, true);
				++i;
			}
		}
		
		return i;
	}
	
	private static void logGamemodeChange(CommandSourceStack command, ServerPlayer serverPlayer, PlayerPatch.PlayerMode playerMode) {
		Component component = Component.translatable("gameMode.epicfight." + playerMode.name().toLowerCase(Locale.ROOT));
		
		if (command.getEntity() == serverPlayer) {
			command.sendSuccess(() -> Component.translatable("commands.gamemode.success.self", component), true);
		} else {
			if (command.getLevel().getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
				serverPlayer.sendSystemMessage(Component.translatable("gameMode.changed", component));
			}
			command.sendSuccess(() -> Component.translatable("commands.gamemode.success.other", serverPlayer.getDisplayName(), component), true);
		}
	}
}