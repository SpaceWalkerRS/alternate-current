package alternate.current;

import java.util.logging.LogManager;
import java.util.logging.Logger;

import alternate.current.util.profiler.ACProfiler;
import alternate.current.util.profiler.Profiler;

public class AlternateCurrentMod {

	public static final String MOD_ID = "alternate-current";
	public static final String MOD_NAME = "Alternate Current";
	public static final String MOD_VERSION = "1.7.0";
	public static final Logger LOGGER = LogManager.getLogManager().getLogger(MOD_NAME);
	public static final boolean DEBUG = false;

	public static boolean on = true;

	public static Profiler createProfiler() {
		return DEBUG ? new ACProfiler() : Profiler.DUMMY;
	}
}
