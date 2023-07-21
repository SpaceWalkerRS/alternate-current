package alternate.current.wire;

import java.util.Arrays;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.wire.WireHandler.Directions;

import net.minecraft.block.Block;
import net.minecraft.server.world.ServerWorld;

/**
 * A Node represents a block in the world. It also holds a few other pieces of
 * information that speed up the calculations in the WireHandler class.
 * 
 * @author Space Walker
 */
public class Node {

	// flags that encode the Node type
	private static final int CONDUCTOR = 0b01;
	private static final int SOURCE    = 0b10;

	final ServerWorld world;
	final Node[] neighbors;

	BlockPos pos;
	BlockState state;
	boolean invalid;

	private int flags;

	/** The previous node in the priority queue. */
	Node prev_node;
	/** The next node in the priority queue. */
	Node next_node;
	/** The priority with which this node was queued. */
	int priority;
	/** The wire that queued this node for an update. */
	WireNode neighborWire;

	Node(ServerWorld world) {
		this.world = world;
		this.neighbors = new Node[Directions.ALL.length];
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof Node)) {
			return false;
		}

		Node node = (Node)obj;

		return world == node.world && pos.equals(node.pos);
	}

	@Override
	public int hashCode() {
		return pos.hashCode();
	}

	Node set(BlockPos pos, BlockState state, boolean clearNeighbors) {
		if (state.is(Block.REDSTONE_WIRE)) {
			throw new IllegalStateException("Cannot update a regular Node to a WireNode!");
		}

		if (clearNeighbors) {
			Arrays.fill(neighbors, null);
		}

		this.pos = pos;
		this.state = state;
		this.invalid = false;

		this.flags = 0;

		if (this.state.isConductor()) {
			this.flags |= CONDUCTOR;
		}
		if (this.state.isSignalSource()) {
			this.flags |= SOURCE;
		}

		return this;
	}

	/**
	 * Determine the priority with which this node should be queued.
	 */
	int priority() {
		return neighborWire.priority;
	}

	public boolean isWire() {
		return false;
	}

	public boolean isConductor() {
		return (flags & CONDUCTOR) != 0;
	}

	public boolean isSignalSource() {
		return (flags & SOURCE) != 0;
	}

	public WireNode asWire() {
		throw new UnsupportedOperationException("Not a WireNode!");
	}
}
