package alternate.current;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import alternate.current.util.profiler.ACProfiler;
import alternate.current.util.profiler.Profiler;

import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {

	public static final String MOD_ID = "alternate-current";
	public static final String MOD_NAME = "Alternate Current";
	public static final String MOD_VERSION = "1.2.1";
	public static final Logger LOGGER = LogManager.getLogManager().getLogger(MOD_NAME);
	public static final boolean DEBUG = false;

	public static boolean on = true;

	@Override
	public void onInitialize() {
		if (DEBUG) {
			LOGGER.warning(String.format("You are running a DEBUG version of %s!", MOD_NAME));
		}
	}

	public static Profiler createProfiler() {
		return DEBUG ? new ACProfiler() : Profiler.DUMMY;
	}
}
