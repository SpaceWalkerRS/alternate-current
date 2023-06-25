package alternate.current.wire;

import java.util.Iterator;
import java.util.Queue;
import java.util.function.Consumer;

//import alternate.current.AlternateCurrentMod;
//import alternate.current.util.profiler.Profiler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Redstone;

/**
 * This class handles power changes for redstone wire. The algorithm was
 * designed with the following goals in mind:
 * <br>
 * 1. Minimize the number of times a wire checks its surroundings to determine
 * its power level.
 * <br>
 * 2. Minimize the number of block and shape updates emitted.
 * <br>
 * 3. Emit block and shape updates in a deterministic, non-locational order,
 * fixing bug MC-11193.
 * 
 * <p>
 * In Vanilla redstone wire is laggy because it fails on points 1 and 2.
 * 
 * <p>
 * Redstone wire updates recursively and each wire calculates its power level in
 * isolation rather than in the context of the network it is a part of. This
 * means a wire in a grid can change its power level over half a dozen times
 * before settling on its final value. This problem used to be worse in 1.13 and
 * below, where a wire would only decrease its power level by 1 at a time.
 * 
 * <p>
 * In addition to this, a wire emits 42 block updates and up to 22 shape updates
 * each time it changes its power level.
 * 
 * <p>
 * Of those 42 block updates, 6 are to itself, which are thus not only
 * redundant, but a big source of lag, since those cause the wire to
 * unnecessarily re-calculate its power level. A block only has 24 neighbors
 * within a Manhattan distance of 2, meaning 12 of the remaining 36 block
 * updates are duplicates and thus also redundant.
 * 
 * <p>
 * Of the 22 shape updates, only 6 are strictly necessary. The other 16 are sent
 * to blocks diagonally above and below. These are necessary if a wire changes
 * its connections, but not when it changes its power level.
 * 
 * <p>
 * Redstone wire in Vanilla also fails on point 3, though this is more of a
 * quality-of-life issue than a lag issue. The recursive nature in which it
 * updates, combined with the location-dependent order in which each wire
 * updates its neighbors, makes the order in which neighbors of a wire network
 * are updated incredibly inconsistent and seemingly random.
 * 
 * <p>
 * Alternate Current fixes each of these problems as follows.
 * 
 * <p>
 * 1. To make sure a wire calculates its power level as little as possible, we
 * remove the recursive nature in which redstone wire updates in Vanilla.
 * Instead, we build a network of connected wires, find those wires that receive
 * redstone power from "outside" the network, and spread the power from there.
 * This has a few advantages:
 * <br>
 * - Each wire checks for power from non-wire components at most once, and from
 * nearby wires just twice.
 * <br>
 * - Each wire only sets its power level in the world once. This is important,
 * because calls to Level.setBlock are even more expensive than calls to
 * Level.getBlockState.
 * 
 * <p>
 * 2. There are 2 obvious ways in which we can reduce the number of block and
 * shape updates.
 * <br>
 * - Get rid of the 18 redundant block updates and 16 redundant shape updates,
 * so each wire only emits 24 block updates and 6 shape updates whenever it
 * changes its power level.
 * <br>
 * - Only emit block updates and shape updates once a wire reaches its final
 * power level, rather than at each intermediary stage.
 * <br>
 * For an individual wire, these two optimizations are the best you can do, but
 * for an entire grid, you can do better!
 * 
 * <p>
 * Since we calculate the power of the entire network, sending block and shape
 * updates to the wires in it is redundant. Removing those updates can reduce
 * the number of block and shape updates by up to 20%.
 * 
 * <p>
 * 3. To make the order of block updates to neighbors of a network
 * deterministic, the first thing we must do is to replace the location-
 * dependent order in which a wire updates its neighbors. Instead, we base it on
 * the direction of power flow. This part of the algorithm was heavily inspired
 * by theosib's 'RedstoneWireTurbo', which you can read more about in theosib's
 * comment on Mojira <a href="https://bugs.mojang.com/browse/MC-81098?focusedCommentId=420777&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-420777">here</a>
 * or by checking out its implementation in carpet mod <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/helpers/RedstoneWireTurbo.java">here</a>.
 * 
 * <p>
 * The idea is to determine the direction of power flow through a wire based on
 * the power it receives from neighboring wires. For example, if the only power
 * a wire receives is from a neighboring wire to its west, it can be said that
 * the direction of power flow through the wire is east.
 * 
 * <p>
 * We make the order of block updates to neighbors of a wire depend on what is
 * determined to be the direction of power flow. This not only removes
 * locationality entirely, it even removes directionality in a large number of
 * cases. Unlike in 'RedstoneWireTurbo', however, I have decided to keep a
 * directional element in ambiguous cases, rather than to introduce randomness,
 * though this is trivial to change.
 * 
 * <p>
 * While this change fixes the block update order of individual wires, we must
 * still address the overall block update order of a network. This turns out to
 * be a simple fix, because of a change we made earlier: we search through the
 * network for wires that receive power from outside it, and spread the power
 * from there. If we make each wire transmit its power to neighboring wires in
 * an order dependent on the direction of power flow, we end up with a
 * non-locational and largely non-directional wire update order.
 * 
 * @author Space Walker
 */
public class WireHandler {

	public static class Directions {

		public static final Direction[] ALL        = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN, Direction.UP };
		public static final Direction[] HORIZONTAL = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH };

		// Indices for the arrays above.
		// The cardinal directions are ordered clockwise. This allows
		// for conversion between relative and absolute directions
		// ('left' 'right' vs 'east' 'west') with simple arithmetic:
		// If some Direction index 'iDir' is considered 'forward', then
		// '(iDir + 1) & 0b11' is 'right', '(iDir + 2) & 0b11' is 'backward', etc.
		public static final int WEST  = 0b000; // 0
		public static final int NORTH = 0b001; // 1
		public static final int EAST  = 0b010; // 2
		public static final int SOUTH = 0b011; // 3
		public static final int DOWN  = 0b100; // 4
		public static final int UP    = 0b101; // 5

		public static int iOpposite(int iDir) {
			return iDir ^ (0b10 >>> (iDir >>> 2));
		}

		// Each array is placed at the index that encodes the direction that is missing
		// from the array.
		private static final int[][] I_EXCEPT = {
			{       NORTH, EAST, SOUTH, DOWN, UP },
			{ WEST,        EAST, SOUTH, DOWN, UP },
			{ WEST, NORTH,       SOUTH, DOWN, UP },
			{ WEST, NORTH, EAST,        DOWN, UP },
			{ WEST, NORTH, EAST, SOUTH,       UP },
			{ WEST, NORTH, EAST, SOUTH, DOWN     }
		};
		private static final int[][] I_EXCEPT_CARDINAL = {
			{       NORTH, EAST, SOUTH },
			{ WEST,        EAST, SOUTH },
			{ WEST, NORTH,       SOUTH },
			{ WEST, NORTH, EAST,       },
			{ WEST, NORTH, EAST, SOUTH },
			{ WEST, NORTH, EAST, SOUTH }
		};
	}

	/**
	 * This conversion table takes in information about incoming flow, and outputs
	 * the determined outgoing flow.
	 * 
	 * <p>
	 * The input is a 4 bit number that encodes the incoming flow. Each bit
	 * represents a cardinal direction, and when it is 'on', there is flow in that
	 * direction.
	 * 
	 * <p>
	 * The output is a single Direction index, or -1 for ambiguous cases.
	 * 
	 * <p>
	 * The outgoing flow is determined as follows:
	 * 
	 * <p>
	 * If there is just 1 direction of incoming flow, that direction will be the
	 * direction of outgoing flow.
	 * 
	 * <p>
	 * If there are 2 directions of incoming flow, and these directions are not each
	 * other's opposites, the direction that is 'more clockwise' will be the
	 * direction of outgoing flow. More precisely, the direction that is 1 clockwise
	 * turn from the other is picked.
	 * 
	 * <p>
	 * If there are 3 directions of incoming flow, the two opposing directions
	 * cancel each other out, and the remaining direction will be the direction of
	 * outgoing flow.
	 * 
	 * <p>
	 * In all other cases, the flow is completely ambiguous.
	 */
	static final int[] FLOW_IN_TO_FLOW_OUT = {
		-1,               // 0b0000: -                     -> x
		Directions.WEST,  // 0b0001: west                  -> west
		Directions.NORTH, // 0b0010: north                 -> north
		Directions.NORTH, // 0b0011: west/north            -> north
		Directions.EAST,  // 0b0100: east                  -> east
		-1,               // 0b0101: west/east             -> x
		Directions.EAST,  // 0b0110: north/east            -> east
		Directions.NORTH, // 0b0111: west/north/east       -> north
		Directions.SOUTH, // 0b1000: south                 -> south
		Directions.WEST,  // 0b1001: west/south            -> west
		-1,               // 0b1010: north/south           -> x
		Directions.WEST,  // 0b1011: west/north/south      -> west
		Directions.SOUTH, // 0b1100: east/south            -> south
		Directions.SOUTH, // 0b1101: west/east/south       -> south
		Directions.EAST,  // 0b1110: north/east/south      -> east
		-1,               // 0b1111: west/north/east/south -> x
	};
	/**
	 * Update orders of all directions. Given that the index encodes the direction
	 * that is to be considered 'forward', the resulting update order is
	 * { front, back, right, left, down, up }.
	 */
	static final int[][] FULL_UPDATE_ORDERS = {
		{ Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH, Directions.DOWN, Directions.UP },
		{ Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST , Directions.DOWN, Directions.UP },
		{ Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH, Directions.DOWN, Directions.UP },
		{ Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST , Directions.DOWN, Directions.UP }
	};
	/**
	 * The default update order of all directions. It is equivalent to the order of
	 * shape updates in vanilla Minecraft.
	 */
	static final int[] DEFAULT_FULL_UPDATE_ORDER = FULL_UPDATE_ORDERS[0];
	/**
	 * Update orders of cardinal directions. Given that the index encodes the
	 * direction that is to be considered 'forward', the resulting update order is
	 * { front, back, right, left }.
	 */
	static final int[][] CARDINAL_UPDATE_ORDERS = {
		{ Directions.WEST , Directions.EAST , Directions.NORTH, Directions.SOUTH },
		{ Directions.NORTH, Directions.SOUTH, Directions.EAST , Directions.WEST  },
		{ Directions.EAST , Directions.WEST , Directions.SOUTH, Directions.NORTH },
		{ Directions.SOUTH, Directions.NORTH, Directions.WEST , Directions.EAST  }
	};
	/**
	 * The default update order of all cardinal directions.
	 */
	static final int[] DEFAULT_CARDINAL_UPDATE_ORDER = CARDINAL_UPDATE_ORDERS[0];

	private static final int POWER_MIN = Redstone.SIGNAL_MIN;
	private static final int POWER_MAX = Redstone.SIGNAL_MAX;
	private static final int POWER_STEP = 1;

	// If Vanilla will ever multi-thread the ticking of levels, there should
	// be only one WireHandler per level, in case redstone updates in multiple
	// levels at the same time. There are already mods that add multi-threading
	// as well.
	private final ServerLevel level;

	/** Map of wires and neighboring blocks. */
	private final Long2ObjectMap<Node> nodes;
	/** Queue for the breadth-first search through the network. */
	private final Queue<WireNode> search;
	/** Queue of updates to wires and neighboring blocks. */
	private final Queue<Node> updates;

	// Rather than creating new nodes every time a network is updated we keep
	// a cache of nodes that can be re-used.
	private Node[] nodeCache;
	private int nodeCount;

	/** Is this WireHandler currently working through the update queue? */
	private boolean updating;

	public WireHandler(ServerLevel level) {
		this.level = level;

		this.nodes = new Long2ObjectOpenHashMap<>();
		this.search = new SimpleQueue();
		this.updates = new PriorityQueue();

		this.nodeCache = new Node[16];
		this.fillNodeCache(0, 16);
	}

	/**
	 * Retrieve the {@link alternate.current.wire.Node Node} that represents the
	 * block at the given position in the level.
	 */
	private Node getOrAddNode(BlockPos pos) {
		return nodes.compute(pos.asLong(), (key, node) -> {
			if (node == null) {
				// If there is not yet a node at this position, retrieve and
				// update one from the cache.
				return getNextNode(pos);
			}
			if (node.invalid) {
				return revalidateNode(node);
			}

			return node;
		});
	}

	/**
	 * Remove and return the {@link alternate.current.wire.Node Node} at the given
	 * position.
	 */
	private Node removeNode(BlockPos pos) {
		return nodes.remove(pos.asLong());
	}

	/**
	 * Return a {@link alternate.current.wire.Node Node} that represents the block
	 * at the given position.
	 */
	private Node getNextNode(BlockPos pos) {
		return getNextNode(pos, level.getBlockState(pos));
	}

	/**
	 * Return a node that represents the given position and block state. If it is a
	 * wire, then create a new {@link alternate.current.wire.WireNode WireNode}.
	 * Otherwise, grab the next {@link alternate.current.wire.Node Node} from the
	 * cache and update it.
	 */
	private Node getNextNode(BlockPos pos, BlockState state) {
		return state.is(Blocks.REDSTONE_WIRE) ? new WireNode(level, pos, state) : getNextNode().set(pos, state, true);
	}

	/**
	 * Grab the first unused node from the cache. If all of the cache is already in
	 * use, increase it in size first.
	 */
	private Node getNextNode() {
		if (nodeCount == nodeCache.length) {
			increaseNodeCache();
		}

		return nodeCache[nodeCount++];
	}

	private void increaseNodeCache() {
		Node[] oldCache = nodeCache;
		nodeCache = new Node[oldCache.length << 1];

		for (int index = 0; index < oldCache.length; index++) {
			nodeCache[index] = oldCache[index];
		}

		fillNodeCache(oldCache.length, nodeCache.length);
	}

	private void fillNodeCache(int start, int end) {
		for (int index = start; index < end; index++) {
			nodeCache[index] = new Node(level);
		}
	}

	/**
	 * Try to revalidate the given node by looking at the block state that is
	 * occupying its position. If the given node is a wire but the block state is
	 * not, or vice versa, a new node must be created/grabbed from the cache.
	 * Otherwise, the node can be quickly revalidated with the new block state.
	 */
	private Node revalidateNode(Node node) {
		BlockPos pos = node.pos;
		BlockState state = level.getBlockState(pos);

		boolean wasWire = node.isWire();
		boolean isWire = state.is(Blocks.REDSTONE_WIRE);

		if (wasWire != isWire) {
			return getNextNode(pos, state);
		}

		node.invalid = false;

		if (isWire) {
			// No need to update the block state of this wire - it will grab
			// the current block state just before setting power anyway.
			WireNode wire = node.asWire();

			wire.root = false;
			wire.discovered = false;
			wire.searched = false;
		} else {
			node.set(pos, state, false);
		}

		return node;
	}

	/**
	 * Retrieve the neighbor of a node in the given direction and create a link
	 * between the two nodes if they are not yet linked. This link makes accessing
	 * neighbors of a node signficantly faster.
	 */
	private Node getNeighbor(Node node, int iDir) {
		Node neighbor = node.neighbors[iDir];

		if (neighbor == null || neighbor.invalid) {
			Direction dir = Directions.ALL[iDir];
			BlockPos pos = node.pos.relative(dir);

			Node oldNeighbor = neighbor;
			neighbor = getOrAddNode(pos);

			if (neighbor != oldNeighbor) {
				int iOpp = Directions.iOpposite(iDir);

				node.neighbors[iDir] = neighbor;
				neighbor.neighbors[iOpp] = node;
			}
		}

		return neighbor;
	}

	/**
	 * Iterate over all neighboring nodes of the given wire. The iteration order is
	 * designed to be an extension of the default block update order, and is
	 * determined as follows:
	 * <br>
	 * 1. The direction of power flow through the wire is to be considered
	 * 'forward'. The iteration order depends on the neighbors' relative positions
	 * to the wire.
	 * <br>
	 * 2. Each neighbor is identified by the step(s) you must take, starting at the
	 * wire, to reach it. Each step is 1 block, thus the position of a neighbor is
	 * encoded by the direction(s) of the step(s), e.g. (right), (down), (up, left),
	 * etc.
	 * <br>
	 * 3. Neighbors are iterated over in pairs that lie on opposite sides of the
	 * wire.
	 * <br>
	 * 4. Neighbors are iterated over in order of their distance from the wire. This
	 * means they are iterated over in 3 groups: direct neighbors first, then
	 * diagonal neighbors, and last are the far neighbors that are 2 blocks directly
	 * out.
	 * <br>
	 * 5. The order within each group is determined using the following basic order:
	 * { front, back, right, left, down, up }. This order was chosen because it
	 * converts to the following order of absolute directions when west is said to
	 * be 'forward': { west, east, north, south, down, up } - this is the order of
	 * shape updates.
	 */
	private void forEachNeighbor(WireNode wire, Consumer<Node> consumer) {
		int forward   = wire.iFlowDir;
		int rightward = (forward + 1) & 0b11;
		int backward  = (forward + 2) & 0b11;
		int leftward  = (forward + 3) & 0b11;
		int downward  = Directions.DOWN;
		int upward    = Directions.UP;

		Node front = getNeighbor(wire, forward);
		Node right = getNeighbor(wire, rightward);
		Node back  = getNeighbor(wire, backward);
		Node left  = getNeighbor(wire, leftward);
		Node below = getNeighbor(wire, downward);
		Node above = getNeighbor(wire, upward);

		// direct neighbors (6)
		consumer.accept(front);
		consumer.accept(back);
		consumer.accept(right);
		consumer.accept(left);
		consumer.accept(below);
		consumer.accept(above);

		// diagonal neighbors (12)
		consumer.accept(getNeighbor(front, rightward));
		consumer.accept(getNeighbor(back, leftward));
		consumer.accept(getNeighbor(front, leftward));
		consumer.accept(getNeighbor(back, rightward));
		consumer.accept(getNeighbor(front, downward));
		consumer.accept(getNeighbor(back, upward));
		consumer.accept(getNeighbor(front, upward));
		consumer.accept(getNeighbor(back, downward));
		consumer.accept(getNeighbor(right, downward));
		consumer.accept(getNeighbor(left, upward));
		consumer.accept(getNeighbor(right, upward));
		consumer.accept(getNeighbor(left, downward));

		// far neighbors (6)
		consumer.accept(getNeighbor(front, forward));
		consumer.accept(getNeighbor(back, backward));
		consumer.accept(getNeighbor(right, rightward));
		consumer.accept(getNeighbor(left, leftward));
		consumer.accept(getNeighbor(below, downward));
		consumer.accept(getNeighbor(above, upward));
	}

	/**
	 * This method should be called whenever a wire receives a block update.
	 */
	public boolean onWireUpdated(BlockPos pos) {
		Node node = getOrAddNode(pos);

		invalidate();
		findRoots(pos);
		tryUpdate();

		return node.isWire();
	}

	/**
	 * This method should be called whenever a wire is placed.
	 */
	public void onWireAdded(BlockPos pos) {
		Node node = getOrAddNode(pos);

		if (!node.isWire()) {
			return; // we should never get here
		}

		WireNode wire = node.asWire();
		wire.added = true;

		invalidate();
		revalidateNode(wire);
		findRoot(wire);
		tryUpdate();
	}

	/**
	 * This method should be called whenever a wire is removed.
	 */
	public void onWireRemoved(BlockPos pos, BlockState state) {
		Node node = removeNode(pos);
		WireNode wire;

		if (node == null || !node.isWire()) {
			wire = new WireNode(level, pos, state);
		} else {
			wire = node.asWire();
		}

		wire.invalid = true;
		wire.removed = true;

		// If these fields are set to 'true', the removal of this wire was part of
		// already ongoing power changes, so we can exit early here.
		if (updating && wire.shouldBreak) {
			return;
		}

		invalidate();
		revalidateNode(wire);
		findRoot(wire);
		tryUpdate();
	}

	/**
	 * The nodes map is a snapshot of the state of the world. It becomes invalid
	 * when power changes are carried out, since the block and shape updates can
	 * lead to block changes. If these block changes cause the network to be updated
	 * again every node must be invalidated, and revalidated before it is used
	 * again. This ensures the power calculations of the network are accurate.
	 */
	private void invalidate() {
		if (updating && !nodes.isEmpty()) {
			Iterator<Entry<Node>> it = Long2ObjectMaps.fastIterator(nodes);

			while (it.hasNext()) {
				Entry<Node> entry = it.next();
				Node node = entry.getValue();

				node.invalid = true;
			}
		}
	}

	/**
	 * Look for wires at and around the given position that are in an invalid state
	 * and require power changes. These wires are called 'roots' because it is only
	 * when these wires change power level that neighboring wires must adjust as
	 * well.
	 * 
	 * <p>
	 * While it is strictly only necessary to check the wire at the given position,
	 * if that wire is part of a network, it is beneficial to check its surroundings
	 * for other wires that require power changes. This is because a network can
	 * receive power at multiple points. Consider the following setup:
	 * 
	 * <p>
	 * (top-down view, W = wire, L = lever, _ = air/other)
	 * <br> {@code _ _ W _ _ }
	 * <br> {@code _ W W W _ }
	 * <br> {@code W W L W W }
	 * <br> {@code _ W W W _ }
	 * <br> {@code _ _ W _ _ }
	 * 
	 * <p>
	 * The lever powers four wires in the network at once. If this is identified
	 * correctly, the entire network can (un)power at once. While it is not
	 * practical to cover every possible situation where a network is (un)powered
	 * from multiple points at once, checking for common cases like the one
	 * described above is relatively straight-forward.
	 */
	private void findRoots(BlockPos pos) {
		Node node = getOrAddNode(pos);

		if (!node.isWire()) {
			return; // we should never get here
		}

		WireNode wire = node.asWire();
		findRoot(wire);

		// If the wire at the given position is not in an invalid state
		// we can exit early.
		if (!wire.searched) {
			return;
		}

		for (int iDir : FULL_UPDATE_ORDERS[wire.iFlowDir]) {
			Node neighbor = getNeighbor(wire, iDir);

			if (neighbor.isConductor() || neighbor.isSignalSource()) {
				findRootsAround(neighbor, Directions.iOpposite(iDir));
			}
		}
	}

	/**
	 * Look for wires around the given node that require power changes.
	 */
	private void findRootsAround(Node node, int except) {
		for (int iDir : Directions.I_EXCEPT_CARDINAL[except]) {
			Node neighbor = getNeighbor(node, iDir);

			if (neighbor.isWire()) {
				findRoot(neighbor.asWire());
			}
		}
	}

	/**
	 * Check if the given wire requires power changes. If it does, queue it for the
	 * breadth-first search as a root.
	 */
	private void findRoot(WireNode wire) {
		// Each wire only needs to be checked once.
		if (wire.discovered) {
			return;
		}

		discover(wire);
		findExternalPower(wire);
		findPower(wire, false);

		if (needsUpdate(wire)) {
			searchRoot(wire);
		}
	}

	/**
	 * Prepare the given wire for the breadth-first search. This means:
	 * <br>
	 * - Check if the wire should break. Rather than breaking the wire right away,
	 * its effects are integrated into the power calculations.
	 * <br>
	 * - Reset the virtual and external power.
	 * <br>
	 * - Find connections to neighboring wires.
	 */
	private void discover(WireNode wire) {
		if (wire.discovered) {
			return;
		}

		wire.discovered = true;
		wire.searched = false;

		if (!wire.removed && !wire.shouldBreak && !wire.state.canSurvive(level, wire.pos)) {
			wire.shouldBreak = true;
		}

		wire.virtualPower = wire.currentPower;
		wire.externalPower = POWER_MIN - 1;

		wire.connections.set(this::getNeighbor);
	}

	/**
	 * Determine the power level the given wire receives from the blocks around it.
	 * Power from non-wire components only needs to be computed if power from
	 * neighboring wires has decreased, so as to determine how low the power of the
	 * wire can fall.
	 */
	private void findPower(WireNode wire, boolean ignoreSearched) {
		// As wire power is (re-)computed, flow information must be reset.
		wire.virtualPower = wire.externalPower;
		wire.flowIn = 0;

		// If the wire is removed or going to break, its power level should always be
		// the minimum value. This is because it (effectively) no longer exists, so
		// cannot provide any power to neighboring wires.
		if (wire.removed || wire.shouldBreak) {
			return;
		}

		// Power received from neighboring wires will never exceed POWER_MAX -
		// POWER_STEP, so if the external power is already larger than or equal to
		// that, there is no need to check for power from neighboring wires.
		if (wire.externalPower < (POWER_MAX - POWER_STEP)) {
			findWirePower(wire, ignoreSearched);
		}
	}

	/**
	 * Determine the power the given wire receives from connected neighboring wires
	 * and update the virtual power accordingly.
	 */
	private void findWirePower(WireNode wire, boolean ignoreSearched) {
		wire.connections.forEach(connection -> {
			if (!connection.accept) {
				return;
			}

			WireNode neighbor = connection.wire;

			if (!ignoreSearched || !neighbor.searched) {
				int power = Math.max(POWER_MIN, neighbor.virtualPower - POWER_STEP);
				int iOpp = Directions.iOpposite(connection.iDir);

				wire.offerPower(power, iOpp);
			}
		});
	}

	/**
	 * Determine the redstone signal the given wire receives from non-wire
	 * components and update the virtual power accordingly.
	 */
	private void findExternalPower(WireNode wire) {
		// If the wire is removed or going to break, its power level should always be
		// the minimum value. Thus external power need not be computed.
		// In other cases external power need only be computed once.
		if (wire.removed || wire.shouldBreak || wire.externalPower >= POWER_MIN) {
			return;
		}

		wire.externalPower = getExternalPower(wire);

		if (wire.externalPower > wire.virtualPower) {
			wire.virtualPower = wire.externalPower;
		}
	}

	/**
	 * Determine the redstone signal the given wire receives from non-wire
	 * components.
	 */
	private int getExternalPower(WireNode wire) {
		int power = POWER_MIN;

		for (int iDir = 0; iDir < Directions.ALL.length; iDir++) {
			Node neighbor = getNeighbor(wire, iDir);

			// Power from wires is handled separately.
			if (neighbor.isWire()) {
				continue;
			}

			// Since 1.16 there is a block that is both a conductor and a signal
			// source: the target block!
			if (neighbor.isConductor()) {
				power = Math.max(power, getDirectSignalTo(wire, neighbor, Directions.iOpposite(iDir)));
			}
			if (neighbor.isSignalSource()) {
				power = Math.max(power, neighbor.state.getSignal(level, neighbor.pos, Directions.ALL[iDir]));
			}

			if (power >= POWER_MAX) {
				return POWER_MAX;
			}
		}

		return power;
	}

	/**
	 * Determine the direct signal the given wire receives from neighboring blocks
	 * through the given conductor node.
	 */
	private int getDirectSignalTo(WireNode wire, Node node, int except) {
		int power = POWER_MIN;

		for (int iDir : Directions.I_EXCEPT[except]) {
			Node neighbor = getNeighbor(node, iDir);

			if (neighbor.isSignalSource()) {
				power = Math.max(power, neighbor.state.getDirectSignal(level, neighbor.pos, Directions.ALL[iDir]));

				if (power >= POWER_MAX) {
					return POWER_MAX;
				}
			}
		}

		return power;
	}

	/**
	 * Check if the given wire needs to update its state in the world.
	 */
	private boolean needsUpdate(WireNode wire) {
		return wire.removed || wire.shouldBreak || wire.virtualPower != wire.currentPower;
	}

	/**
	 * Queue the given wire for the breadth-first search as a root.
	 */
	private void searchRoot(WireNode wire) {
		int iBackupFlowDir;

		if (wire.connections.iFlowDir < 0) {
			iBackupFlowDir = 0;
		} else {
			iBackupFlowDir = wire.connections.iFlowDir;
		}

		search(wire, true, iBackupFlowDir);
	}

	/**
	 * Queue the given wire for the breadth-first search and set a backup flow
	 * direction.
	 */
	private void search(WireNode wire, boolean root, int iBackupFlowDir) {
		search.offer(wire);

		wire.root = root;
		wire.searched = true;
		// Normally the flow is not set until the power level is updated. However,
		// in networks with multiple power sources the update order between them
		// depends on which was discovered first. To make this less prone to
		// directionality, each wire node is given a 'backup' flow. For roots, this
		// is the determined flow of their connections. For non-roots this is the
		// direction from which they were discovered.
		wire.iFlowDir = iBackupFlowDir;
	}

	private void tryUpdate() {
		if (!search.isEmpty()) {
			update();
		}
		if (!updating) {
			nodes.clear();
			nodeCount = 0;
		}
	}

	/**
	 * Update the network and neighboring blocks. This is done in 3 steps.
	 * 
	 * <p>
	 * <b>1. Search through the network</b>
	 * <br>
	 * Conduct a breadth-first search around the roots to find wires that are in an
	 * invalid state and need power changes.
	 * 
	 * <p>
	 * <b>2. Depower the network</b>
	 * <br>
	 * Depower all wires in the network. This allows power to be spread most
	 * efficiently.
	 * 
	 * <p>
	 * <b>3. Power the network</b>
	 * <br>
	 * Work through the update queue, setting the new power level of each wire and
	 * updating neighboring blocks. After a wire has updated its power level, it
	 * will emit shape updates and queue updates for neighboring wires and blocks.
	 */
	private void update() {
		// The profiler keeps track of how long various parts of the algorithm take.
		// It is only here for debugging purposes, and is commented out in production.
//		Profiler profiler = AlternateCurrentMod.createProfiler();
//		profiler.start();

		// Search through the network for wires that need power changes. This includes
		// the roots as well as any wires that will be affected by power changes to
		// those roots.
//		profiler.push("search network");
		searchNetwork();

		// Depower all the wires in the network.
//		profiler.swap("depower network");
		depowerNetwork();

		// Bring each wire up to its new power level and update neighboring blocks.
//		profiler.swap("power network");
		try {
			powerNetwork();
		} catch (Throwable t) {
			// If anything goes wrong while carrying out power changes, this field must
			// be reset to 'false', or the wire handler will be locked out of carrying
			// out power changes until the world is reloaded.
			updating = false;

			throw t;
//		} finally {
//			profiler.pop();
//			profiler.end();
		}
	}

	/**
	 * Search through the network for wires that are in an invalid state and need
	 * power changes. These wires are added to the end of the queue, so that their
	 * neighbors can be searched next.
	 */
	private void searchNetwork() {
		for (WireNode wire : search) {
			// The order in which wires are searched will influence the order in
			// which they update their power levels.
			wire.connections.forEach(connection -> {
				if (!connection.offer) {
					return;
				}

				WireNode neighbor = connection.wire;

				if (neighbor.searched) {
					return;
				}

				discover(neighbor);
				findPower(neighbor, false);

				// If power from neighboring wires has decreased, check for power
				// from non-wire components so as to determine how low power can
				// fall.
				if (neighbor.virtualPower < neighbor.currentPower) {
					findExternalPower(neighbor);
				}

				if (needsUpdate(neighbor)) {
					search(neighbor, false, connection.iDir);
				}
			}, wire.iFlowDir);
		}
	}

	/**
	 * Depower all wires in the network so that power can be spread from the power
	 * sources.
	 */
	private void depowerNetwork() {
		while (!search.isEmpty()) {
			WireNode wire = search.poll();
			findPower(wire, true);

			if (wire.root || wire.removed || wire.shouldBreak || wire.virtualPower > POWER_MIN) {
				queueWire(wire);
			} else {
				// Wires that do not receive any power do not queue power changes
				// until they are offered power from a neighboring wire. To ensure
				// that they accept any power from neighboring wires and thus queue
				// their power changes, their virtual power is set to below the
				// minimum.
				wire.virtualPower--;
			}
		}
	}

	/**
	 * Work through the update queue, setting the new power level of each wire, then
	 * queueing updates to connected wires and neighboring blocks.
	 */
	private void powerNetwork() {
		// If an instantaneous update chain causes updates to another network
		// (or the same network in another place), new power changes will be
		// integrated into the already ongoing power queue, so we can exit early
		// here.
		if (updating) {
			return;
		}

		updating = true;

		while (!updates.isEmpty()) {
			Node node = updates.poll();

			if (node.isWire()) {
				WireNode wire = node.asWire();

				if (!needsUpdate(wire)) {
					continue;
				}

				findPowerFlow(wire);
				transmitPower(wire);

				if (wire.setPower()) {
					queueNeighbors(wire);

					// If the wire was newly placed or removed, shape updates have
					// already been emitted.
					if (!wire.added && !wire.shouldBreak) {
						updateNeighborShapes(wire);
					}
				}
			} else {
				WireNode neighborWire = node.neighborWire;

				if (neighborWire != null) {
					BlockPos neighborPos = neighborWire.pos;
					Block neighborBlock = neighborWire.state.getBlock();

					updateBlock(node, neighborPos, neighborBlock);
				}
			}
		}

		updating = false;
	}

	/**
	 * Use the information of incoming power flow to determine the direction of
	 * power flow through this wire. If that flow is ambiguous, try to use a flow
	 * direction based on connections to neighboring wires. If that is also
	 * ambiguous, use the backup value that was set when the wire was first added to
	 * the network.
	 */
	private void findPowerFlow(WireNode wire) {
		int flow = FLOW_IN_TO_FLOW_OUT[wire.flowIn];

		if (flow >= 0) {
			wire.iFlowDir = flow;
		} else if (wire.connections.iFlowDir >= 0) {
			wire.iFlowDir = wire.connections.iFlowDir;
		}
	}

	/**
	 * Transmit power from the given wire to neighboring wires and queue updates to
	 * those wires.
	 */
	private void transmitPower(WireNode wire) {
		wire.connections.forEach(connection -> {
			if (!connection.offer) {
				return;
			}

			WireNode neighbor = connection.wire;

			int power = Math.max(POWER_MIN, wire.virtualPower - POWER_STEP);
			int iDir = connection.iDir;

			if (neighbor.offerPower(power, iDir)) {
				queueWire(neighbor);
			}
		}, wire.iFlowDir);
	}

	/**
	 * Emit shape updates around the given wire.
	 */
	private void updateNeighborShapes(WireNode wire) {
		BlockPos wirePos = wire.pos;
		BlockState wireState = wire.state;

		for (int iDir : DEFAULT_FULL_UPDATE_ORDER) {
			Node neighbor = getNeighbor(wire, iDir);

			if (!neighbor.isWire()) {
				int iOpp = Directions.iOpposite(iDir);
				Direction opp = Directions.ALL[iOpp];

				updateShape(neighbor, opp, wirePos, wireState);
			}
		}
	}

	private void updateShape(Node node, Direction dir, BlockPos neighborPos, BlockState neighborState) {
		BlockPos pos = node.pos;
		BlockState state = level.getBlockState(pos);

		// Shape updates to redstone wire are very expensive, and should never happen
		// as a result of power changes anyway.
		if (!state.isAir() && !state.is(Blocks.REDSTONE_WIRE)) {
			BlockState newState = state.updateShape(dir, neighborState, level, pos, neighborPos);
			Block.updateOrDestroy(state, newState, level, pos, Block.UPDATE_CLIENTS);
		}
	}

	/**
	 * Queue block updates to nodes around the given wire.
	 */
	private void queueNeighbors(WireNode wire) {
		forEachNeighbor(wire, neighbor -> {
			queueNeighbor(neighbor, wire);
		});
	}

	/**
	 * Queue the given node for an update from the given neighboring wire.
	 */
	private void queueNeighbor(Node node, WireNode neighborWire) {
		// Updates to wires are queued when power is transmitted.
		if (!node.isWire()) {
			node.neighborWire = neighborWire;
			updates.offer(node);
		}
	}

	/**
	 * Queue the given wire for a power change. If the wire does not need a power
	 * change (perhaps because its power has already changed), transmit power to
	 * neighboring wires.
	 */
	private void queueWire(WireNode wire) {
		if (needsUpdate(wire)) {
			updates.offer(wire);
		} else {
			findPowerFlow(wire);
			transmitPower(wire);
		}
	}

	/**
	 * Emit a block update to the given node.
	 */
	private void updateBlock(Node node, BlockPos neighborPos, Block neighborBlock) {
		BlockPos pos = node.pos;
		BlockState state = level.getBlockState(pos);

		// While this check makes sure wires in the network are not given block
		// updates, it also prevents block updates to wires in neighboring networks.
		// While this should not make a difference in theory, in practice, it is
		// possible to force a network into an invalid state without updating it, even
		// if it is relatively obscure.
		// While I was willing to make this compromise in return for some significant
		// performance gains in certain setups, if you are not, you can add all the
		// positions of the network to a set and filter out block updates to wires in
		// the network that way.
		if (!state.isAir() && !state.is(Blocks.REDSTONE_WIRE)) {
			state.neighborChanged(level, pos, neighborBlock, neighborPos, false);
		}
	}

	@FunctionalInterface
	public static interface NodeProvider {

		public Node getNeighbor(Node node, int iDir);

	}
}
