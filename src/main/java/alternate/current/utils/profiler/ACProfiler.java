package alternate.current.utils.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.logging.log4j.Logger;

import alternate.current.AlternateCurrentMod;

public class ACProfiler implements Profiler {
	
	public static final Logger LOGGER = AlternateCurrentMod.LOGGER;
	
	private final Stack<Integer> indexStack;
	private final List<String> locations;
	private final List<Long> times;
	
	private boolean started;
	
	public ACProfiler() {
		this.indexStack = new Stack<>();
		this.locations = new ArrayList<>();
		this.times = new ArrayList<>();
	}
	
	@Override
	public void start() {
		if (started) {
			LOGGER.warn("profiling already started!");
		} else {
			indexStack.clear();
			locations.clear();
			times.clear();
			started = true;
			
			push("total");
		}
	}
	
	@Override
	public void end() {
		if (started) {
			pop();
			started = false;
			
			if (!indexStack.isEmpty()) {
				LOGGER.warn("profiling ended before stack was fully popped, did something go wrong?");
			}
			
			logResults();
		} else {
			LOGGER.warn("profiling already ended!");
		}
	}
	
	@Override
	public void push(String location) {
		if (started) {
			indexStack.add(times.size());
			locations.add(location);
			times.add(System.nanoTime());
		} else {
			LOGGER.error("cannot push " + location + " as profiling hasn't started!");
		}
	}
	
	@Override
	public void pop() {
		if (started) {
			Integer index = indexStack.pop();
			
			if (index == null) {
				LOGGER.error("no element to pop!");
			} else {
				long startTime = times.get(index);
				long endTime = System.nanoTime();
				times.set(index, endTime - startTime);
			}
		} else {
			LOGGER.error("cannot pop as profiling hasn't started!");
		}
	}
	
	@Override
	public void swap(String location) {
		pop();
		push(location);
	}
	
	private void logResults() {
		LOGGER.info("--- Alternate Current Profiler Results ---");
		
		long total = times.get(0);
		LOGGER.info("total: " + total);
		
		for (int index = 1; index < times.size(); index++) {
			String loc = locations.get(index);
			long time = times.get(index);
			
			LOGGER.info(String.format("%s: %d (~%d%%)", loc, time, (100 * time / total)));
		}
	}
}
