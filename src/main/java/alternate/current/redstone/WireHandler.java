package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import alternate.current.AlternateCurrentMod;
import alternate.current.utils.Directions;
import alternate.current.utils.profiler.Profiler;

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
 * in which it updates, combined with the location-dependant order in
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
 * And there is more! Because we calculate the power of the entire
 * network, sending block and shape updates to the wires in it is
 * redundant. Removing those updates can reduce the number of block and
 * shape updates by up to 20%.
 * 
 * <p>
 * 3.
 * To make the order of block updates to neighbors of a network
 * deterministic, the first thing we must do is to replace the location-
 * dependant order in which a wire updates its neighbors. The order I have
 * chosen to use can be seen at the bottom of this class.
 * 
 * <p>
 * Next, we make the order of block updates to neighbors of a network as
 * a whole depend on the order in which the wires in it change their power
 * levels. For the sake of breaking as few redstone contraptions as
 * possible, I have chosen to update neighbors around wires in the nework
 * in reverse of the order in which they change their power levels.
 * 
 * <p>
 * The order in which wires change their power levels depends on the
 * position of power sources. Wires that change to a higher power level
 * update first. If multiple wires change to the same power level, they
 * update in the order in which the power changes were queued. While this
 * does introduce some direction-dependency, most redstone components
 * already exibit some direction-dependency, so I do not see this as an
 * issue.
 * 
 * @author Space Walker
 */
public class WireHandler {
	
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
	
	/** List of all the wires in the network */
	private final List<WireNode> network;
	/** Map of wires and neighboring blocks */
	private final Long2ObjectMap<Node> nodes;
	/** Queue of all the power changes that need to happen */
	private final PriorityQueue<WireNode> powerChanges;
	/** List of positions that contain a wire that is part of the network */
	private final List<BlockPos> allWires;
	/** List of wires that changed power level */
	private final List<BlockPos> updatedWires;
	
	// Rather than creating new nodes every time a network is updated
	// we keep a cache of nodes that can be re-used.
	private Node[] nodeCache;
	private int usedNodes;
	// Each WireNode is given a "ticket" number that, together with its
	// power level, determines its place in the queue to change power.
	private int nextTicket;
	
	public WireHandler(ServerWorld world, WireBlock wireBlock) {
		this.world = world;
		this.wireBlock = wireBlock;
		this.minPower = this.wireBlock.getMinPower();
		this.maxPower = this.wireBlock.getMaxPower();
		this.powerStep = this.wireBlock.getPowerStep();
		
		this.network = new ArrayList<>();
		this.nodes = new Long2ObjectOpenHashMap<>();
		this.powerChanges = new PriorityQueue<>();
		this.allWires = new ArrayList<>();
		this.updatedWires = new ArrayList<>();
		
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
				wire.ticket = -1;
				wire.prepared = false;
				wire.inNetwork = false;
				
				return wire;
			}
		}
		
		return nextNodeFromCache().update(pos, state);
	}
	
	/**
	 * Grab the first unused Node from the cache. If all of the
	 * cache is already in use, increase it in size, then grab the
	 * first unused Node.
	 */
	private Node nextNodeFromCache() {
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
	public void updatePower(WireNode wire) {
		// The profiler keeps track of how long various parts of the
		// algorithm take. It is only here for debugging purposes,
		// and is commented out in production.
		Profiler profiler = AlternateCurrentMod.createProfiler();
		profiler.start();
		
		profiler.push("build network");
		buildNetwork(wire);
		
		// Find those wires in the network that receive redstone
		// power from outside it. Remember that the power changes
		// for those wires are already queued here!
		profiler.swap("find powered wires");
		findPoweredWires(wire);
		
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
		if (powerChanges.isEmpty()) {
			profiler.swap("let power flow");
			letPowerFlow();
			nextTicket = 0;
			
			// Block updates are emitted after all the power changes
			// so duplicates block updates from multiple wires can be
			// prevented. This is done by adding all the positions that
			// should be updated to a LinkedHashSet.
			profiler.swap("queue block updates");
			Collection<BlockPos> blockUpdates = queueBlockUpdates();
			
			profiler.swap("clear wire lists");
			allWires.clear();
			updatedWires.clear();
			
			profiler.swap("update neighbors");
			dispatchBlockUpdates(blockUpdates);
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
	private void buildNetwork(WireNode sourceWire) {
		// The source wire might be removed, in which case its position
		// is now occupied by another block (most likely air).
		addNode(sourceWire.pos);
		// For the source wire, no call to prepareForNetwork is
		// necessary. Those preparations were already done before
		// the call to updatePower.
		addToNetwork(sourceWire);
		
		int minDepth = getMinUpdateDepth(sourceWire);
		int nextLayer = network.size();
		
		for (int index = 0; index < network.size(); index++) {
			if (index == nextLayer) {
				minDepth--;
				nextLayer = network.size();
			}
			
			WireNode wire = network.get(index);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null && !connectedWire.inNetwork) {
					prepareForNetwork(connectedWire);
					findPower(connectedWire, false);
					
					if (minDepth > 0 || !hasPrevPower(connectedWire)) {
						addToNetwork(connectedWire);
					}
				}
			}
		}
	}
	
	private void addToNetwork(WireNode wire) {
		network.add(wire);
		wire.inNetwork = true;
	}
	
	private int getMinUpdateDepth(WireNode sourceWire) {
		if (sourceWire.virtualPower < sourceWire.prevPower) {
			return (sourceWire.prevPower - minPower) / powerStep + 2;
		}
		
		return 0;
	}
	
	private void prepareForNetwork(WireNode wire) {
		if (!wire.prepared) {
			if (!wire.removed && !wire.shouldBreak && !wire.state.canPlaceAt(world, wire.pos)) {
				wire.shouldBreak = true;
			}
			
			wire.prevPower = wireBlock.getPower(world, wire.pos, wire.state);
			wire.virtualPower = wire.externalPower = getPreliminaryPower(wire);
			
			wire.prepared = true;
		}
	}
	
	private int getPreliminaryPower(WireNode wire) {
		if (wire.removed || wire.shouldBreak) {
			return minPower;
		}
		
		return getExternalPower(wire);
	}
	
	private int getExternalPower(WireNode wire) {
		int power = minPower;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = wire.pos.offset(dir);
			Node neighbor = getOrAddNode(side);
			
			if (neighbor.isWire) {
				continue;
			}
			
			if (neighbor.isSolidBlock) {
				power = Math.max(power, getStrongPowerTo(neighbor.pos, dir.getOpposite()));
			}
			if (neighbor.isRedstoneComponent) {
				power = Math.max(power, neighbor.state.getWeakRedstonePower(world, neighbor.pos, dir));
			}
			
			if (power >= maxPower) {
				return maxPower;
			}
		}
		
		return power;
	}
	
	private int getStrongPowerTo(BlockPos pos, Direction ignore) {
		int power = minPower;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			
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
		if (!wire.removed && !wire.shouldBreak) {
			wire.virtualPower = wire.externalPower;
			
			if (wire.virtualPower < maxPower) {
				wire.virtualPower = Math.max(wire.virtualPower, getWirePower(wire, ignoreNetwork));
			}
		}
	}
	
	private int getWirePower(WireNode wire, boolean ignoreNetwork) {
		int power = minPower;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getOrAddWire(pos);
			
			if (connectedWire != null && (!ignoreNetwork || !connectedWire.inNetwork)) {
				power = Math.max(power, connectedWire.virtualPower - powerStep);
			}
		}
		
		return power;
	}
	
	private boolean hasPrevPower(WireNode wire) {
		return wire.virtualPower == wire.prevPower;
	}
	
	private void findPoweredWires(WireNode sourceWire) {
		findPower(sourceWire, true);
		queuePowerChanges(sourceWire);
		
		for (int index = 1; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findPower(wire, true);
			
			if (wire.virtualPower > minPower) {
				queuePowerChanges(wire);
			}
		}
	}
	
	private void queuePowerChanges(WireNode wire) {
		if (hasPrevPower(wire)) {
			transmitPower(wire);
		} else {
			addPowerChange(wire);
		}
	}
	
	private void addPowerChange(WireNode wire) {
		powerChanges.add(wire);
		
		if (wire.ticket < 0) {
			wire.ticket = nextTicket++;
		}
	}
	
	private void transmitPower(WireNode wire) {
		int nextPower = wire.virtualPower - powerStep;
		
		for (BlockPos pos : wire.connectionsOut) {
			WireNode connectedWire = wireBlock.getWire(world, pos);
			
			if (connectedWire != null && acceptsPower(connectedWire, nextPower)) {
				connectedWire.virtualPower = nextPower;
				queuePowerChanges(connectedWire);
			}
		}
	}
	
	private void letPowerFlow() {
		while (!powerChanges.isEmpty()) {
			WireNode wire = powerChanges.poll();
			
			if (hasPrevPower(wire)) {
				continue;
			}
			
			allWires.add(wire.pos);
			
			if (updateWireState(wire)) {
				updatedWires.add(wire.pos);
				
				if (!wire.removed) {
					dispatchShapeUpdates(wire);
				}
			}
			
			transmitPower(wire);
		}
	}
	
	private boolean acceptsPower(WireNode wire, int power) {
		if (power > wire.virtualPower) {
			return true;
		}
		
		return wire.virtualPower == minPower;
	}
	
	private boolean updateWireState(WireNode wire) {
		if (wire.removed) {
			return true;
		}
		if (wire.shouldBreak) {
			return wireBlock.breakBlock(world, wire.pos, wire.state, 2);
		}
		
		return wireBlock.setPower(world, wire.pos, wire.state, wire.virtualPower, 18);
	}
	
	private void dispatchShapeUpdates(WireNode wire) {
		BlockPos wirePos = wire.pos;
		BlockState wireState = wire.state;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = wirePos.offset(dir);
			BlockState prevState = world.getBlockState(side);
			
			// Shape updates to redstone wires are super expensive
			// and should never happen as a result of power changes
			// anyway.
			if (!wireBlock.isOf(prevState)) {
				BlockState newState = prevState.getStateForNeighborUpdate(dir.getOpposite(), wireState, world, side, wirePos);
				Block.replace(prevState, newState, world, side, 2);
			}
		}
	}
	
	private Collection<BlockPos> queueBlockUpdates() {
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			collectNeighborPositions(updatedWires.get(index), blockUpdates);
		}
		blockUpdates.removeAll(allWires);
		
		return blockUpdates;
	}
	
	private void dispatchBlockUpdates(Collection<BlockPos> blockUpdates) {
		Block block = wireBlock.asBlock();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, block, pos);
		}
	}
	
	/**
	 * Collect all neighboring positions of the given position (the "origin").
	 * The order in which these are added follows 3 rules:
	 * 
	 * 1. add positions in order of their distance from the origin
	 * 2. use the following basic order: WEST -> EAST -> NORTH -> SOUTH -> DOWN -> UP
	 * 3. every pair of positions are "opposites" (relative to the origin)
	 * 
	 * @param pos        the origin
	 * @param positions  the collection to which the neighboring positions should be added
	 */
	public static void collectNeighborPositions(BlockPos pos, Collection<BlockPos> positions) {
		BlockPos west = pos.west();
		BlockPos east = pos.east();
		BlockPos north = pos.north();
		BlockPos south = pos.south();
		BlockPos down = pos.down();
		BlockPos up = pos.up();
		
		// Direct neighbors (6)
		positions.add(west);
		positions.add(east);
		positions.add(north);
		positions.add(south);
		positions.add(down);
		positions.add(up);
		
		// Diagonal neighbors (12)
		positions.add(west.north());
		positions.add(east.south());
		positions.add(west.south());
		positions.add(east.north());
		positions.add(west.down());
		positions.add(east.up());
		positions.add(west.up());
		positions.add(east.down());
		positions.add(north.down());
		positions.add(south.up());
		positions.add(north.up());
		positions.add(south.down());
		
		// Neighbors 2 out in each direction (6)
		positions.add(west.west());
		positions.add(east.east());
		positions.add(north.north());
		positions.add(south.south());
		positions.add(down.down());
		positions.add(up.up());
		
		// Total: 24
	}
}
