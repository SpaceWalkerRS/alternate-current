package alternate.current.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.util.profiler.ProfilerResults;
import alternate.current.wire.UpdateOrder;
import alternate.current.wire.WireHandler;

import net.minecraft.server.command.Command;
import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.text.LiteralText;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class AlternateCurrentCommand extends Command {

	@Override
	public String getName() {
		return "alternatecurrent";
	}

	@Override
	public int getRequiredPermissionLevel() {
		return 2;
	}

	@Override
	public String getUsage(CommandSource source) {
		return AlternateCurrentMod.DEBUG ? "/alternatecurrent [on/off/updateOrder[ updateOrder]/resetProfiler]" : "/alternatecurrent [on/off/updateOrder[ updateOrder]]";
	}

	@Override
	public void run(CommandSource source, String[] args) throws CommandException {
		switch (args.length) {
		case 0:
			query(source);
			return;
		case 1:
			switch (args[0]) {
			case "on":
				set(source, true);
				return;
			case "off":
				set(source, false);
				return;
			case "updateOrder":
				queryUpdateOrder(source);
				return;
			case "resetProfiler":
				if (AlternateCurrentMod.DEBUG) {
					sendSuccess(source, this, "profiler results have been cleared!");

					ProfilerResults.log();
					ProfilerResults.clear();

					return;
				}
			}

			break;
		case 2:
			switch (args[0]) {
			case "updateOrder":
				for (UpdateOrder updateOrder : UpdateOrder.values()) {
					if (args[1].equals(updateOrder.id())) {
						setUpdateOrder(source, updateOrder);
						return;
					}
				}
			}
		}

		throw new IncorrectUsageException(getUsage(source));
	}

	@Override
	public List<String> getSuggestions(CommandSource source, String[] args, BlockPos pos) {
		switch (args.length) {
		case 0:
			return AlternateCurrentMod.DEBUG
				? suggestMatching(args, "on", "off", "updateOrder", "resetProfiler")
				: suggestMatching(args, "on", "off", "updateOrder");
		case 1:
			switch (args[0]) {
			case "updateOrder":
				List<String> updateOrders = new ArrayList<>();

				for (UpdateOrder updateOrder : UpdateOrder.values()) {
					updateOrders.add(updateOrder.id());
				}

				return suggestMatching(args, updateOrders);
			}
		}

		return Collections.emptyList();
	}

	private void query(CommandSource source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendMessage(new LiteralText(String.format("Alternate Current is currently %s", state)));
	}

	private void set(CommandSource source, boolean on) {
		AlternateCurrentMod.on = on;

		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		sendSuccess(source, this, String.format("Alternate Current has been %s!", state));
	}

	private void queryUpdateOrder(CommandSource source) {
		World world = source.getSourceWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		String value = wireHandler.getConfig().getUpdateOrder().id();
		source.sendMessage(new LiteralText(String.format("Update order is currently %s", value)));
	}

	private void setUpdateOrder(CommandSource source, UpdateOrder updateOrder) {
		World world = source.getSourceWorld();
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		wireHandler.getConfig().setUpdateOrder(updateOrder);

		String value = wireHandler.getConfig().getUpdateOrder().id();
		sendSuccess(source, this, String.format("update order has been set to %s!", value));
	}
}
