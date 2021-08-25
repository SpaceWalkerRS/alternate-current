package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Queue;

import alternate.current.AlternateCurrentMod;
import alternate.current.util.BlockUtil;
import alternate.current.util.profiler.Profiler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
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
 * Redstone wire updates recursively and each wire calculates its power
 * level in isolation rather than in the context of the network it is a
 * part of. This means a wire in a grid can change its power level over
 * half a dozen times before settling on its final value. This problem
 * used to be worse in 1.14 and below, where a wire would only decrease
 * its power level by 1 at a time.
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
 * Redstone wire in Vanilla also fails on point 3, though this is more of
 * a quality-of-life issue than a lag issue. The recursive nature in which
 * it updates, combined with the location-dependent order in which each
 * wire updates its neighbors, makes the order in which neighbors of a
 * wire network are updated incredibly inconsistent and seemingly random.
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
 *   updates, so each wire only emits 24 block updates and 6 shape updates
 *   whenever it changes its power level.
 * <br>
 * - Only emit block updates and shape updates once a wire reaches its
 *   final power level, rather than at each intermediary stage. 
 * <br>
 * For an individual wire, these two optimizations are the best you can
 * do, but for an entire grid, you can do better!
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
 * dependent order in which a wire updates its neighbors. Instead, we
 * base it on the direction of power flow. This part of the algorithm
 * was heavily inspired by theosib's 'RedstoneWireTurbo', which you can
 * read more about in his comment on Mojira
 * <a href="https://bugs.mojang.com/browse/MC-81098?focusedCommentId=420777&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-420777">here</a>
 * or by checking out its implementation in carpet mod
 * <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/helpers/RedstoneWireTurbo.java">here</a>.
 * 
 * <p>
 * The idea is to determine the direction of power flow through a wire
 * based on the power it receives from neighboring wires. For example, if
 * the only power a wire receives is from a neighboring wire to its west,
 * it can be said that the direction of power flow is east. 
 * 
 * <p>
 * We make order of block updates to neighbors of a wire depend on what
 * is determined to be the direction of power flow. This not only removes
 * locationality entirely, it even removes directionality in a large
 * number of cases. Unlike in 'RedstoneWireTurbo', however, I have decided
 * to keep a directional element in ambiguous cases, rather than to 
 * introduce randomness.
 * 
 * <p>
 * While this change fixes the block update order of individual wires,
 * we must still address the overall block update order of a network. This
 * turns out to be a simple fix, because of a change we made earlier: we
 * search through the network for wires that receive power from outside it,
 * and spread the power from there. If we make each wire transmit its power
 * to neighboring wires in an order dependent on the direction of power
 * flow, we end up with a non-locational and largely non-directional wire
 * update order.
 * 
 * @author Space Walker
 */
public class WireHandler {
	
	public static class Directions {
		
		public static final Direction[] ALL        = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.DOWN, Direction.UP };
		public static final Direction[] HORIZONTAL = { Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH };
		
		// Indices for the 'ALL' and 'HORIZONTAL' arrays above
		// The cardinal directions are ordered clockwise. This allows
		// conversion between relative and absolute directions
		// ('left' 'right' vs 'east' 'west') with simple arithmetic.
		// If some Direction index 'iDir' is considered 'forward', then
		// '(iDir + 1) % 4' is 'right', '(iDir + 2) % 4' is 'backward', etc.
		public static final int WEST  = 0;
		public static final int NORTH = 1;
		public static final int EAST  = 2;
		public static final int SOUTH = 3;
		public static final int DOWN  = 4;
		public static final int UP    = 5;
		
	}
	
	/**
	 * This conversion table takes in information about incoming flow, and
	 * outputs the determined outgoing flow, or -1 for ambiguous cases.
	 * 
	 * <p>
	 * The input is a 4 bit number that encodes the incoming flow. Each bit
	 * represents a cardinal direction, and when it is 'on', there is flow
	 * in that direction.
	 * 
	 * <p>
	 * The output is a single Direction index.
	 * 
	 * <p>
	 * The outgoing flow is determined as follows:
	 * 
	 * <p>
	 * If there is just 1 direction of incoming flow, that direction will
	 * be the direction of outgoing flow.
	 * 
	 * <p>
	 * If there are 2 directions of incoming flow, and these directions are
	 * not each other's opposites, the direction that is 'more clockwise'
	 * will be the direction of outgoing flow. More precisely, the
	 * direction that is 1 clockwise turn from the other is picked.
	 * 
	 * <p>
	 * If there are 3 directions of incoming flow, the two opposing
	 * directions cancel each other out, and the remaining direction will
	 * be the direction of outgoing flow.
	 * 
	 * <p>
	 * In all other cases, the flow is completely ambiguous.
	 */
	public static final int[] FLOW_IN_TO_FLOW_OUT = {
		-1, // 0b0000: -                      -> x
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
		-1, // 0b1111: west/north/east/south  -> x
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
	
	private int rootCount;
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
	public void updatePower(WireNode wire) {
		// In the simple case of a single dot changing power it is
		// not necessary to build up an entire network first.
		if (wire.connections.count == 0) {
			if (updateWireState(wire) && !wire.removed) {
				dispatchShapeUpdates(wire);
			}
			
			List<BlockPos> neighbors = new ArrayList<>();
			collectNeighborPositions(neighbors, wire.pos);
			dispatchBlockUpdates(neighbors);
			
			return;
		}
		
		// The profiler keeps track of how long various parts of the
		// algorithm take. It is only here for debugging purposes,
		// and is commented out in production.
		Profiler profiler = AlternateCurrentMod.createProfiler();
		profiler.start();
		
		// Collect wires around the source wire that are in an
		// illegal state. 
		profiler.push("collect roots");
		findRoots(wire);
		
		// Build a network of wires that need power changes. This 
		// includes the roots as well as any wires that will be
		// affected by power changes to those roots.
		profiler.swap("build network");
		buildNetwork();
		
		// Find those wires in the network that receive redstone power
		// from outside it. Remember that the power changes for those
		// wires are already queued here!
		profiler.swap("find powered wires");
		findPoweredWires();
		
		// Once the powered wires have been found, the network is
		// no longer needed. In fact, it should be cleared before
		// block and shape updates are emitted, in case a different
		// network is updated that needs power changes.
		profiler.swap("clear " + rootCount + " roots and network of " + network.size());
		rootCount = 0;
		network.clear();
		
		// The same goes for the Node map. Once block and shape
		// updates are emitted, BlockStates of neighboring blocks
		// could change and this map would no longer be an accurate
		// representation of the world.
		profiler.swap("clear nodes");
		usedNodes = 0;
		nodes.clear();
		
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
			// so duplicate block updates from multiple wires can be
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
	 * Find the wires around the source wire that are in an illegal
	 * state and need power changes. This catches some common cases
	 * where multiple wires are (de)powered at once.
	 */
	private void findRoots(WireNode wire) {
		// No need to call tryAddRoot, since the preparations done
		// there were done before the call to updatePower for the
		// source wire.
		addRootToNetwork(wire);
		
		if (!wire.removed) {
			addNode(wire);
			
			for (Direction dir : Directions.ALL) {
				BlockPos side = wire.pos.offset(dir);
				Node neighbor = getOrAddNode(side);
				
				if (neighbor.emitsWeakPowerTo(dir)) {
					// Any redstone component that toggled on/off could
					// (de)power multiple wires at once.
					findRootsAroundRedstone(neighbor, dir.getOpposite());
				} else if (neighbor.isSolidBlock || neighbor.state.isAir() || neighbor.state.isOf(Blocks.MOVING_PISTON)) {
					// When blocks are moved by pistons, multiple wires
					// could be (de)powered at once.
					findRootsAround(neighbor, dir.getOpposite(), neighbor.isSolidBlock);
				}
			}
		}
	}
	
	/**
	 * Look for wires around this node that are in an illegal state
	 * and need power changes.
	 */
	private void findRootsAround(Node node, Direction ignore, boolean checkRedstone) {
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = node.pos.offset(dir);
			Node neighbor = getOrAddNode(side);
			
			if (neighbor.isWire) {
				WireNode wire = neighbor.asWire();
				
				if (!wire.prepared) {
					tryAddRoot(wire);
				}
			} else if (checkRedstone && neighbor.emitsStrongPowerTo(dir)) {
				findRootsAroundRedstone(neighbor, dir.getOpposite());
			}
		}
	}
	
	/**
	 * Look for wires powered by this redstone component that are
	 * in an illegal state and need power changes.
	 */
	private void findRootsAroundRedstone(Node redstoneNode, Direction ignore) {
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			Direction opp = dir.getOpposite();
			
			boolean weak = redstoneNode.emitsWeakPowerTo(opp);
			boolean strong = redstoneNode.emitsStrongPowerTo(opp);
			
			if (!weak && !strong) {
				continue;
			}
			
			BlockPos side = redstoneNode.pos.offset(dir);
			Node neighbor = getOrAddNode(side);
			
			if (weak && neighbor.isWire) {
				WireNode wire = neighbor.asWire();
				
				if (!wire.prepared) {
					tryAddRoot(wire);
				}
			} else if (strong && neighbor.isSolidBlock) {
				findRootsAround(neighbor, opp, false);
			}
		}
	}
	
	/**
	 * Build up a network of WireNodes that need power changes.
	 * This includes the roots that were already added and any
	 * wires powered by those roots that will need power changes
	 * as a result of power changes to the roots.
	 */
	private void buildNetwork() {
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			
			for (int iDir = 0; iDir < 4; iDir++) {
				for (BlockPos pos : wire.connections.out[iDir]) {
					WireNode connectedWire = getOrAddWire(pos);
					
					if (connectedWire != null && !connectedWire.inNetwork) {
						prepareForNetwork(connectedWire);
						findPower(connectedWire, false);
						
						if (needsPowerChange(connectedWire)) {
							addToNetwork(connectedWire, iDir);
						}
					}
				}
			}
		}
	}
	
	private void tryAddRoot(WireNode wire) {
		prepareForNetwork(wire);
		findPower(wire, false);
		
		if (needsPowerChange(wire)) {
			addRootToNetwork(wire);
		}
	}
	
	private void addRootToNetwork(WireNode wire) {
		addToNetwork(wire, 0);
		rootCount++;
	}
	
	private void addToNetwork(WireNode wire, int backupFlow) {
		network.add(wire);
		
		wire.flowOut = backupFlow;
		wire.inNetwork = true;
	}
	
	private void prepareForNetwork(WireNode wire) {
		if (!wire.prepared) {
			wire.prepared = true;
			
			if (!wire.removed && !wire.shouldBreak && wireBlock.shouldBreak(world, wire.pos, wire.state)) {
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
		} else if (wire.connections.flow >= 0) {
			wire.flowOut = wire.connections.flow;
		}
	}
	
	private boolean needsPowerChange(WireNode wire) {
		if (wire.currentPower == minPower) {
			return wire.virtualPower > minPower;
		}
		
		return wire.virtualPower != wire.currentPower;
	}
	
	private void findPoweredWires() {
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findPower(wire, true);
			
			if (index < rootCount || wire.virtualPower > minPower) {
				queuePowerChange(wire);
			}
		}
	}
	
	private void queuePowerChange(WireNode wire) {
		if (needsPowerChange(wire)) {
			powerChanges.add(wire);
		} else {
			findPowerFlow(wire);
			transmitPower(wire);
		}
	}
	
	private void transmitPower(WireNode wire) {
		int nextPower = wire.virtualPower - powerStep;
		
		for (int offset = 0; offset < 4; offset++) {
			int iDir = (wire.flowOut + offset) & 0b11;
			
			for (BlockPos pos : wire.connections.out[iDir]) {
				WireNode connectedWire = wireBlock.getOrCreateWire(world, pos, true);
				
				if (connectedWire != null && !connectedWire.removed && !connectedWire.shouldBreak && connectedWire.offerPower(nextPower, iDir)) {
					queuePowerChange(connectedWire);
				}
			}
		}
	}
	
	private void letPowerFlow() {
		updatingPower = true;
		
		while (!powerChanges.isEmpty()) {
			WireNode wire = powerChanges.poll();
			
			if (!needsPowerChange(wire)) {
				continue;
			}
			
			wirePositions.add(wire.pos);
			findPowerFlow(wire);
			
			if (updateWireState(wire)) {
				queueBlockUpdates(wire);
				
				if (!wire.removed) {
					dispatchShapeUpdates(wire);
				}
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
		for (Direction dir : BlockUtil.DIRECTIONS) {
			BlockPos side = wire.pos.offset(dir);
			BlockState prevState = world.getBlockState(side);
			
			// Shape updates to redstone wires are super expensive
			// and should never happen as a result of power changes
			// anyway.
			if (!wireBlock.isOf(prevState)) {
				BlockState newState = prevState.getStateForNeighborUpdate(dir.getOpposite(), wire.state, world, side, wire.pos);
				Block.replace(prevState, newState, world, side, Block.NOTIFY_LISTENERS);
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
		collectNeighborPositions(p, o, 0);
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
