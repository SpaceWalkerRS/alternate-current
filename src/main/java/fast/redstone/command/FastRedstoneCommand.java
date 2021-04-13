package fast.redstone.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import fast.redstone.FastRedstoneMod;
import fast.redstone.interfaces.mixin.IMinecraftServer;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.LiteralText;

public class FastRedstoneCommand {
	
	public static void registerCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
		LiteralArgumentBuilder<ServerCommandSource> builder = CommandManager.
			literal("fastredstone").
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
		source.sendFeedback(new LiteralText(String.format("Fast Redstone is currently %s", FastRedstoneMod.ENABLED ? "enabled" : "disabled")), false);
		
		return 1;
	}
	
	private static int fastRedstone(ServerCommandSource source, boolean enable) {
		if (enable == FastRedstoneMod.ENABLED) {
			source.sendFeedback(new LiteralText(String.format("Fast Redstone is already %s!", enable ? "enabled" : "disabled")), false);
		} else {
			FastRedstoneMod.ENABLED = enable;
			
			if (!enable) {
				((IMinecraftServer)source.getMinecraftServer()).clearWires();
			}
			
			source.sendFeedback(new LiteralText(String.format("Fast Redstone has been %s!", enable ? "enabled" : "disabled")), false);
		}
		
		return 1;
	}
}
