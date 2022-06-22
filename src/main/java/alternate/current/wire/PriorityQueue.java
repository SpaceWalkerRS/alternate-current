package alternate.current.wire;

import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Iterator;

import net.minecraft.world.level.redstone.Redstone;

public class PriorityQueue extends AbstractQueue<Node> {

	private static final int OFFSET = -Redstone.SIGNAL_MIN;

	/** The last node for each priority value. */
	private final Node[] tails;

	private Node head;
	private Node tail;

	private int size;

	PriorityQueue() {
		this.tails = new Node[(Redstone.SIGNAL_MAX + OFFSET) + 1];
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
		Node next = node.next_node;

		if (next == null) {
			clear(); // reset the tails array
		} else {
			if (node.priority != next.priority) {
				// If the head is also a tail, its entry in the array
				// can be cleared; there is no previous node with the
				// same priority to take its place.
				tails[node.priority + OFFSET] = null;
			}

			node.next_node = null;
			next.prev_node = null;
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
			node = node.next_node;

			n.prev_node = null;
			n.next_node = null;
		}

		Arrays.fill(tails, null);

		head = null;
		tail = null;

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
		return node == head || node.prev_node != null;
	}

	private void move(Node node, int priority) {
		remove(node);
		insert(node, priority);
	}

	private void remove(Node node) {
		Node prev = node.prev_node;
		Node next = node.next_node;

		if (node == tail || node.priority != next.priority) {
			// assign a new tail for this node's priority
			if (node == head || node.priority != prev.priority) {
				// there is no other node with the same priority; clear
				tails[node.priority + OFFSET] = null;
			} else {
				// the previous node in the queue becomes the tail
				tails[node.priority + OFFSET] = prev;
			}
		}

		if (node == head) {
			head = next;
		} else {
			prev.next_node = next;
		}
		if (node == tail) {
			tail = prev;
		} else {
			next.prev_node = prev;
		}

		node.prev_node = null;
		node.next_node = null;

		size--;
	}

	private void insert(Node node, int priority) {
		node.priority = priority;

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

		tails[priority + OFFSET] = node;

		size++;
	}

	private void linkHead(Node node) {
		node.next_node = head;
		head.prev_node = node;
		head = node;
	}

	private void linkTail(Node node) {
		tail.next_node = node;
		node.prev_node = tail;
		tail = node;
	}

	private void linkAfter(Node prev, Node node) {
		linkBetween(prev, node, prev.next_node);
	}

	private void linkBetween(Node prev, Node node, Node next) {
		prev.next_node = node;
		node.prev_node = prev;

		node.next_node = next;
		next.prev_node = node;
	}

	private Node findPrev(Node node) {
		Node prev = null;

		for (int i = node.priority + OFFSET; i < tails.length; i++) {
			prev = tails[i];

			if (prev != null) {
				break;
			}
		}

		return prev;
	}
}
