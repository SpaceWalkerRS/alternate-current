package alternate.current.utils.profiler;

public interface Profiler {
	
	public void start();
	
	public void end();
	
	public void push(String location);
	
	public void pop();
	
	public void swap(String location);
	
}
