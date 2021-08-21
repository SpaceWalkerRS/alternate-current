package alternate.current.MAX_PERFORMANCE;

import java.util.AbstractQueue;
import java.util.Iterator;
import java.util.Queue;

import alternate.current.util.collection.SimpleQueue;

public class PowerQueue extends AbstractQueue<WireNode> {
	
	private final int minPower;
	private final int maxPower;
	private final Queue<WireNode>[] queues;
	
	private int size;
	private int currentQueue;
	
	public PowerQueue(int minPower, int maxPower) {
		this.minPower = minPower;
		this.maxPower = maxPower;
		this.queues = createQueues(this.maxPower - this.minPower + 1);
	}
	
	private static Queue<WireNode>[] createQueues(int queueCount) {
		@SuppressWarnings("unchecked")
		Queue<WireNode>[] queues = new Queue[queueCount];
		
		for (int index = 0; index < queueCount; index++) {
			queues[index] = new SimpleQueue<>();
		}
		
		return queues;
	}
	
	@Override
	public boolean offer(WireNode wire) {
		int queueIndex = wire.nextPower() - minPower;
		queues[queueIndex].offer(wire);
		size++;
		
		if (queueIndex > currentQueue) {
			currentQueue = queueIndex;
		}
		
		return true;
	}
	
	@Override
	public WireNode poll() {
		if (size == 0) {
			return null;
		}
		
		WireNode wire;
		
		do {
			wire = queues[currentQueue].poll();
		} while (wire == null && currentQueue-- > 0);
		
		if (wire != null) {
			size--;
		}
		
		return wire;
	}
	
	@Override
	public WireNode peek() {
		if (size == 0) {
			return null;
		}
		
		WireNode wire;
		
		do {
			wire = queues[currentQueue].peek();
		} while (wire == null && currentQueue-- > 0);
		
		return wire;
	}
	
	@Override
	public Iterator<WireNode> iterator() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int size() {
		return size;
	}
}
