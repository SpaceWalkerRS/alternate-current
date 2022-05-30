package alternate.current.wire;

import java.util.AbstractQueue;
import java.util.Iterator;

public class UpdateQueue extends AbstractQueue<Node> {

	private static final int DEFAULT_TAILS_CAPACITY = (WireTypes.REDSTONE.maxPower + 1) - WireTypes.REDSTONE.minPower;

	private Node head;
	private Node tail;

	/** The last node for each priority value. */
	private Node[] tails;

	private int offset;
	private int size;

	public UpdateQueue() {
		clear();
	}

	@Override
	public boolean offer(Node node) {
		if (node == null) {
			throw new NullPointerException();
		}

		int priority = node.priority();

		if (contains(node)) {
			if (node.priority == priority) {
				// already queued with this priority; exit
				return false;
			} else {
				// already queued with different priority; move it
				move(node, priority);
			}
		} else {
			insert(node, priority);
		}

		return true;
	}

	@Override
	public Node poll() {
		if (head == null) {
			return null;
		}

		Node node = head;
		Node next = node.next;

		if (next == null) {
			clear(); // reset the tails array and offset
		} else {
			if (node.priority != next.priority) {
				// If the head is also a tail, its entry in the array
				// can be cleared; there is no previous node with the
				// same priority to take its place.
				tails[node.priority + offset] = null;
			}

			node.next = null;
			next.prev = null;
			head = next;

			size--;
		}

		return node;
	}

	@Override
	public Node peek() {
		return head;
	}

	@Override
	public void clear() {
		for (Node node = head; node != null; ) {
			Node n = node;
			node = node.next;

			n.prev = null;
			n.next = null;
		}

		head = null;
		tail = null;

		tails = new Node[DEFAULT_TAILS_CAPACITY];

		offset = 0;
		size = 0;
	}

	@Override
	public Iterator<Node> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	public boolean contains(Node node) {
		return node == head || node.prev != null;
	}

	private void move(Node node, int priority) {
		remove(node);
		insert(node, priority);
	}

	private void remove(Node node) {
		Node prev = node.prev;
		Node next = node.next;

		if (node == tail || node.priority != next.priority) {
			// assign a new tail for this node's priority
			if (node == head || node.priority != prev.priority) {
				// there is no other node with the same priority; clear
				tails[node.priority + offset] = null;
			} else {
				// the previous node in the queue becomes the tail
				tails[node.priority + offset] = prev;
			}
		}

		if (node == head) {
			head = next;
		} else {
			prev.next = next;
		}
		if (node == tail) {
			tail = prev;
		} else {
			next.prev = prev;
		}

		node.prev = null;
		node.next = null;

		size--;
	}

	private void insert(Node node, int priority) {
		node.priority = priority;

		// update the tails array and offset if needed
		index(node);

		// nodes are sorted by priority (highest to lowest)
		// nodes with the same priority are ordered FIFO
		if (head == null) {
			// first element in this queue \o/
			head = tail = node;
		} else if (priority > head.priority) {
			linkHead(node);
		} else if (priority <= tail.priority) {
			linkTail(node);
		} else {
			// since the node is neither the head nor the tail
			// findPrev is guaranteed to find a non-null element
			linkAfter(findPrev(node), node);
		}

		tails[priority + offset] = node;

		size++;
	}

	private void index(Node node) {
		// required size is at least index + 1
		int size = (node.priority + offset) + 1;
		// move is new offset minus current offset
		int move = node.offset() - offset;

		// new size cannot be less than current size
		if (size < tails.length) {
			size = tails.length;
		}
		// since size cannot decrease, move cannot be negative
		if (move < 0) {
			move = 0;
		}

		// size increases by the increase in offset
		size += move;

		if (size > tails.length) {
			resize(size, move);
		}
	}

	private void resize(int size, int move) {
		Node[] array = tails;
		tails = new Node[size];

		for (int i = 0; i < array.length; i++) {
			tails[i + move] = array[i];
		}

		offset += move;
	}

	private void linkHead(Node node) {
		node.next = head;
		head.prev = node;
		head = node;
	}

	private void linkTail(Node node) {
		tail.next = node;
		node.prev = tail;
		tail = node;
	}

	private void linkAfter(Node prev, Node node) {
		linkBetween(prev, node, prev.next);
	}

	private void linkBetween(Node prev, Node node, Node next) {
		prev.next = node;
		node.prev = prev;

		node.next = next;
		next.prev = node;
	}

	private Node findPrev(Node node) {
		Node prev = null;

		for (int i = node.priority + offset; i < tails.length; i++) {
			prev = tails[i];

			if (prev != null) {
				break;
			}
		}

		return prev;
	}
}
