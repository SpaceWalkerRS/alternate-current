package alternate.current.util.collection;

import java.util.AbstractQueue;
import java.util.Iterator;

public class SimpleQueue<E> extends AbstractQueue<E> {
	
	private static final int MINIMUM_CAPACITY = 16;
	
	private Object[] queue;
	private int size;
	private int head;
	private int tail;
	
	public SimpleQueue() {
		this(MINIMUM_CAPACITY);
	}
	
	public SimpleQueue(int initialCapacity) {
		if (initialCapacity <= 0) {
			throw new IllegalArgumentException("The value of initialCapacity must be greater than 0!");
		}
		
		this.queue = new Object[initialCapacity];
	}
	
	@Override
	public boolean offer(E e) {
		if (e == null) {
            throw new NullPointerException();
		}
		
		if (size == queue.length) {
			resize(size << 1);
		}
		
		queue[tail] = e;
		tail = incr(tail);
		size++;
		
		return true;
	}
	
	@Override
	public E poll() {
		@SuppressWarnings("unchecked")
		E e = (E)queue[head];
		
		if (e != null) {
			queue[head] = null;
			head = incr(head);
			size--;
			
			if (queue.length > MINIMUM_CAPACITY && size == (queue.length >> 2)) {
				resize(size << 1);
			}
		}
		
		return e;
	}
	
	@Override
	public E peek() {
		@SuppressWarnings("unchecked")
		E e = (E)queue[head];
		return e;
	}
	
	@Override
	public Iterator<E> iterator() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public int size() {
		return size;
	}
	
	private void resize(int newSize) {
		Object[] oldQueue = queue;
		queue = new Object[newSize];
		
		int i = 0;
		
		if (head < tail) {
			for (int j = head; j < tail; ) {
				queue[i++] = oldQueue[j++];
			}
		} else {
			for (int j = head; j < oldQueue.length; ) {
				queue[i++] = oldQueue[j++];
			}
			for (int j = 0; j < tail; ) {
				queue[i++] = oldQueue[j++];
			}
		}
		
		head = 0;
		tail = size;
	}
	
	private int incr(int i) {
		return ++i < queue.length ? i : 0;
	}
}
