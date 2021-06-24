package alternate.current.utils.profiler;

public interface Profiler {
	
	public void start();
	
	public void end();
	
	public void push(String e);
	
	public void pop();
	
	public void swap(String e);
	
}
