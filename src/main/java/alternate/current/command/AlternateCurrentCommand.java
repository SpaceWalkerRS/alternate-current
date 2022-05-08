package alternate.current.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.class_3915;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {

	public static void register(CommandDispatcher<class_3915> dispatcher) {
		LiteralArgumentBuilder<class_3915> builder = CommandManager.
			method_17529("alternatecurrent").
			requires(source -> source.method_17575(2)).
			executes(context -> query(context.getSource())).
			then(CommandManager.
				method_17529("on").
				executes(context -> set(context.getSource(), true))).
			then(CommandManager.
				method_17529("off").
				executes(context -> set(context.getSource(), false))).
			then(CommandManager.
				method_17529("resetProfiler").
				requires(source -> AlternateCurrentMod.DEBUG).
				executes(context -> resetProfiler(context.getSource())));

		dispatcher.register(builder);
	}

	private static int query(class_3915 source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.method_17459(new LiteralText(String.format("Alternate Current is currently %s", state)), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int set(class_3915 source, boolean on) {
		AlternateCurrentMod.on = on;

		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.method_17459(new LiteralText(String.format("Alternate Current has been %s!", state)), true);

		return Command.SINGLE_SUCCESS;
	}

	private static int resetProfiler(class_3915 source) {
		source.method_17459(new LiteralText("profiler results have been cleared!"), true);

		ProfilerResults.log();
		ProfilerResults.clear();

		return Command.SINGLE_SUCCESS;
	}
}
