package fast.redstone;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.fabricmc.api.ModInitializer;

public class FastRedstoneMod implements ModInitializer {
	
	public static final Logger LOGGER = LogManager.getLogger("Fast Redstone");
	public static final int MAIN = 0;
	public static final int V1 = 1;
	public static final int NEW = 2;
	
	//public static boolean ENABLED;
	
	public static int version = -1;
	
	@Override
	public void onInitialize() {
		LOGGER.info("Fast Redstone has been initialized!");
	}
}
