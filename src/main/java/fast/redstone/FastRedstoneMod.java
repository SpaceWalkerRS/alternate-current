package fast.redstone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class FastRedstoneMod implements ModInitializer {
	
	public static final Logger LOGGER = LogManager.getLogger("Fast Redstone");
	
	public static boolean ENABLED = true;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Fast Redstone has been initialized!");
	}
}
