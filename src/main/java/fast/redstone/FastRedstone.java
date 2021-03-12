package fast.redstone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class FastRedstone implements ModInitializer {
	
	public static final Logger LOGGER = LogManager.getLogger("Fast Redstone");
	
	public static boolean ACTIVE;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Fast Redstone has been initialized.");
	}
}
