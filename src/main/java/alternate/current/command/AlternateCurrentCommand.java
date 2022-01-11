package alternate.current.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;

public class AlternateCurrentCommand {
	
	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = Commands.
			literal("alternatecurrent").
			requires(source -> source.hasPermission(2)).
			executes(context -> query(context.getSource())).
			then(Commands.
				literal("on").
				executes(context -> toggle(context.getSource(), true))).
			then(Commands.
				literal("off").
				executes(context -> toggle(context.getSource(), false))).
			then(Commands.
				literal("resetProfiler").
				requires(source -> AlternateCurrentMod.DEBUG).
				executes(context -> resetProfiler(context.getSource())));
		
		dispatcher.register(builder);
	}
	
	private static int query(CommandSourceStack source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendSuccess(new TextComponent(String.format("Alternate Current is currently %s", state)), false);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int toggle(CommandSourceStack source, boolean on) {
		AlternateCurrentMod.on = on;
		
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendSuccess(new TextComponent(String.format("Alternate Current has been %s!", state)), true);
		
		return Command.SINGLE_SUCCESS;
	}
	
	private static int resetProfiler(CommandSourceStack source) {
		source.sendSuccess(new TextComponent("profiler results have been cleared!"), true);
		
		ProfilerResults.log();
		ProfilerResults.clear();
		
		return Command.SINGLE_SUCCESS;
	}
}
