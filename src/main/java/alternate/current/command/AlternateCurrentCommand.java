package alternate.current.command;

import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.command.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandUsageException;
import net.minecraft.server.MinecraftServer;

public class AlternateCurrentCommand extends AbstractCommand {
	
	@Override
	public String getName() {
		return "alternatecurrent";
	}
	
	@Override
	public int getRequiredLevel() {
		return 2;
	}
	
	@Override
	public String getUsage(CommandSource source) {
		return "/alternatecurrent resetProfiler";
	}
	
	@Override
	public void execute(MinecraftServer server, CommandSource source, String[] args) throws CommandException {
		if (args.length == 1 && args[0].equals("resetProfiler")) {
			method_28710(source, this, "profiler results have been cleared!");
			
			ProfilerResults.log();
			ProfilerResults.clear();
			
			return;
		}
		
		throw new CommandUsageException(getUsage(source));
	}
}
