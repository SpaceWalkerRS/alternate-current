package alternate.current.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.PerformanceMode;
import alternate.current.interfaces.mixin.IMinecraftServer;

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
			requires(source -> source.hasPermissionLevel(2)).
			executes(context -> queryMode(context.getSource())).
			then(CommandManager.
				argument("mode", StringArgumentType.word()).
				suggests((context, suggestionsBuilder) -> CommandSource.suggestMatching(MODES, suggestionsBuilder)).
				executes(context -> setMode(context.getSource(), PerformanceMode.valueOf(StringArgumentType.getString(context, "mode")))));
		
		dispatcher.register(builder);
	}
	
	private static int queryMode(ServerCommandSource source) {
		String message;
		
		switch (AlternateCurrentMod.MODE) {
		case MAX_PERFORMANCE:
			message = "Alternate Current is running in MAX_PERFORMANCE mode.";
			break;
		default:
			message = "Alternate Current is currently disabled.";
			break;
		}
		
		source.sendFeedback(new LiteralText(message), false);
		
		return 1;
	}
	
	private static int setMode(ServerCommandSource source, PerformanceMode mode) {
		AlternateCurrentMod.MODE = mode;
		
		((IMinecraftServer)source.getMinecraftServer()).clearWires();
		
		if (mode == PerformanceMode.VANILLA) {
			source.sendFeedback(new LiteralText("Alternate Current has been disabled."), false);
		} else {
			source.sendFeedback(new LiteralText(String.format("Alternate Current will now run in %s mode.", mode.toString())), false);
		}
		
		return 1;
	}
}
