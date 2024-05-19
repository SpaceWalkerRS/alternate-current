package alternate.current.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import alternate.current.AlternateCurrentMod;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.util.DimensionUtil;
import alternate.current.util.profiler.ProfilerResults;
import alternate.current.wire.UpdateOrder;
import alternate.current.wire.WireHandler;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.Command;
import net.minecraft.server.command.exception.CommandException;
import net.minecraft.server.command.exception.IncorrectUsageException;
import net.minecraft.server.command.source.CommandSource;
import net.minecraft.world.World;

public class AlternateCurrentCommand extends Command {

	@Override
	public String getName() {
		return "alternatecurrent";
	}

	@Override
	public String getUsage(CommandSource source) {
		return AlternateCurrentMod.DEBUG ? "/alternatecurrent [on/off/updateOrder[ updateOrder]/resetProfiler]" : "/alternatecurrent [on/off/updateOrder[ updateOrder]]";
	}

	@Override
	public void run(CommandSource source, String[] args) throws CommandException {
		switch (args.length) {
		case 0:
			queryEnabled(source);
			return;
		case 1:
			switch (args[0]) {
			case "on":
				setEnabled(source, true);
				return;
			case "off":
				setEnabled(source, false);
				return;
			case "updateOrder":
				queryUpdateOrder(source);
				return;
			case "resetProfiler":
				if (AlternateCurrentMod.DEBUG) {
					sendSuccess(source, "profiler results have been cleared!");

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
	public List<String> getSuggestions(CommandSource source, String[] args) {
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

	private static World getWorld(CommandSource source) {
		if (source instanceof MinecraftServer) {
			return ((MinecraftServer)source).getWorld(DimensionUtil.OVERWORLD);
		}
		if (source instanceof Entity) {
			return ((Entity)source).world;
		}
		return null;
	}


	private void queryEnabled(CommandSource source) {
		World world = getWorld(source);
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		String state = wireHandler.getConfig().getEnabled()? "enabled" : "disabled";
		source.sendMessage(String.format("Alternate Current is currently %s", state));
	}

	private void setEnabled(CommandSource source, boolean on) {
		World world = getWorld(source);
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		wireHandler.getConfig().setEnabled(on);

		String state = wireHandler.getConfig().getEnabled() ? "enabled" : "disabled";
		sendSuccess(source, String.format("Alternate Current has been %s!", state));
	}

	private void queryUpdateOrder(CommandSource source) {
		World world = getWorld(source);
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		String value = wireHandler.getConfig().getUpdateOrder().id();
		source.sendMessage(String.format("Update order is currently %s", value));
	}

	private void setUpdateOrder(CommandSource source, UpdateOrder updateOrder) {
		World world = getWorld(source);
		WireHandler wireHandler = ((IServerWorld) world).alternate_current$getWireHandler();

		wireHandler.getConfig().setUpdateOrder(updateOrder);

		String value = wireHandler.getConfig().getUpdateOrder().id();
		sendSuccess(source, String.format("update order has been set to %s!", value));
	}

	@Override
	public int compareTo(Object o) {
		return super.compareTo((Command) o);
	}
}
