package alternate.current.command;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.command.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.CommandUsageException;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.LiteralText;

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
		return "/alternatecurrent [on/off]";
	}
	
	@Override
	public void execute(MinecraftServer server, CommandSource source, String[] args) throws CommandException {
		switch (args.length) {
		case 0:
			query(source);
			return;
		case 1:
			String arg = args[0];
			
			switch (arg) {
			case "on":
				toggle(source, true);
				return;
			case "off":
				toggle(source, false);
				return;
			case "resetProfiler":
				if (AlternateCurrentMod.DEBUG) {
					method_28710(source, this, "profiler results have been cleared!");
					
					ProfilerResults.log();
					ProfilerResults.clear();
					
					return;
				}
			}
			
			break;
		}
		
		throw new CommandUsageException(getUsage(source));
	}
	
	private void query(CommandSource source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendSystemMessage(new LiteralText(String.format("Alternate Current is currently %s", state)));
	}
	
	private void toggle(CommandSource source, boolean on) {
		AlternateCurrentMod.on = on;
		
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		method_28710(source, this, String.format("Alternate Current has been %s!", state));
	}
}
