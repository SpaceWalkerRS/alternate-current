package alternate.current;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import alternate.current.util.profiler.ACProfiler;
import alternate.current.util.profiler.Profiler;
import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final String MOD_ID = "alternatecurrent";
	public static final String MOD_NAME = "Alternate Current";
	public static final String VERSION = "0.3.0";
	public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
	public static final boolean DEBUG = false;
	
	public static PerformanceMode MODE = PerformanceMode.MAX_PERFORMANCE;
	
	@Override
	public void onInitialize() {
		LOGGER.info(String.format("Alternate Current %s has been initialized!", VERSION));
		
		if (DEBUG) {
			LOGGER.warn("You are running a DEBUG version of Alternate Current!");
		}
	}
	
	public static Profiler createProfiler() {
		return DEBUG ? new ACProfiler() : Profiler.DUMMY;
	}
}
