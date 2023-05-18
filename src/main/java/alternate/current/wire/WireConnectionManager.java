package alternate.current.wire;

import alternate.current.wire.WireHandler.Directions;
import alternate.current.wire.WireHandler.NodeProvider;

import java.util.Arrays;
import java.util.function.Consumer;

public class WireConnectionManager {

	/** The owner of these connections. */
	final WireNode owner;

	/** The first connection for each cardinal direction. */
	private final WireConnection[] heads;

	private WireConnection head;
	private WireConnection tail;

	/** The total number of connections. */
	int total;

	/**
	 * A 4 bit number that encodes which in direction(s) the owner has connections
	 * to other wires.
	 */
	private int flowTotal;
	/** The direction of flow based connections to other wires. */
	int iFlowDir;

	WireConnectionManager(WireNode owner) {
		this.owner = owner;

		this.heads = new WireConnection[Directions.HORIZONTAL.length];

		this.total = 0;

		this.flowTotal = 0;
		this.iFlowDir = -1;
	}

	void set(NodeProvider nodes) {
		if (total > 0) {
			clear();
		}

		boolean belowIsConductor = nodes.getNeighbor(owner, Directions.DOWN).isConductor();
		boolean aboveIsConductor = nodes.getNeighbor(owner, Directions.UP).isConductor();

		for (int iDir = 0; iDir < Directions.HORIZONTAL.length; iDir++) {
			Node neighbor = nodes.getNeighbor(owner, iDir);

			if (neighbor.isWire()) {
				add(neighbor.asWire(), iDir, true, true);

				continue;
			}

			boolean sideIsConductor = neighbor.isConductor();

			if (!sideIsConductor) {
				Node node = nodes.getNeighbor(neighbor, Directions.DOWN);

				if (node.isWire()) {
					add(node.asWire(), iDir, belowIsConductor, true);
				}
			}
			if (!aboveIsConductor) {
				Node node = nodes.getNeighbor(neighbor, Directions.UP);

				if (node.isWire()) {
					add(node.asWire(), iDir, true, sideIsConductor);
				}
			}
		}

		if (total > 0) {
			iFlowDir = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
		}
	}

	private void clear() {
		Arrays.fill(heads, null);

		head = null;
		tail = null;

		total = 0;

		flowTotal = 0;
		iFlowDir = -1;
	}

	private void add(WireNode wire, int iDir, boolean offer, boolean accept) {
		add(new WireConnection(wire, iDir, offer, accept));
	}

	private void add(WireConnection connection) {
		if (head == null) {
			head = connection;
			tail = connection;
		} else {
			tail.next = connection;
			tail = connection;
		}

		total++;

		if (heads[connection.iDir] == null) {
			heads[connection.iDir] = connection;
			flowTotal |= (1 << connection.iDir);
		}
	}

	/**
	 * Iterate over all connections. Use this method if the iteration order is not
	 * important.
	 */
	void forEach(Consumer<WireConnection> consumer) {
		for (WireConnection c = head; c != null; c = c.next) {
			consumer.accept(c);
		}
	}

	/**
	 * Iterate over all connections. Use this method if the iteration order is
	 * important.
	 */
	void forEach(Consumer<WireConnection> consumer, int iFlowDir) {
		for (int iDir : WireHandler.CARDINAL_UPDATE_ORDERS[iFlowDir]) {
			for (WireConnection c = heads[iDir]; c != null && c.iDir == iDir; c = c.next) {
				consumer.accept(c);
			}
		}
	}
}
