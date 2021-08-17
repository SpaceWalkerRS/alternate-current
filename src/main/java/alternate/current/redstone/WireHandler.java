package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Queue;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.Directions;
import alternate.current.util.profiler.Profiler;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

/**
 * This class handles power changes for redstone wire. The algorithm
 * was designed with the following goals in mind:
 * <br>
 * 1. Minimize the number of times a wire checks its surroundings to
 *    determine its power level.
 * <br>
 * 2. Minimize the number of block and shape updates emitted.
 * <br>
 * 3. Emit block and shape updates in a deterministic, non-locational
 *    order, fixing bug MC-11193.
 * 
 * <p>
 * In Vanilla redstone wire is laggy because it fails on points 1 and 2.
 * 
 * <p>
 * It updates recursively and each wire calculates its power level in
 * isolation rather than in the context of the network it is a part of.
 * This means a wire in a grid can change its power level over half a
 * dozen times before settling on its final value. This problem used to
 * be worse in 1.14 and below, where a wire would only decrease its power
 * level by 1 at a time.
 * 
 * <p>
 * In addition to this, a wire emits 42 block updates and up to 22 shape
 * updates each time it changes its power level.
 * 
 * <p>
 * Of those 42 block updates, 6 are to itself, which are thus not only
 * redundant, but a big source of lag, since those cause the wire to
 * unnecessarily re-calculate its power level. A block only has 24 
 * neighbors within a Manhattan distance of 2, meaning 12 of the remaining
 * 36 block updates are duplicates and thus also redundant.
 * 
 * <p>
 * Of the 22 shape updates, only 6 are strictly necessary. The other 16
 * are sent to blocks diagonally above and below. These are necessary
 * if a wire changes its connections, but not when it changes its power.
 * 
 * <p>
 * Redstone wire in Vanilla also fails on point 3. The recursive nature
 * in which it updates, combined with the location-dependent order in
 * which each wire updates its neighbors, makes the order in which
 * neighbors of a wire network are updated incredibly inconsistent and
 * seemingly random.
 * 
 * <p>
 * Alternate Current fixes each of these problems as follows.
 * 
 * <p>
 * 1.
 * To make sure a wire calculates its power level as little as possible,
 * we remove the recursive nature in which redstone wire updates in
 * Vanilla. Instead, we build a network of connected wires, find those
 * wires that receive redstone power from "outside" the network, and
 * spread the power from there. This has a few advantages:
 * <br>
 * - Each wire checks for power from non-wire components just once, and
 *   from nearby wires just twice.
 * <br>
 * - Each wire only sets its power level in the world once. This is
 *   important, because calls to World.setBlockState are even more
 *   expensive than calls to World.getBlockState.
 * 
 * <p>
 * 2.
 * There are 2 obvious ways in which we can reduce the number of block
 * and shape updates.
 * <br>
 * - Get rid of the 18 redundant block updates and 16 redundant shape
 *   updates, so each wire only emits 24 block updates and 6 shape updates.
 * <br>
 * - Only emit block updates and shape updates once a wire reaches its
 *   final power level, rather than at each intermediary stage. 
 * <br>
 * For an individual wire, these two optimizations are the best you can
 * do, but for an entire grid, we can do better!
 * 
 * <p>
 * Notice that, while each wire individually makes sure it updates each
 * neighbor only once, each neighbor could still be updated by multiple
 * wires. Removing those redundant block updates can reduce the number
 * of block updates by up to 66%.
 * 
 * <p>
 * And there is more! Since we calculate the power of the entire network,
 * sending block and shape updates to the wires in it is redundant.
 * Removing those updates can reduce the number of block and shape updates
 * by up to 20%.
 * 
 * <p>
 * 3.
 * To make the order of block updates to neighbors of a network
 * deterministic, the first thing we must do is to replace the location-
 * dependent order in which a wire updates its neighbors. The order I have
 * chosen to use can be seen at the bottom of this class.
 * 
 * <p>
 * Next, we make the order of block updates to neighbors of a network as
 * a whole depend on the order in which the wires in it change their power
 * levels. For the sake of breaking as few redstone contraptions as
 * possible, I have chosen to update neighbors around wires in the network
 * in reverse of the order in which they change their power levels.
 * 
 * <p>
 * The order in which wires change their power levels depends on the
 * position of power sources. Wires that change to a higher power level
 * update first. If multiple wires change to the same power level, they
 * update in the order in which the power changes were queued. While this
 * does introduce some direction-dependency, most redstone components
 * already exhibit some direction-dependency, so I do not see this as an
 * issue.
 * 
 * @author Space Walker
 */
public class WireHandler {
	
	private static final int[][] UPDATE_ORDERS = {
		{ 0, 2, 1, 3 },
		{ 1, 3, 2, 0 },
		{ 2, 0, 3, 1 },
		{ 3, 1, 0, 2 }
	};
	private static final int[] FLOW_IN_TO_FLOW_OUT = {
		-1, // 0b0000: x                      -> x
		0 , // 0b0001: west                   -> west
		1 , // 0b0010: north                  -> north
		1 , // 0b0011: west/north             -> north
		2 , // 0b0100: east                   -> east
		-1, // 0b0101: west/east              -> x
		2 , // 0b0110: north/east             -> east
		1 , // 0b0111: west/north/east        -> north
		3 , // 0b1000: south                  -> south
		0 , // 0b1001: west/south             -> west
		-1, // 0b1010: north/south            -> x
		0 , // 0b1011: west/north/south       -> west
		3 , // 0b1100: east/south             -> south
		3 , // 0b1101: west/east/south        -> south
		2 , // 0b1110: north/east/south       -> east
		0 , // 0b1111: west/north/east/south  -> x
	};
	
	/*
	 * While these fields are not strictly necessary, I opted to add
	 * them with "future proofing" in mind, and to avoid hard-coding
	 * certain constants.
	 * 
	 * If Vanilla will ever multi-thread the ticking of dimensions,
	 * there should be only one WireHandler per dimension, in case 
	 * redstone updates in both dimensions at the same time. There are
	 * already mods that add multi-threading as well.
	 * 
	 * If Vanilla ever adds new redstone wire types that cannot interact
	 * with each other, there should be one WireHandler for each wire
	 * type, in case two networks of different types update each other.
	 */
	private final ServerWorld world;
	private final WireBlock wireBlock;
	private final int minPower;
	private final int maxPower;
	private final int powerStep;
	
	/** All the wires in the network */
	private final List<WireNode> network;
	/** Map of wires and neighboring blocks */
	private final Long2ObjectMap<Node> nodes;
	/** All the power changes that need to happen */
	private final Queue<WireNode> powerChanges;
	/** Positions of wires that are part of the network */
	private final Set<BlockPos> wirePositions;
	/** Neighboring positions that should receive block updates */
	private final List<BlockUpdateEntry> blockUpdates;
	
	// Rather than creating new nodes every time a network is updated
	// we keep a cache of nodes that can be re-used.
	private Node[] nodeCache;
	private int usedNodes;
	
	private boolean updatingPower;
	
	public WireHandler(ServerWorld world, WireBlock wireBlock) {
		this.world = world;
		this.wireBlock = wireBlock;
		this.minPower = this.wireBlock.getMinPower();
		this.maxPower = this.wireBlock.getMaxPower();
		this.powerStep = this.wireBlock.getPowerStep();
		
		this.network = new ArrayList<>();
		this.nodes = new Long2ObjectOpenHashMap<>();
		this.powerChanges = new PowerQueue(this.minPower, this.maxPower);
		this.wirePositions = new HashSet<>();
		this.blockUpdates = new ArrayList<>();
		
		this.nodeCache = new Node[16];
		this.fillNodeCache(0, 16);
	}
	
	private Node getNode(BlockPos pos) {
		return nodes.get(pos.asLong());
	}
	
	private Node getOrAddNode(BlockPos pos) {
		Node node = getNode(pos);
		return node == null ? addNode(pos) : node;
	}
	
	private WireNode getOrAddWire(BlockPos pos) {
		Node node = getOrAddNode(pos);
		return node.isWire ? node.asWire() : null;
	}
	
	private Node addNode(BlockPos pos) {
		Node node = getNextNode(pos);
		return addNode(node);
	}
	
	private Node addNode(Node node) {
		nodes.put(node.pos.asLong(), node);
		return node;
	}
	
	/**
	 * Check the BlockState that occupies the given position. If it is
	 * a wire, then retrieve its WireNode. Otherwise, grab the next
	 * Node from the cache and update it.
	 */
	private Node getNextNode(BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (wireBlock.isOf(state)) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, true);
			
			if (wire != null) {
				wire.flowIn = 0;
				wire.flowOut = 0;
				wire.prepared = false;
				wire.inNetwork = false;
				
				return wire;
			}
		}
		
		return getNextNode().update(pos, state);
	}
	
	/**
	 * Grab the first unused Node from the cache. If all of the
	 * cache is already in use, increase it in size first.
	 */
	private Node getNextNode() {
		if (usedNodes == nodeCache.length) {
			increaseNodeCache();
		}
		
		return nodeCache[usedNodes++];
	}
	
	private void increaseNodeCache() {
		int oldSize = nodeCache.length;
		int newSize = 2 * oldSize;
		
		Node[] oldCache = nodeCache;
		nodeCache = new Node[newSize];
		
		for (int index = 0; index < oldSize; index++) {
			nodeCache[index] = oldCache[index];
		}
		
		fillNodeCache(oldSize, newSize);
	}
	
	private void fillNodeCache(int start, int end) {
		for (int index = start; index < end; index++) {
			nodeCache[index] = new Node(world, wireBlock);
		}
	}
	
	/**
	 * Whenever a redstone wire is placed, removed or updated, it
	 * evaluates its own power level. If it is in an invalid state,
	 * this method is called to make the required power changes.
	 * 
	 * <p>
	 * Power changes are done in the following 4 steps.
	 * 
	 * <p>
	 * <b>1. Build up the network</b>
	 * <br>
	 * Collect all the wires around the source wire that might need
	 * to change their power levels.
	 * 
	 * <p>
	 * <b>2. Find powered wires</b>
	 * <br>
	 * Find those wires in the network that receive redstone power
	 * from outside the network. This can come in 2 forms:
	 * <br>
	 * - Power from non-wire components (repeaters, torches, etc.).
	 * <br>
	 * - Power from wires that are not in the network.
	 * <br>
	 * These powered wires will then queue their power changes.
	 * 
	 * <p>
	 * <b>3. Let power flow</b>
	 * <br>
	 * Work through the queue of power changes. After each wire's
	 * power change, emit shape updates to neighboring blocks, then
	 * queue power changes for connected wires.
	 * 
	 * <p>
	 * <b>4. Update neighbors</b>
	 * <br>
	 * Emit block updates to neighbors of all wires that changed their
	 * power levels.
	 */
	public void updatePower(WireNode... wires) {
		// The profiler keeps track of how long various parts of the
		// algorithm take. It is only here for debugging purposes,
		// and is commented out in production.
		Profiler profiler = AlternateCurrentMod.createProfiler();
		profiler.start();
		
		// Build a network of wires that need power changes.
		profiler.push("build network");
		buildNetwork(wires);
		
		// Find those wires in the network that receive redstone
		// power from outside it. Remember that the power changes
		// for those wires are already queued here!
		profiler.swap("find powered wires");
		findPoweredWires(wires);
		
		// Once the powered wires have been found, the network is
		// no longer needed. In fact, it should be cleared before
		// block and shape updates are emitted, in case a different
		// network is updated that needs power changes.
		profiler.swap("clear network of " + network.size());
		network.clear();
		
		// The same goes for the Node map. Once block and shape
		// updates are emitted, BlockStates of neighboring blocks
		// could change and this map would no longer be an accurate
		// representation of the world.
		profiler.swap("clear nodes");
		nodes.clear();
		usedNodes = 0;
		
		// Since shape updates are emitted during the power changes
		// of a network, an instantaneous update chain could update
		// another network (or the same network in a different place),
		// leading to a second call to this method.
		// If that happens, we can simply exit here, since the power
		// changes required for this network will be integrated into
		// the already ongoing method call.
		if (!updatingPower) {
			// Carry out the power changes and emit shape updates.
			profiler.swap("let power flow");
			letPowerFlow();
			
			// Block updates are emitted after all the power changes
			// so duplicates block updates from multiple wires can be
			// prevented. This is done by adding all the positions that
			// should be updated to a Set. A LinkedHashSet is used so
			// that order is preserved.
			profiler.swap("condense block update queue");
			Collection<BlockPos> blockUpdateQueue = getBlockUpdateQueue();
			
			profiler.swap("clean up");
			wirePositions.clear();
			blockUpdates.clear();
			
			profiler.swap("update neighbors");
			dispatchBlockUpdates(blockUpdateQueue);
		}
		
		profiler.pop();
		profiler.end();
	}
	
	/**
	 * Build up a network of WireNodes that are likely to need power
	 * changes. 
	 * 
	 * @param sourceWire
	 *   the redstone wire that got the initial block update that
	 *   triggered the power changes.
	 */
	private void buildNetwork(WireNode... sourceWires) {
		for (WireNode wire : sourceWires) {
			// If the source wire is removed, its position is now occupied
			// by a different block.
			if (wire.removed) {
				addNode(wire.pos);
			} else {
				addNode(wire);
			}
			// For the source wire, no call to prepareForNetwork is
			// necessary. Those preparations were already done before the
			// call to updatePower.
			addToNetwork(wire, Directions.WEST);
		}
		
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			
			for (int iDir = 0; iDir < 4; iDir++) {
				for (BlockPos pos : wire.connections.out[iDir]) {
					WireNode connectedWire = getOrAddWire(pos);
					
					if (connectedWire != null && !connectedWire.inNetwork) {
						prepareForNetwork(connectedWire);
						findPower(connectedWire, false);
						
						if (!hasPrevPower(connectedWire)) {
							addToNetwork(connectedWire, iDir);
						}
					}
				}
			}
		}
	}
	
	private void addToNetwork(WireNode wire, int iDir) {
		network.add(wire);
		
		wire.flowOut = iDir;
		wire.inNetwork = true;
	}
	
	private void prepareForNetwork(WireNode wire) {
		if (!wire.prepared) {
			wire.prepared = true;
			
			if (!wire.removed && !wire.shouldBreak && !wire.state.canPlaceAt(world, wire.pos)) {
				wire.shouldBreak = true;
			}
			
			findPreliminaryPower(wire);
		}
	}
	
	private void findPreliminaryPower(WireNode wire) {
		wire.virtualPower = wire.externalPower = (wire.removed || wire.shouldBreak) ? minPower : getExternalPower(wire);
	}
	
	private int getExternalPower(WireNode wire) {
		int power = minPower;
		
		for (Direction dir : Directions.ALL) {
			power = Math.max(power, getExternalPowerFrom(wire, dir));
			
			if (power >= maxPower) {
				return maxPower;
			}
		}
		
		return power;
	}
	
	private int getExternalPowerFrom(WireNode wire, Direction dir) {
		int power = minPower;
		
		BlockPos side = wire.pos.offset(dir);
		Node neighbor = getOrAddNode(side);
		
		if (neighbor.isSolidBlock) {
			power = Math.max(power, getStrongPowerTo(neighbor.pos, dir.getOpposite()));
		}
		if (neighbor.isRedstoneComponent) {
			power = Math.max(power, neighbor.state.getWeakRedstonePower(world, neighbor.pos, dir));
		}
		
		return power;
	}
	
	private int getStrongPowerTo(BlockPos pos, Direction ignore) {
		int power = minPower;
		
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			Node neighbor = getOrAddNode(side);
			
			if (neighbor.isRedstoneComponent) {
				power = Math.max(power, neighbor.state.getStrongRedstonePower(world, side, dir));
				
				if (power >= maxPower) {
					return maxPower;
				}
			}
		}
		
		return power;
	}
	
	private void findPower(WireNode wire, boolean ignoreNetwork) {
		if (!wire.removed && !wire.shouldBreak && wire.virtualPower < maxPower) {
			wire.virtualPower = wire.externalPower;
			wire.flowIn = 0;
			
			findWirePower(wire, ignoreNetwork);
		}
	}
	
	private void findWirePower(WireNode wire, boolean ignoreNetwork) {
		for (int iDir = 0; iDir < 4; iDir++) {
			for (BlockPos pos : wire.connections.in[iDir]) {
				WireNode connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null && (!ignoreNetwork || !connectedWire.inNetwork) && connectedWire.virtualPower >= (minPower + powerStep)) {
					wire.offerPower(connectedWire.virtualPower - powerStep, (iDir + 2) & 0b11);
				}
			}
		}
	}
	
	private void findPowerFlow(WireNode wire) {
		int flowOut = FLOW_IN_TO_FLOW_OUT[wire.flowIn];
		
		if (flowOut >= 0) {
			wire.flowOut = flowOut;
		}
	}
	
	private boolean hasPrevPower(WireNode wire) {
		if (wire.virtualPower == wire.prevPower) {
			return true;
		}
		
		return wire.virtualPower < minPower && wire.prevPower == minPower;
	}
	
	private void findPoweredWires(WireNode... sourceWires) {
		for (WireNode wire : sourceWires) {
			findPower(wire, true);
			queuePowerChange(wire);
		}
		
		for (int index = sourceWires.length; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findPower(wire, true);
			
			if (wire.virtualPower > minPower) {
				queuePowerChange(wire);
			}
		}
	}
	
	private void queuePowerChange(WireNode wire) {
		if (hasPrevPower(wire)) {
			findPowerFlow(wire);
			transmitPower(wire);
		} else {
			addPowerChange(wire);
		}
	}
	
	private void addPowerChange(WireNode wire) {
		powerChanges.add(wire);
	}
	
	private void transmitPower(WireNode wire) {
		int nextPower = wire.virtualPower - powerStep;
		
		for (int iDir : UPDATE_ORDERS[wire.flowOut]) {
			for (BlockPos pos : wire.connections.out[iDir]) {
				WireNode connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null && connectedWire.offerPower(nextPower, iDir)) {
					queuePowerChange(connectedWire);
				}
			}
		}
	}
	
	private void letPowerFlow() {
		updatingPower = true;
		
		while (!powerChanges.isEmpty()) {
			WireNode wire = powerChanges.poll();
			
			if (hasPrevPower(wire)) {
				continue;
			}
			
			wirePositions.add(wire.pos);
			findPowerFlow(wire);
			
			if (updateWireState(wire) && !wire.removed) {
				queueBlockUpdates(wire);
				dispatchShapeUpdates(wire);
			}
			
			transmitPower(wire);
		}
		
		updatingPower = false;
	}
	
	private boolean updateWireState(WireNode wire) {
		if (wire.removed) {
			return true;
		}
		if (wire.shouldBreak) {
			return wireBlock.breakBlock(world, wire.pos, wire.state, Block.NOTIFY_LISTENERS);
		}
		
		return wireBlock.setPower(world, wire.pos, wire.state, wire.virtualPower, Block.FORCE_STATE | Block.NOTIFY_LISTENERS);
	}
	
	private void dispatchShapeUpdates(WireNode wire) {
		BlockPos pos = wire.pos;
		BlockState state = wire.state;
		
		for (Direction dir : Directions.ALL) {
			BlockPos side = pos.offset(dir);
			BlockState prevState = world.getBlockState(side);
			
			// Shape updates to redstone wires are super expensive
			// and should never happen as a result of power changes
			// anyway.
			if (!wireBlock.isOf(prevState)) {
				BlockState newState = prevState.getStateForNeighborUpdate(dir.getOpposite(), state, world, side, pos);
				Block.replace(prevState, newState, world, side, 2);
			}
		}
	}
	
	private void queueBlockUpdates(WireNode wire) {
		BlockUpdateEntry entry = new BlockUpdateEntry(wire.pos, wire.flowOut);
		blockUpdates.add(entry);
	}
	
	private Collection<BlockPos> getBlockUpdateQueue() {
		Set<BlockPos> queue = new LinkedHashSet<>();
		
		for (int index = blockUpdates.size() - 1; index >= 0; index--) {
			BlockUpdateEntry entry = blockUpdates.get(index);
			collectNeighborPositions(queue, entry.pos, entry.flowDir);
		}
		queue.removeAll(wirePositions);
		
		return queue;
	}
	
	private void dispatchBlockUpdates(Collection<BlockPos> blockUpdates) {
		Block block = wireBlock.asBlock();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, block, pos);
		}
	}
	
	public static void collectNeighborPositions(Collection<BlockPos> p, BlockPos o) {
		collectNeighborPositions(p, o, Directions.WEST);
	}
	
	/**
	 * Collect all neighboring positions of the given position (the "origin").
	 * The order in which these are added follows 3 rules:
	 * <br>
	 * 1. Add positions in order of their distance from the origin.
	 * <br>
	 * 2. use the following basic order: { front, back, right, left, down, up }.
	 *    This order was chosen because it results in the following order when
	 *    west is considered forwards: { west, east, north, south, down, up },
	 *    which is the order of shape updates. The vertical directions are added
	 *    after the cardinal directions, rather than "in between", so as to
	 *    eliminate some directionality issues.
	 * <br>
	 * 3. Every pair of positions are "opposites" (relative to the origin).
	 * 
	 * @param p     the collection to which the neighboring positions should be added
	 * @param o     the origin
	 * @param iDir  the index of the cardinal direction that is to be considered forward
	 */
	public static void collectNeighborPositions(Collection<BlockPos> p, BlockPos o, int iDir) {
		Direction forward   = Directions.HORIZONTAL[ iDir            ];
		Direction rightward = Directions.HORIZONTAL[(iDir + 1) & 0b11];
		Direction backward  = Directions.HORIZONTAL[(iDir + 2) & 0b11];
		Direction leftward  = Directions.HORIZONTAL[(iDir + 3) & 0b11];
		Direction downward  = Direction.DOWN;
		Direction upward    = Direction.UP;
		
		BlockPos front = o.offset(forward);
		BlockPos right = o.offset(rightward);
		BlockPos back  = o.offset(backward);
		BlockPos left  = o.offset(leftward);
		BlockPos below = o.offset(downward);
		BlockPos above = o.offset(upward);
		
		// direct neighbors (6)
		p.add(front);
		p.add(back);
		p.add(right);
		p.add(left);
		p.add(below);
		p.add(above);
		
		// diagonal neighbors (12)
		p.add(front.offset(rightward));
		p.add(back .offset(leftward));
		p.add(front.offset(leftward));
		p.add(back .offset(rightward));
		p.add(front.offset(downward));
		p.add(back .offset(upward));
		p.add(front.offset(upward));
		p.add(back .offset(downward));
		p.add(right.offset(downward));
		p.add(left .offset(upward));
		p.add(right.offset(upward));
		p.add(left .offset(downward));
		
		// far neighbors (6)
		p.add(front.offset(forward));
		p.add(back .offset(backward));
		p.add(right.offset(rightward));
		p.add(left .offset(leftward));
		p.add(below.offset(downward));
		p.add(above.offset(upward));
		
		// total: 24
	}
	
	private class BlockUpdateEntry {
		
		private final BlockPos pos;
		private final int flowDir;
		
		public BlockUpdateEntry(BlockPos pos, int flowDir) {
			this.pos = pos;
			this.flowDir = flowDir;
		}
	}
}
