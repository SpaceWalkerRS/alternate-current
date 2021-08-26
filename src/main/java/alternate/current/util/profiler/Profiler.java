package alternate.current.util.profiler;

public interface Profiler {
	
	public static final Profiler DUMMY = new Profiler() {
		@Override public void start() { }
		@Override public void end() { }
		@Override public void push(String location) { }
		@Override public void pop() { }
		@Override public void swap(String location) { }
	};
	
	public void start();
	
	public void end();
	
	public void push(String location);
	
	public void pop();
	
	public void swap(String location);
	
}
