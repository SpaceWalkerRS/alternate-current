package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;

//import alternate.current.AlternateCurrentMod;
import alternate.current.util.BlockUtil;
//import alternate.current.util.profiler.Profiler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.block.BlockState;
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
 * if a wire changes its connections, but not when it changes its power
 * level.
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
 * read more about in theosib's comment on Mojira
 * <a href="https://bugs.mojang.com/browse/MC-81098?focusedCommentId=420777&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-420777">here</a>
 * or by checking out its implementation in carpet mod
 * <a href="https://github.com/gnembon/fabric-carpet/blob/master/src/main/java/carpet/helpers/RedstoneWireTurbo.java">here</a>.
 * 
 * <p>
 * The idea is to determine the direction of power flow through a wire
 * based on the power it receives from neighboring wires. For example, if
 * the only power a wire receives is from a neighboring wire to its west,
 * it can be said that the direction of power flow through the wire is east. 
 * 
 * <p>
 * We make the order of block updates to neighbors of a wire depend on what
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
		
		// Indices for the 'ALL' and 'HORIZONTAL' arrays above.
		// The cardinal directions are ordered clockwise. This allows
		// for conversion between relative and absolute directions
		// ('left' 'right' vs 'east' 'west') with simple arithmetic:
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
	/**
	 * Update order of cardinal directions. Given that the index is
	 * to be considered the direction that is 'forward', the resulting
	 * update order is { front, back, right, left }.
	 */
	private static final int[][] UPDATE_ORDER = {
		{ 0, 2, 1, 3 },
		{ 1, 3, 2, 0 },
		{ 2, 0, 3, 1 },
		{ 3, 1, 0, 2 }
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
	private final WireBlock wireBlock;
	private final WorldAccess world;
	private final int minPower;
	private final int maxPower;
	private final int powerStep;
	
	/** All the wires in the network */
	private final List<WireNode> network;
	/** Map of wires and neighboring blocks */
	private final Long2ObjectMap<Node> nodes;
	/** All the power changes that need to happen */
	private final Queue<WireNode> powerChanges;
	
	private int rootCount;
	// Rather than creating new nodes every time a network is updated
	// we keep a cache of nodes that can be re-used.
	private Node[] nodeCache;
	private int usedNodes;
	
	private boolean updatingPower;
	
	public WireHandler(WireBlock wireBlock, WorldAccess world) {
		this.wireBlock = wireBlock;
		this.world = world;
		this.minPower = this.wireBlock.getMinPower();
		this.maxPower = this.wireBlock.getMaxPower();
		this.powerStep = this.wireBlock.getPowerStep();
		
		this.network = new ArrayList<>();
		this.nodes = new Long2ObjectOpenHashMap<>();
		this.powerChanges = new PowerQueue(this.minPower, this.maxPower);
		
		this.nodeCache = new Node[16];
		this.fillNodeCache(0, 16);
	}
	
	private Node getOrAddNode(BlockPos pos) {
		return nodes.computeIfAbsent(pos.asLong(), key -> getNextNode(pos));
	}
	
	private WireNode getOrAddWire(BlockPos pos) {
		Node node = getOrAddNode(pos);
		return node.isWire() ? node.asWire() : null;
	}
	
	private void cleanUp() {
		usedNodes = 0;
		nodes.clear();
	}
	
	/**
	 * Check the BlockState that occupies the given position. If it is
	 * a wire, then retrieve its WireNode. Otherwise, grab the next
	 * Node from the cache and update it.
	 */
	private Node getNextNode(BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (wireBlock.isOf(state)) {
			WireNode wire = world.getWire(pos, true, true);
			
			if (wire != null) {
				wire.state = state;
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
		Node[] oldCache = nodeCache;
		nodeCache = new Node[oldCache.length << 1];
		
		for (int index = 0; index < oldCache.length; index++) {
			nodeCache[index] = oldCache[index];
		}
		
		fillNodeCache(oldCache.length, nodeCache.length);
	}
	
	private void fillNodeCache(int start, int end) {
		for (int index = start; index < end; index++) {
			nodeCache[index] = new Node(wireBlock, world);
		}
	}
	
	/**
	 * This method is called whenever a redstone wire receives a
	 * block update.
	 */
	public void onWireUpdated(BlockPos pos) {
		findRoots(pos);
		tryUpdatePower();
	}
	
	/**
	 * This method is called whenever a redstone wire is placed.
	 */
	public void onWireAdded(WireNode wire) {
		tryAddRoot(wire);
		tryUpdatePower();
	}
	
	/**
	 * This method is called whenever a redstone wire is removed.
	 */
	public void onWireRemoved(WireNode wire) {
		tryAddRoot(wire);
		tryUpdatePower();
	}
	
	/**
	 * Look for wires at and around the given position that are
	 * in an invalid state and require power changes. These wires
	 * are called 'roots' because it is only when these wires
	 * change power level that neighboring wires must adjust as
	 * well.
	 * 
	 * <p>
	 * While it it strictly only necessary to check the wire at
	 * the given position, if that wire is part of a network, it
	 * is beneficial to check its surroundings for other wires
	 * that require power changes. This is because a network can
	 * receive power at multiple points. Consider the following
	 * setup:
	 * 
	 * <p>
	 * (top-down view, W = wire, L = lever, _ = air/other)
	 * <br> _ _ W _ _
	 * <br> _ W W W _
	 * <br> W W L W W
	 * <br> _ W W W _
	 * <br> _ _ W _ _
	 * 
	 * <p>
	 * The lever powers four wires in the network at once. If we
	 * identify this correctly, we can (un)power the entire network
	 * at once. While it is not practical to cover every possible
	 * situation where a network is (un)powered from multiple
	 * points at once, checking for common cases like the one
	 * described above is relatively straight-forward.
	 */
	private void findRoots(BlockPos pos) {
		WireNode wire = getOrAddWire(pos);
		tryAddRoot(wire);
		
		// If the wire at the given position is not in an invalid
		// state or is not part of a larger network, we can abort
		// early.
		if (!wire.inNetwork || wire.connections.all.length == 0) {
			return;
		}
		
		for (Direction dir : Directions.ALL) {
			Node neighbor = getOrAddNode(pos.offset(dir));
			
			// Redstone components can power multiple wires through
			// solid blocks.
			if (neighbor.isSolidBlock()) {
				findRedstoneAround(neighbor, dir.getOpposite());
			} else
			// Redstone components can also power multiple wires
			// directly.
			if (world.emitsWeakPowerTo(neighbor.pos, neighbor.state, dir)) {
				findRootsAroundRedstone(neighbor, dir.getOpposite());
			}
		}
	}
	
	/**
	 * Find redstone components around the given node that can
	 * strongly power that node, and then search for wires that
	 * require power changes around those redstone components.
	 */
	private void findRedstoneAround(Node node, Direction ignore) {
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			Node neighbor = getOrAddNode(node.pos.offset(dir));
			
			if (world.emitsStrongPowerTo(neighbor.pos, neighbor.state, dir)) {
				findRootsAroundRedstone(neighbor, null);
			}
		}
	}
	
	/**
	 * Find wires around the given redstone component that require
	 * power changes.
	 */
	private void findRootsAroundRedstone(Node node, Direction ignore) {
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			// Directions are backwards in Minecraft, so we must check
			// for power emitted in the opposite direction that we are
			// interested in.
			Direction opp = dir.getOpposite();
			
			boolean weak = world.emitsWeakPowerTo(node.pos, node.state, opp);
			boolean strong = world.emitsStrongPowerTo(node.pos, node.state, opp);
			
			// If the redstone component does not emit any power in
			// this direction, move on to the next direction.
			if (!weak && !strong) {
				continue;
			}
			
			Node neighbor = getOrAddNode(node.pos.offset(dir));
			
			if (weak && neighbor.isWire()) {
				tryAddRoot(neighbor.asWire());
			} else if (strong && neighbor.isSolidBlock()) {
				findRootsAround(neighbor, opp);
			}
		}
	}
	
	/**
	 * Look for wires around the given node that require power
	 * changes.
	 */
	private void findRootsAround(Node node, Direction ignore) {
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			Node neighbor = getOrAddNode(node.pos.offset(dir));
			
			if (neighbor.isWire()) {
				tryAddRoot(neighbor.asWire());
			}
		}
	}
	
	/**
	 * Check if the given wire is in an illegal state and needs
	 * power changes.
	 */
	private void tryAddRoot(WireNode wire) {
		// We only want need to check each wire once
		if (wire.prepared) {
			return;
		}
		
		prepareWire(wire);
		findPower(wire, false);
		
		if (needsPowerChange(wire)) {
			network.add(wire);
			rootCount++;
			
			if (wire.connections.flow >= 0) {
				wire.flowOut = wire.connections.flow;
			}
			
			wire.inNetwork = true;
		}
	}
	
	/**
	 * Before a wire can be added to the network, it must be
	 * properly prepared. This method
	 * <br>
	 * - checks if this wire should break. Rather than break
	 *   the wire right away, we integrate its effects into
	 *   the power calculations.
	 * <br>
	 * - determines the 'external power' this wire receives
	 *   (power from non-wire components).
	 */
	private void prepareWire(WireNode wire) {
		if (wire.prepared) {
			return;
		}
		
		wire.prepared = true;
		
		if (!wire.removed && !wire.shouldBreak && world.shouldBreak(wire.pos, wire.state)) {
			wire.shouldBreak = true;
		}
		
		// If the wire is removed or going to break, we treat it
		// as a power source that emits the minimum signal strength.
		// That way the power changes that result from it do not
		// have to be calculated separately afterwards.
		wire.virtualPower = wire.externalPower = (wire.removed || wire.shouldBreak) ? minPower : getExternalPower(wire);
	}
	
	/**
	 * Determine the power level the given wire receives from
	 * neighboring (non-wire) redstone components.
	 */
	private int getExternalPower(WireNode wire) {
		int power = minPower;
		
		for (Direction dir : Directions.ALL) {
			Node neighbor = getOrAddNode(wire.pos.offset(dir));
			
			// We are only interested in power from non-wire
			// components for now...
			if (neighbor.isWire()) {
				continue;
			}
			
			// A block can be both a solid block and a redstone
			// component (e.g. target blocks).
			if (neighbor.isSolidBlock()) {
				power = Math.max(power, getStrongPowerTo(neighbor, dir.getOpposite()));
			}
			if (neighbor.isRedstoneComponent()) {
				power = Math.max(power, world.getWeakPowerFrom(neighbor.pos, neighbor.state, dir));
			}
			
			if (power >= maxPower) {
				return maxPower;
			}
		}
		
		return power;
	}
	
	/**
	 * Determine the strong power the given node receives from
	 * neighboring redstone components.
	 */
	private int getStrongPowerTo(Node node, Direction ignore) {
		int power = minPower;
		
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			Node neighbor = getOrAddNode(node.pos.offset(dir));
			
			if (neighbor.isRedstoneComponent()) {
				power = Math.max(power, world.getStrongPowerFrom(neighbor.pos, neighbor.state, dir));
				
				if (power >= maxPower) {
					return maxPower;
				}
			}
		}
		
		return power;
	}
	
	/**
	 * Determine the power level the given wire receives from the
	 * blocks around it. Power from non-wire components has
	 * already been determined, so only power received from other
	 * wires needs to be checked. There are a few exceptions:
	 * <br>
	 * - If the wire is removed or going to break, its power level
	 *   should always be the minimum value. This is because it
	 *   (effectively) no longer exists, so cannot provide any
	 *   power to neighboring wires.
	 * <br>
	 * - Power received from neighboring wires will never exceed
	 *   {@code maxPower - powerStep}, so if the external power
	 *   is already larger than or equal to that, there is no need
	 *   to check for power from neighboring wires.
	 */
	private void findPower(WireNode wire, boolean ignoreNetwork) {
		if (wire.removed || wire.shouldBreak || wire.externalPower >= (maxPower - powerStep)) {
			return;
		}
		
		// We reset the virtual power to the external power, so
		// the flow information must be reset as well.
		wire.virtualPower = wire.externalPower;
		wire.flowIn = 0;
		
		findWirePower(wire, ignoreNetwork);
	}
	
	/**
	 * Determine the power level the given wire receives from
	 * neighboring wires.
	 */
	private void findWirePower(WireNode wire, boolean ignoreNetwork) {
		for (WireConnection connection : wire.connections.all) {
			if (!connection.in) {
				continue;
			}
			
			WireNode neighbor = getOrAddWire(connection.pos);
			
			if (!ignoreNetwork || !neighbor.inNetwork) {
				int power = Math.max(minPower, neighbor.virtualPower - powerStep);
				// Get the index of the opposite direction
				int iDirOpp = (connection.iDir + 2) & 0b11;
				
				wire.offerPower(power, iDirOpp);
			}
		}
	}
	
	private boolean needsPowerChange(WireNode wire) {
		return wire.removed || wire.shouldBreak || wire.virtualPower != wire.currentPower;
	}
	
	private void tryUpdatePower() {
		if (rootCount == 0 ? !updatingPower : !updatePower()) {
			cleanUp();
		}
	}
	
	/**
	 * Propagate power changes through the network and notify
	 * neighboring blocks of these changes.
	 * 
	 * <p>
	 * Power changes are done in the following 4 steps.
	 * 
	 * <p>
	 * <b>1. Build up the network</b>
	 * <br>
	 * Collect all the wires around the roots that need to change
	 * their power levels.
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
	private boolean updatePower() {
		// The profiler keeps track of how long various parts of the
		// algorithm take. It is only here for debugging purposes,
		// and is commented out in production.
//		Profiler profiler = AlternateCurrentMod.createProfiler();
//		profiler.start();
		
		// Build a network of wires that need power changes. This 
		// includes the roots as well as any wires that will be
		// affected by power changes to those roots.
//		profiler.push("build network");
		buildNetwork();
		
		// Find those wires in the network that receive redstone power
		// from outside it. Remember that the power changes for those
		// wires are already queued here!
//		profiler.swap("find powered wires");
		findPoweredWires();
		
		// Once the powered wires have been found, the network is
		// no longer needed. In fact, it should be cleared before
		// block and shape updates are emitted, in case a different
		// network is updated that needs power changes.
//		profiler.swap("clear " + rootCount + " roots and network of " + network.size());
		rootCount = 0;
		network.clear();
		
		boolean wasUpdating = updatingPower;
		
		// Carry out the power changes and emit shape updates.
//		profiler.swap("let power flow");
		letPowerFlow();
		
//		profiler.pop();
//		profiler.end();
		
		return wasUpdating;
	}
	
	/**
	 * Build up a network of wires that need power changes. This
	 * includes the roots that were already added and any wires
	 * powered by those roots that will need power changes as a
	 * result of power changes to the roots.
	 */
	private void buildNetwork() {
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			
			for (int iDir : UPDATE_ORDER[wire.flowOut]) {
				for (WireConnection connection : wire.connections.byDir[iDir]) {
					if (!connection.out) {
						continue;
					}
					
					WireNode neighbor = getOrAddWire(connection.pos);
					
					if (neighbor.inNetwork) {
						continue;
					}
					
					prepareWire(neighbor);
					findPower(neighbor, false);
					
					if (needsPowerChange(neighbor)) {
						addToNetwork(neighbor, iDir);
					}
				}
			}
		}
	}
	
	/**
	 * Add the given wire to the network and set its outgoing flow
	 * to some backup value. This is a last ditch effort to avoid
	 * directionality in the update order.
	 */
	private void addToNetwork(WireNode wire, int backupFlow) {
		network.add(wire);
		
		wire.inNetwork = true;
		wire.flowOut = backupFlow;
	}
	
	/**
	 * Find those wires in the network that receive power from
	 * outside it, either from non-wire components or from wires
	 * that are not in the network, and queue the power changes for
	 * those wires.
	 */
	private void findPoweredWires() {
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findPower(wire, true);
			
			if (index < rootCount || wire.removed || wire.shouldBreak || wire.virtualPower > minPower) {
				queuePowerChange(wire);
			} else {
				// Wires that do not receive any power do not queue
				// power changes until they are offered power from a
				// neighboring wire. To ensure that they accept any
				// power from neighboring wires and thus queue their
				// power changes, their virtual power is set to below
				// the minimum.
				wire.virtualPower--;
			}
		}
	}
	
	/**
	 * Queue the power change for the given wire.
	 */
	private void queuePowerChange(WireNode wire) {
		if (needsPowerChange(wire)) {
			powerChanges.add(wire);
		} else {
			findPowerFlow(wire);
			transmitPower(wire);
		}
	}
	
	/**
	 * Use the information of incoming power flow to determine the
	 * direction of power flow through this wire. If that flow is
	 * ambiguous, try to use a flow direction based on connections
	 * to neighboring wires. If that is also ambiguous, use the
	 * backup value that was set when the wire was prepared.
	 */
	private void findPowerFlow(WireNode wire) {
		int flow = FLOW_IN_TO_FLOW_OUT[wire.flowIn];
		
		if (flow >= 0) {
			wire.flowOut = flow;
		} else if (wire.connections.flow >= 0) {
			wire.flowOut = wire.connections.flow;
		}
	}
	
	/**
	 * Transmit power from the given wire to neighboring wires.
	 */
	private void transmitPower(WireNode wire) {
		int nextPower = Math.max(minPower, wire.virtualPower - powerStep);
		
		for (int iDir : UPDATE_ORDER[wire.flowOut]) {
			for (WireConnection connection : wire.connections.byDir[iDir]) {
				if (!connection.out) {
					continue;
				}
				
				WireNode connectedWire = world.getWire(connection.pos, true, true);
				
				if (!connectedWire.shouldBreak && connectedWire.offerPower(nextPower, iDir)) {
					queuePowerChange(connectedWire);
				}
			}
		}
	}
	
	private void letPowerFlow() {
		// If an instantaneous update chain causes updates to another
		// network (or the same network in another place), new power
		// changes will be integrated into the already ongoing power
		// queue, so we can exit early here.
		if (updatingPower) {
			return;
		}
		
		updatingPower = true;
		
		while (!powerChanges.isEmpty()) {
			WireNode wire = powerChanges.poll();
			
			// don't continue if an unpowered wire should break
			if (wire.virtualPower == wire.currentPower) {
				continue;
			}
			
			findPowerFlow(wire);
			
			if (wire.updateState()) {
				if (!wire.removed) {
					updateNeighborShapes(wire);
				}
				
				updateNeighborBlocks(wire);
			}
			
			transmitPower(wire);
		}
		
		updatingPower = false;
	}
	
	/**
	 * Emit shape updates around the given wire.
	 */
	private void updateNeighborShapes(WireNode wire) {
		BlockPos wirePos = wire.pos;
		BlockState wireState = wire.state;
		
		for (Direction dir : BlockUtil.DIRECTIONS) {
			updateNeighborShape(wirePos.offset(dir), dir.getOpposite(), wirePos, wireState);
		}
	}
	
	private void updateNeighborShape(BlockPos pos, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState state = world.getBlockState(pos);
		
		// Shape updates to redstone wire are very expensive,
		// and should never happen as a result of power changes
		// anyway.
		if (!state.isAir() && !wireBlock.isOf(state)) {
			world.updateNeighborShape(pos, state, fromDir, fromPos, fromState);
		}
	}
	
	/**
	 * Emit block updates around the given wire.
	 */
	private void updateNeighborBlocks(WireNode wire) {
		int iDir = wire.flowOut;
		
		Direction forward   = Directions.HORIZONTAL[ iDir            ];
		Direction rightward = Directions.HORIZONTAL[(iDir + 1) & 0b11];
		Direction backward  = Directions.HORIZONTAL[(iDir + 2) & 0b11];
		Direction leftward  = Directions.HORIZONTAL[(iDir + 3) & 0b11];
		Direction downward  = Direction.DOWN;
		Direction upward    = Direction.UP;
		
		BlockPos front = wire.pos.offset(forward);
		BlockPos right = wire.pos.offset(rightward);
		BlockPos back  = wire.pos.offset(backward);
		BlockPos left  = wire.pos.offset(leftward);
		BlockPos below = wire.pos.offset(downward);
		BlockPos above = wire.pos.offset(upward);
		
		// direct neighbors (6)
		updateNeighbor(front, wire.pos);
		updateNeighbor(back, wire.pos);
		updateNeighbor(right, wire.pos);
		updateNeighbor(left, wire.pos);
		updateNeighbor(below, wire.pos);
		updateNeighbor(above, wire.pos);
		
		// diagonal neighbors (12)
		updateNeighbor(front.offset(rightward), wire.pos);
		updateNeighbor(back .offset(leftward), wire.pos);
		updateNeighbor(front.offset(leftward), wire.pos);
		updateNeighbor(back .offset(rightward), wire.pos);
		updateNeighbor(front.offset(downward), wire.pos);
		updateNeighbor(back .offset(upward), wire.pos);
		updateNeighbor(front.offset(upward), wire.pos);
		updateNeighbor(back .offset(downward), wire.pos);
		updateNeighbor(right.offset(downward), wire.pos);
		updateNeighbor(left .offset(upward), wire.pos);
		updateNeighbor(right.offset(upward), wire.pos);
		updateNeighbor(left .offset(downward), wire.pos);
		
		// far neighbors (6)
		updateNeighbor(front.offset(forward), wire.pos);
		updateNeighbor(back .offset(backward), wire.pos);
		updateNeighbor(right.offset(rightward), wire.pos);
		updateNeighbor(left .offset(leftward), wire.pos);
		updateNeighbor(below.offset(downward), wire.pos);
		updateNeighbor(above.offset(upward), wire.pos);
	}
	
	private void updateNeighbor(BlockPos pos, BlockPos fromPos) {
		BlockState state = world.getBlockState(pos);
		
		if (!state.isAir() && !wireBlock.isOf(state)) {
			world.updateNeighborBlock(pos, fromPos, wireBlock.asBlock());
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
}