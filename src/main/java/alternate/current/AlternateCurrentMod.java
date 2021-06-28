package alternate.current;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final String MOD_ID = "alternate-current";
	public static final String VERSION = "0.1.4";
	public static final Logger LOGGER = LogManager.getLogger("Alternate Current");
	
	public static PerformanceMode MODE = PerformanceMode.MAX_PERFORMANCE;
	
	@Override
	public void onInitialize() {
		LOGGER.info(String.format("Alternate Current %s has been initialized!", VERSION));
	}
}
