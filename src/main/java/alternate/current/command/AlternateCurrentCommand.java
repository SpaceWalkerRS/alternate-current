package alternate.current.command;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.profiler.ProfilerResults;

import net.minecraft.command.AbstractCommand;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.IncorrectUsageException;
import net.minecraft.text.LiteralText;

public class AlternateCurrentCommand extends AbstractCommand {

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
		return AlternateCurrentMod.DEBUG ? "/alternatecurrent [on/off/resetProfiler]" : "/alternatecurrent [on/off]";
	}

	@Override
	public void execute(CommandSource source, String[] args) throws CommandException {
		switch (args.length) {
		case 0:
			query(source);
			return;
		case 1:
			String arg = args[0];

			switch (arg) {
			case "on":
				set(source, true);
				return;
			case "off":
				set(source, false);
				return;
			case "resetProfiler":
				if (AlternateCurrentMod.DEBUG) {
					run(source, this, "profiler results have been cleared!");

					ProfilerResults.log();
					ProfilerResults.clear();

					return;
				}
			}

			break;
		}

		throw new IncorrectUsageException(getUsageTranslationKey(source));
	}

	private void query(CommandSource source) {
		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		source.sendMessage(new LiteralText(String.format("Alternate Current is currently %s", state)));
	}

	private void set(CommandSource source, boolean on) {
		AlternateCurrentMod.on = on;

		String state = AlternateCurrentMod.on ? "enabled" : "disabled";
		run(source, this, String.format("Alternate Current has been %s!", state));
	}
}
