package alternate.current.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.PerformanceMode;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IMinecraftServer;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {
	
	private static final String[] MODES;
	
	static {
		
		PerformanceMode[] modes = PerformanceMode.values();
		MODES = new String[modes.length];
		
		for (int index = 0; index < modes.length; index++) {
			MODES[index] = modes[index].toString();
		}
	}
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("alternatecurrent").
			executes(context -> queryMode(context.getSource())).
			then(CommandManager.
				argument("mode", StringArgumentType.word()).
				requires(source -> source.hasPermissionLevel(2)).
				suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(MODES, suggestionsBuilder)).
				executes(context -> setMode(context.getSource(), PerformanceMode.valueOf(StringArgumentType.getString(context, "mode"))))).
			then(CommandManager.
				literal("reset").
				requires(source -> AlternateCurrentMod.DEBUG && source.hasPermissionLevel(2)).
				executes(context -> resetProfiler(context.getSource())));
		
		dispatcher.register(builder);
	}
	
	private static int queryMode(ServerCommandSource source) {
		String message;
		
		if (AlternateCurrentMod.MODE == PerformanceMode.DISABLED) {
			message = "Alternate Current is currently disabled.";
		} else {
			message = String.format("Alternate Current is running in %s mode.", AlternateCurrentMod.MODE.toString());
		}
		
		source.sendFeedback(new LiteralText(message), false);
		
		return 1;
	}
	
	private static int setMode(ServerCommandSource source, PerformanceMode mode) {
		AlternateCurrentMod.MODE = mode;
		((IMinecraftServer)source.getServer()).clearWires();
		
		String message;
		
		if (mode == PerformanceMode.DISABLED) {
			message = "Alternate Current has been disabled.";
		} else {
			message = String.format("Alternate Current will now run in %s mode.", mode.toString());
		}
		
		source.sendFeedback(new LiteralText(message), true);
		
		return 1;
	}
	
	private static int resetProfiler(ServerCommandSource source) {
		source.sendFeedback(new LiteralText("profiler results have been cleared!"), true);
		ProfilerResults.clear();
		
		return 1;
	}
}
