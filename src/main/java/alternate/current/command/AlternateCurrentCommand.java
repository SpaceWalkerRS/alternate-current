package alternate.current.command;

import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.class_2662;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;

public class AlternateCurrentCommand extends class_2662 {
	
	@Override
	public String getCommandName() {
		return "alternatecurrent";
	}
	
	@Override
	public int getPermissionLevel() {
		return 2;
	}
	
	@Override
	public String getUsageTranslationKey(CommandSource source) {
		return "/alternatecurrent resetProfiler";
	}
	
	@Override
	public void execute(CommandSource source, String[] args) throws CommandException {
		if (args.length == 1 && args[0].equals("resetProfiler")) {
			method_10772(source, this, "profiler results have been cleared!");
			
			ProfilerResults.log();
			ProfilerResults.clear();
			
			return;
		}
		
		throw new IncorrectUsageException(getUsageTranslationKey(source));
	}
	
	@Override
	public int compareTo(Object obj) {
		return 0;
	}
}
