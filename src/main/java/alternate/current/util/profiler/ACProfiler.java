package alternate.current.util.profiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.logging.log4j.Logger;

import alternate.current.AlternateCurrentMod;

public class ACProfiler implements Profiler {
	
	private static final Logger LOGGER = AlternateCurrentMod.LOGGER;
	
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
			
			ProfilerResults.add(locations, times);
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
}
