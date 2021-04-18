package alternate.current.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IMinecraftServer;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("alternatecurrent").
			executes(context -> query(context.getSource())).
			then(CommandManager.
				literal("enable").
				requires(source -> source.hasPermissionLevel(2)).
				executes(context -> fastRedstone(context.getSource(), true))).
			then(CommandManager.
				literal("disable").
				requires(source -> source.hasPermissionLevel(2)).
				executes(context -> fastRedstone(context.getSource(), false)));
		
		dispatcher.register(builder);
	}
	
	private static int query(ServerCommandSource source) {
		source.sendFeedback(new LiteralText(String.format("Alternate Current is currently %s", AlternateCurrentMod.ENABLED ? "enabled" : "disabled")), false);
		
		return 1;
	}
	
	private static int fastRedstone(ServerCommandSource source, boolean enable) {
		if (enable == AlternateCurrentMod.ENABLED) {
			source.sendFeedback(new LiteralText(String.format("Alternate Current is already %s!", enable ? "enabled" : "disabled")), false);
		} else {
			AlternateCurrentMod.ENABLED = enable;
			
			if (!enable) {
				((IMinecraftServer)source.getMinecraftServer()).clearWires();
			}
			
			source.sendFeedback(new LiteralText(String.format("Alternate Current has been %s!", enable ? "enabled" : "disabled")), false);
		}
		
		return 1;
	}
}
