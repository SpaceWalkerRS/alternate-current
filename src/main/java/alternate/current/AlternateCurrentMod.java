package alternate.current;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class AlternateCurrentMod implements ModInitializer {
	
	public static final Logger LOGGER = LogManager.getLogger("Alternate Current");
	
	public static boolean ENABLED = true;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Alternate Current has been initialized!");
	}
}
