package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import alternate.current.utils.Directions;
import alternate.current.utils.profiler.ACProfiler;
import alternate.current.utils.profiler.ACProfilerDummy;
import alternate.current.utils.profiler.Profiler;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;

public class WireHandler {
	
	private final ServerWorld world;
	private final WireBlock wireBlock;
	private final int minPower;
	private final int maxPower;
	
	private final List<WireNode> network;
	private final Long2ObjectMap<Node> nodes;
	private final PriorityQueue<WireNode> poweredWires;
	
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
		this.poweredWires = new PriorityQueue<>();
		
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
	
	private Node addNode(BlockPos pos) {
		Node node = getNextNode(pos);
		return addNode(node);
	}
	
	private Node addNode(Node node) {
		nodes.put(node.pos.asLong(), node);
		return node;
	}
	
	private WireNode getWire(BlockPos pos) {
		Node node = getNode(pos);
		return node != null && node.isWire() ? node.asWire() : null;
	}
	
	private WireNode getOrAddWire(BlockPos pos) {
		Node node = getOrAddNode(pos);
		return node.isWire() ? node.asWire() : null;
	}
	
	private Node getNextNode(BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (wireBlock.isOf(state)) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, true);
			
			if (wire != null) {
				wire.state = state;
				return wire;
			}
		}
		
		return nextNodeFromCache().update(pos, state);
	}
	
	private Node nextNodeFromCache() {
		if (usedNodes >= nodeCache.length) {
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
		if (!updatingPower) {
			usedNodes = 0;
		}
		
		Profiler profiler = ACProfilerDummy.INSTANCE;
		profiler.start();
		
		profiler.push("build network");
		addToNetwork(wire);
		buildNetwork(wire);
		
		profiler.swap("find powered wires");
		findPoweredWires(wire);
		
		profiler.swap("clear network");
		network.clear();
		
		if (updatingPower) {
			profiler.pop();
			profiler.end();
			return;
		}
		
		profiler.swap("let power flow");
		List<BlockPos> updatedWires = letPowerFlow();
		
		profiler.swap("clear nodes");
		nodes.clear();
		
		profiler.swap("update neighbors");
		Collection<BlockPos> blockUpdates = queueBlockUpdates(updatedWires);
		dispatchBlockUpdates(blockUpdates);
		
		profiler.pop();
		profiler.end();
	}
	
	private void buildNetwork(WireNode sourceWire) {
		int minDepth = 0;
		
		if (sourceWire.removed || (sourceWire.power < sourceWire.prevPower)) {
			minDepth = sourceWire.prevPower - minPower + 2;
		}
		
		int nextLayer = network.size();
		
		for (int index = 0; index < network.size(); index++) {
			if (index == nextLayer) {
				nextLayer = network.size();
				minDepth--;
			}
			
			WireNode wire = network.get(index);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null && !connectedWire.inNetwork) {
					prepareForNetwork(connectedWire);
					
					if (minDepth > 0 || !isEdgeNode(connectedWire)) {
						addToNetwork(connectedWire);
					}
				}
			}
		}
	}
	
	private void prepareForNetwork(WireNode wire) {
		if (!wire.removed) {
			collectNeighbors(wire);
		}
		setInitialPower(wire);
	}
	
	private void addToNetwork(WireNode wire) {
		network.add(wire);
		wire.inNetwork = true;
	}
	
	private void removedFromNetwork(WireNode wire) {
		wire.inNetwork = false;
		wire.isPowerSource = false;
		if (!wire.removed) {
			wire.clearNeighbors();
		}
	}
	
	private void collectNeighbors(WireNode wire) {
		Mutable pos = new Mutable();
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			pos.set(wire.pos, dir);
			
			wire.neighbors[index] = getOrAddNode(pos);
		}
	}
	
	private void setInitialPower(WireNode wire) {
		wire.prevPower = wire.power;
		
		if (wire.removed) {
			wire.power = 0;
		} else {
			wire.power = wire.externalPower = getExternalPower(wire);
			
			if (wire.externalPower < maxPower) {
				int wirePower = getWirePower(wire, false);
				
				if (wirePower > wire.externalPower) {
					wire.power = wirePower;
				}
			}
		}
	}
	
	private void validatePower(WireNode wire) {
		if (!wire.removed) {
			wire.power = wire.externalPower;
			
			if (wire.power < maxPower) {
				int wirePower = getWirePower(wire, true);
				
				if (wirePower > wire.externalPower) {
					wire.power = wirePower;
				}
			}
		}
	}
	
	private int getExternalPower(WireNode wire) {
		int power = minPower;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			Node neighbor = wire.neighbors[index];
			
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
		
		Mutable side = new Mutable();
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			
			if (dir == ignore) {
				continue;
			}
			
			side.set(pos, dir);
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
		validatePower(sourceWire);
		addPowerSource(sourceWire);
		
		for (int index = 1; index < network.size(); index++) {
			WireNode wire = network.get(index);
			validatePower(wire);
			
			if (wire.power > minPower) {
				addPowerSource(wire);
			}
		}
	}
	
	private void addPowerSource(WireNode wire) {
		poweredWires.add(wire);
		wire.isPowerSource = true;
	}
	
	private List<BlockPos> letPowerFlow() {
		List<BlockPos> updatedWires = new ArrayList<>();
		
		updatingPower = true;
		
		while (!poweredWires.isEmpty()) {
			WireNode wire = poweredWires.poll();
			
			if (!wire.inNetwork) {
				continue;
			}
			
			int nextPower = wire.power - 1;
			
			if (updateWireState(wire)) {
				dispatchShapeUpdates(wire);
			}
			
			updatedWires.add(wire.pos);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getWire(pos);
				
				if (connectedWire != null && connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					connectedWire.power = nextPower;
					addPowerSource(connectedWire);
				}
			}
		}
		
		updatingPower = false;
		
		return updatedWires;
	}
	
	private boolean updateWireState(WireNode wire) {
		removedFromNetwork(wire);
		
		if (wire.removed) {
			return false;
		}
		if (wire.power < minPower) {
			wire.power = minPower;
		}
		
		return wireBlock.setPower(wire, wire.power, 18);
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
	
	private Collection<BlockPos> queueBlockUpdates(List<BlockPos> updatedWires) {
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			collectNeighborPositions(updatedWires.get(index), blockUpdates);
		}
		
		blockUpdates.removeAll(updatedWires);
		
		return blockUpdates;
	}
	
	private void dispatchBlockUpdates(Collection<BlockPos> blockUpdates) {
		Block block = wireBlock.asBlock();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, block, pos);
		}
	}
	
	public static void collectNeighborPositions(BlockPos pos, Collection<BlockPos> positions) {
		BlockPos west = pos.west();
		BlockPos east = pos.east();
		BlockPos north = pos.north();
		BlockPos south = pos.south();
		BlockPos down = pos.down();
		BlockPos up = pos.up();
		
		// Direct neighbors
		positions.add(west);
		positions.add(east);
		positions.add(north);
		positions.add(south);
		positions.add(down);
		positions.add(up);
		
		// Diagonal neighbors
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
		
		// Neighbors 2 out in each direction
		positions.add(west.west());
		positions.add(east.east());
		positions.add(north.north());
		positions.add(south.south());
		positions.add(down.down());
		positions.add(up.up());
	}
}
