package alternate.current;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import alternate.current.utils.profiler.ACProfiler;
import alternate.current.utils.profiler.ACProfilerDummy;
import alternate.current.utils.profiler.Profiler;

import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final String MOD_ID = "alternate-current";
	public static final String VERSION = "0.1.4";
	public static final Logger LOGGER = LogManager.getLogger("Alternate Current");
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
		return DEBUG ? new ACProfiler() : ACProfilerDummy.INSTANCE;
	}
}
