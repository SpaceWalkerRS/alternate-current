package alternate.current.utils.profiler;

public class ACProfilerDummy implements Profiler {
	
	public static final Profiler INSTANCE = new ACProfilerDummy();
	
	private ACProfilerDummy() {
		
	}
	
	@Override
	public void start() {
		
	}
	
	@Override
	public void end() {
		
	}
	
	@Override
	public void push(String location) {
		
	}
	
	@Override
	public void pop() {
		
	}
	
	@Override
	public void swap(String location) {
		
	}
}
