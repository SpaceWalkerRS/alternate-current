package fast.redstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fast.redstone.FastRedstoneMod;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class FastRedstoneCommand {
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		/*LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("fastredstone").
			requires(source -> source.hasPermissionLevel(2)).
			executes(context -> query(context.getSource())).
			then(CommandManager.
				literal("enable").
				executes(context -> fastRedstone(context.getSource(), true))).
			then(CommandManager.
				literal("disable").
				executes(context -> fastRedstone(context.getSource(), false)));*/
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("fastredstone").
			requires(source -> source.hasPermissionLevel(2)).
			then(CommandManager.
				argument("version", IntegerArgumentType.integer()).
				executes(context -> fastRedstone(context.getSource(), IntegerArgumentType.getInteger(context, "version"))));
		
		dispatcher.register(builder);
	}
	
	/*private static int query(ServerCommandSource source) {
		source.sendFeedback(new LiteralText(String.format("Fast Redstone is currently %s", FastRedstoneMod.ENABLED ? "enabled" : "disabled")), false);
		
		return 1;
	}
	
	private static int fastRedstone(ServerCommandSource source, boolean enable) {
		if (enable == FastRedstoneMod.ENABLED) {
			source.sendFeedback(new LiteralText(String.format("Fast Redstone is already %s!", enable ? "enabled" : "disabled")), false);
		} else {
			FastRedstoneMod.ENABLED = enable;
			
			source.sendFeedback(new LiteralText(String.format("Fast Redstone has been %s!", enable ? "enabled" : "disabled")), false);
		}
		
		return 1;
	}*/
	
	private static int fastRedstone(ServerCommandSource source, int version) {
		FastRedstoneMod.version = version;
		
		if (version < 0 || version > 2) {
			source.sendFeedback(new LiteralText("Disabled fast redstone"), false);
		} else {
			source.sendFeedback(new LiteralText("Enabled version " + version + " of fast redstone"), false);
		}
		
		return 1;
	}
}
