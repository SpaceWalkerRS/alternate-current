package alternate.current.util.profiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.Logger;

import alternate.current.AlternateCurrentMod;

public class ProfilerResults {
	
	private static final Logger LOGGER = AlternateCurrentMod.LOGGER;
	
	private static final Map<String, Long> RESULTS = new LinkedHashMap<>();
	private static long totalTime;
	
	public static void clear() {
		RESULTS.clear();
		totalTime = 0;
	}
	
	public static void add(List<String> locations, List<Long> times) {
		long total = times.get(0);
		totalTime += total;
		
		for (int index = 1; index < locations.size(); index++) {
			String location = locations.get(index);
			long time = times.get(index);
			
			RESULTS.compute(location, (l, t) -> t == null ? time : t + time);
		}
	}
	
	public static void log() {
		LOGGER.info("------------------------------------------------------");
		LOGGER.info("..... Alternate Current Profiler Session Results .....");
		
		LOGGER.info("total: " + totalTime);
		
		for (Entry<String, Long> entry : RESULTS.entrySet()) {
			String loc = entry.getKey();
			long time = entry.getValue();
			
			LOGGER.info(String.format("%s: %d (~%d%%)", loc, time, (100 * time / totalTime)));
		}
	}
}
