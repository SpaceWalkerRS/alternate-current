package alternate.current.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("alternatecurrent").
			requires(source -> source.hasPermissionLevel(2)).
			then(CommandManager.
				literal("resetProfiler").
				executes(context -> resetProfiler(context.getSource())));
		
		dispatcher.register(builder);
	}
	
	private static int resetProfiler(ServerCommandSource source) {
		source.sendFeedback(new LiteralText("profiler results have been cleared!"), true);
		
		ProfilerResults.log();
		ProfilerResults.clear();
		
		return 1;
	}
}
