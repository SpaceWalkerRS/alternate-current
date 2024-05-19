package alternate.current.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.util.profiler.ProfilerResults;
import alternate.current.wire.UpdateOrder;
import alternate.current.wire.WireHandler;

import net.minecraft.command.SuggestionProvider;
import net.minecraft.server.command.handler.CommandManager;
import net.minecraft.server.command.source.CommandSourceStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand {

	private static final DynamicCommandExceptionType NO_SUCH_UPDATE_ORDER = new DynamicCommandExceptionType(value -> new LiteralText("no such update order: " + value));

	private static final String[] UPDATE_ORDERS;

	static {
		UpdateOrder[] updateOrders = UpdateOrder.values();
		UPDATE_ORDERS = new String[updateOrders.length];

		for (int i = 0; i < updateOrders.length; i++) {
			UPDATE_ORDERS[i] = updateOrders[i].id();
		}
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralArgumentBuilder<CommandSourceStack> builder = CommandManager.
			literal("alternatecurrent").
			requires(source -> source.hasPermissions(2)).
			executes(context -> queryEnabled(context.getSource())).
			then(CommandManager.
				literal("on").
				executes(context -> setEnabled(context.getSource(), true))).
			then(CommandManager.
				literal("off").
				executes(context -> setEnabled(context.getSource(), false))).
			then(CommandManager.
				literal("updateOrder").
				executes(context -> queryUpdateOrder(context.getSource())).
				then(CommandManager.
					argument("updateOrder", StringArgumentType.word()).
					suggests((context, suggestionBuilder) -> SuggestionProvider.suggestMatching(UPDATE_ORDERS, suggestionBuilder)).
					executes(context -> setUpdateOrder(context.getSource(), parseUpdateOrder(context, "updateOrder"))))).
			then(CommandManager.
				literal("resetProfiler").
				requires(source -> AlternateCurrentMod.DEBUG).
				executes(context -> resetProfiler(context.getSource())));

		dispatcher.register(builder);
	}

	private static UpdateOrder parseUpdateOrder(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		String value = StringArgumentType.getString(context, name);

		try {
			return UpdateOrder.byId(value);
		} catch (Exception e) {
			throw NO_SUCH_UPDATE_ORDER.create(name);
		}
	}

	private static int queryEnabled(CommandSourceStack source) {
		ServerWorld world = source.getWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		String state = wireHandler.getConfig().getEnabled()? "enabled" : "disabled";
		source.sendSuccess(new LiteralText(String.format("Alternate Current is currently %s", state)), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int setEnabled(CommandSourceStack source, boolean on) {
		ServerWorld world = source.getWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		wireHandler.getConfig().setEnabled(on);

		String state = wireHandler.getConfig().getEnabled() ? "enabled" : "disabled";
		source.sendSuccess(new LiteralText(String.format("Alternate Current has been %s!", state)), true);

		return Command.SINGLE_SUCCESS;
	}

	private static int queryUpdateOrder(CommandSourceStack source) {
		ServerWorld world = source.getWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		String value = wireHandler.getConfig().getUpdateOrder().id();
		source.sendSuccess(new LiteralText(String.format("Update order is currently %s", value)), false);

		return Command.SINGLE_SUCCESS;
	}

	private static int setUpdateOrder(CommandSourceStack source, UpdateOrder updateOrder) {
		ServerWorld world = source.getWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		wireHandler.getConfig().setUpdateOrder(updateOrder);

		String value = wireHandler.getConfig().getUpdateOrder().id();
		source.sendSuccess(new LiteralText(String.format("update order has been set to %s!", value)), true);

		return Command.SINGLE_SUCCESS;
	}

	private static int resetProfiler(CommandSourceStack source) {
		source.sendSuccess(new LiteralText("profiler results have been cleared!"), true);

		ProfilerResults.log();
		ProfilerResults.clear();

		return Command.SINGLE_SUCCESS;
	}
}
