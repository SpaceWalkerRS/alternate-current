package fast.redstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fast.redstone.FastRedstoneMod;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.TranslatableText;

public class FastRedstoneCommand {
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("fastredstone").
			requires(source -> source.hasPermissionLevel(2)).
			executes(context -> query(context.getSource())).
			then(CommandManager.
				literal("enable").
				executes(context -> fastRedstone(context.getSource(), true))).
			then(CommandManager.
				literal("disable").
				executes(context -> fastRedstone(context.getSource(), false)));
		
		dispatcher.register(builder);
	}
	
	private static int query(ServerCommandSource source) {
		source.sendFeedback(new TranslatableText(String.format("Fast Redstone is currently %s", FastRedstoneMod.ENABLED ? "enabled" : "disabled")), false);
		
		return 1;
	}
	
	private static int fastRedstone(ServerCommandSource source, boolean enable) {
		if (enable == FastRedstoneMod.ENABLED) {
			source.sendFeedback(new TranslatableText(String.format("Fast Redstone is already %s!", enable ? "enabled" : "disabled")), false);
		} else {
			FastRedstoneMod.ENABLED = enable;
			
			source.sendFeedback(new TranslatableText(String.format("Fast Redstone has been %s!", enable ? "enabled" : "disabled")), false);
		}
		
		return 1;
	}
}
