package alternate.current.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("alternatecurrent").
			requires(source -> source.hasPermissionLevel(2)).
			executes(context -> query(context.getSource())).
			then(CommandManager.
				literal("on").
				executes(context -> toggle(context.getSource(), true))).
			then(CommandManager.
				literal("off").
				executes(context -> toggle(context.getSource(), false))).
			then(CommandManager.
				literal("resetProfiler").
				requires(source -> AlternateCurrentMod.DEBUG).
				executes(context -> resetProfiler(context.getSource())));
		
		dispatcher.register(builder);
	}
	
	private static int query(ServerCommandSource source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendFeedback(new LiteralText(String.format("Alternate Current is currently %s", state)), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int toggle(ServerCommandSource source, boolean on) {
		AlternateCurrentMod.on = on;
		
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendFeedback(new LiteralText(String.format("Alternate Current has been %s!", state)), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int resetProfiler(ServerCommandSource source) {
		source.sendFeedback(new LiteralText("profiler results have been cleared!"), true);
		
		ProfilerResults.log();
		ProfilerResults.clear();
		
		return Command.SINGLE_SUCCESS;
	}
}
