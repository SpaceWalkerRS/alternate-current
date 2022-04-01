package alternate.current.wire;

import java.util.AbstractQueue;
import java.util.Iterator;

public class PowerQueue extends AbstractQueue<WireNode> {

	private WireNode head;
	private WireNode tail;

	/** The last wire for each power level. */
	private WireNode[] tails;

	private int offset;
	private int size;

	public PowerQueue() {
		clear();
	}

	@Override
	public boolean offer(WireNode wire) {
		if (wire == null) {
			throw new NullPointerException();
		}

		int power = wire.nextPower();

		if (contains(wire)) {
			if (power != wire.power) {
				move(wire, power);
			} else {
				return false;
			}
		} else {
			insert(wire, power);
		}

		return true;
	}

	@Override
	public WireNode poll() {
		if (head == null) {
			return null;
		}

		WireNode wire = head;
		head = head.next;

		if (head == null) {
			clear();
		} else {
			head.prev = null;

			if (wire.power != head.power) {
				tails[wire.power + offset] = null;
			}

			size--;
		}

		return wire;
	}

	@Override
	public WireNode peek() {
		return head;
	}

	@Override
	public void clear() {
		for (WireNode wire = head; wire != null; ) {
			WireNode w = wire;
			wire = wire.next;

			w.prev = null;
			w.next = null;
		}

		head = null;
		tail = null;

		tails = new WireNode[0];

		offset = 0;
		size = 0;
	}

	@Override
	public Iterator<WireNode> iterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return size;
	}

	public boolean contains(WireNode wire) {
		return wire.next != null || wire == tail;
	}

	private void move(WireNode wire, int power) {
		if (wire == tail || wire.power != wire.next.power) {
			if (wire == head || wire.prev.power != wire.power) {
				tails[wire.power + offset] = null;
			} else {
				tails[wire.power + offset] = wire.prev;
			}
		}

		size--;

		unlink(wire);
		insert(wire, power);
	}

	private void unlink(WireNode wire) {
		if (wire == head) {
			head = head.next;
		} else {
			wire.prev.next = wire.next;
		}
		if (wire == tail) {
			tail = tail.prev;
		} else {
			wire.next.prev = wire.prev;
		}

		wire.prev = null;
		wire.next = null;
	}

	private void insert(WireNode wire, int power) {
		wire.power = power;

		index(wire);
		link(wire);

		size++;

		int index = wire.power + offset;
		tails[index] = wire;
	}

	private void index(WireNode wire) {
		int size = wire.power + offset + 1;
		int move = -wire.type.minPower - offset;

		if (size < tails.length) {
			size = tails.length;
		}
		if (move < 0) {
			move = 0;
		}

		size += move;

		if (size > tails.length) {
			resize(size, move);
		}
	}

	private void resize(int size, int move) {
		WireNode[] array = tails;
		tails = new WireNode[size];

		for (int i = 0; i < array.length; i++) {
			tails[i + move] = array[i];
		}

		offset += move;
	}

	private void link(WireNode wire) {
		if (head == null) {
			linkFirst(wire);
		} else if (wire.power > head.power) {
			linkHead(wire);
		} else if (wire.power <= tail.power) {
			linkTail(wire);
		} else {
			int index = wire.power + offset;
			WireNode prev = tails[index];

			if (prev == null) {
				prev = findPrevWire(index);
			}

			linkAfter(prev, wire);
		}
	}

	private void linkFirst(WireNode wire) {
		head = tail = wire;
	}

	private void linkHead(WireNode wire) {
		wire.next = head;
		head.prev = wire;
		head = wire;
	}

	private void linkTail(WireNode wire) {
		tail.next = wire;
		wire.prev = tail;
		tail = wire;
	}

	private WireNode findPrevWire(int index) {
		for (int i = index + 1; i < tails.length; i++) {
			WireNode wire = tails[i];

			if (wire != null) {
				return wire;
			}
		}

		return head;
	}

	private void linkAfter(WireNode prev, WireNode wire) {
		prev.next = wire;
		wire.prev = prev;

		WireNode next = prev.next;

		if (next != null) {
			wire.next = next;
			next.prev = wire;
		}
	}
}
