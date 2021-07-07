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

public class WireHandler {
	
	private final ServerWorld world;
	private final WireBlock wireBlock;
	private final int minPower;
	private final int maxPower;
	
	private final List<WireNode> network;
	private final Long2ObjectMap<Node> nodes;
	private final PriorityQueue<WireNode> powerChanges;
	private final List<BlockPos> allWires;
	private final List<BlockPos> updatedWires;
	
	private Node[] nodeCache;
	private int usedNodes;
	
	private boolean updatingPower;
	
	public WireHandler(ServerWorld world, WireBlock wireBlock) {
		this.world = world;
		this.wireBlock = wireBlock;
		this.minPower = this.wireBlock.getMinPower();
		this.maxPower = this.wireBlock.getMaxPower();
		
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
		return node.isWire() ? node.asWire() : null;
	}
	
	private Node addNode(BlockPos pos) {
		Node node = getNextNode(pos);
		return addNode(node);
	}
	
	private Node addNode(Node node) {
		nodes.put(node.pos.asLong(), node);
		return node;
	}
	
	private Node getNextNode(BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (wireBlock.isOf(state)) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, true);
			
			if (wire != null) {
				wire.prepared = false;
				wire.inNetwork = false;
				
				return wire;
			}
		}
		
		return nextNodeFromCache().update(pos, state);
	}
	
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
	
	public void updatePower(WireNode wire) {
		Profiler profiler = AlternateCurrentMod.createProfiler();
		profiler.start();
		
		profiler.push("build network");
		buildNetwork(wire);
		
		profiler.swap("find powered wires");
		findPoweredWires(wire);
		
		profiler.swap("clear network of " + network.size());
		network.clear();
		
		profiler.swap("clear nodes");
		nodes.clear();
		usedNodes = 0;
		
		if (!updatingPower) {
			profiler.swap("let power flow");
			letPowerFlow();
			
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
	
	private void buildNetwork(WireNode sourceWire) {
		addNode(sourceWire.pos);
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
					
					if (minDepth > 0 || !isEdgeNode(connectedWire)) {
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
		if (sourceWire.power < sourceWire.prevPower) {
			return sourceWire.prevPower - minPower + 2;
		}
		
		return 0;
	}
	
	private void prepareForNetwork(WireNode wire) {
		if (!wire.prepared) {
			if (!wire.removed && !wire.shouldBreak && !wire.state.canPlaceAt(world, wire.pos)) {
				wire.shouldBreak = true;
			}
			
			wire.prevPower = wireBlock.getPower(world, wire.pos, wire.state);
			wire.power = wire.externalPower = getPreliminaryPower(wire);
			
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
			
			if (neighbor.isSolidBlock()) {
				power = Math.max(power, getStrongPowerTo(neighbor.pos, dir.getOpposite()));
			}
			if (neighbor.isRedstoneComponent()) {
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
			
			if (neighbor.isRedstoneComponent()) {
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
			wire.power = wire.externalPower;
			
			if (wire.power < maxPower) {
				wire.power = Math.max(wire.power, getWirePower(wire, ignoreNetwork));
			}
		}
	}
	
	private int getWirePower(WireNode wire, boolean ignoreNetwork) {
		int power = minPower;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getOrAddWire(pos);
			
			if (connectedWire != null && (!ignoreNetwork || !connectedWire.inNetwork)) {
				power = Math.max(power, connectedWire.power - 1);
			}
		}
		
		return power;
	}
	
	private boolean isEdgeNode(WireNode wire) {
		return wire.power == wire.prevPower;
	}
	
	private void findPoweredWires(WireNode sourceWire) {
		findPower(sourceWire, true);
		tryQueuePowerChange(sourceWire);
		
		for (int index = 1; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findPower(wire, true);
			
			if (wire.power > minPower) {
				tryQueuePowerChange(wire);
			}
		}
	}
	
	private void tryQueuePowerChange(WireNode wire) {
		if (isEdgeNode(wire)) {
			transmitPower(wire);
		} else {
			queuePowerChange(wire);
		}
	}
	
	private void queuePowerChange(WireNode wire) {
		powerChanges.add(wire);
	}
	
	private void transmitPower(WireNode wire) {
		int nextPower = wire.power - 1;
		
		for (BlockPos pos : wire.connectionsOut) {
			WireNode connectedWire = wireBlock.getWire(world, pos);
			
			if (connectedWire != null && acceptsPower(connectedWire, nextPower)) {
				connectedWire.power = nextPower;
				tryQueuePowerChange(connectedWire);
			}
		}
	}
	
	private void letPowerFlow() {
		updatingPower = true;
		
		while (!powerChanges.isEmpty()) {
			WireNode wire = powerChanges.poll();
			
			if (isEdgeNode(wire)) {
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
		
		updatingPower = false;
	}
	
	private boolean acceptsPower(WireNode wire, int power) {
		if (power > wire.power) {
			return true;
		}
		
		return wire.power == minPower;
	}
	
	private boolean updateWireState(WireNode wire) {
		if (wire.removed) {
			return true;
		}
		if (wire.shouldBreak) {
			return wireBlock.breakBlock(world, wire.pos, wire.state, 2);
		}
		
		return wireBlock.setPower(world, wire.pos, wire.state, wire.power, 18);
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
	 * The order in which these are added follows three rules:
	 * 
	 * <br> 1) add positions in order of their distance from the origin
	 * <br> 2) use the following basic order: WEST -> EAST -> NORTH -> SOUTH -> DOWN -> UP
	 * <br> 3) every pair of positions are "opposites" (relative to the origin)
	 * 
	 * @param pos the origin
	 * @param positions the collection to which the neighboring positions should be added
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
