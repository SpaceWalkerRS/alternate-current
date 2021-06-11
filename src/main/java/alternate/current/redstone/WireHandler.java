package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import alternate.current.utils.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WireHandler {
	
	public static final int MAX_POWER = 15;
	
	private final Block wireBlock;
	
	private final Map<BlockPos, Node> nodes;
	private final List<Node> network;
	private final Queue<Node> poweredWires;
	private final List<BlockPos> updatedWires;
	
	private Node[] nodeCache;
	private int nodeCount;
	
	private World world;
	
	public WireHandler(Block wireBlock) {
		this.wireBlock = wireBlock;
		
		this.nodes = new HashMap<>();
		this.network = new ArrayList<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
		
		this.nodeCache = new Node[16];
		
		cleanNodeCache(0, 16);
	}
	
	public void updatePower(World world, BlockPos pos, BlockState state) {
		long s = System.nanoTime();
		
		this.world = world;
		Node node = addNode(pos, state);
		
		addWireToNetwork(node);
		
		if (isAtEdge(node)) {
			nodes.clear();
			network.clear();
			nodeCount = 0;
			
			return;
		}
		
		for (Node connectedWire : node.connectionsOut) {
			addWireToNetwork(connectedWire);
		}
		
		buildNetwork(node);
		findPoweredWires(node);
		letPowerFlow();
		
		System.out.println("updating neighbors");
		long start = System.nanoTime();
		
		updateNeighbors(pos);
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("total: " + (System.nanoTime() - s));
	}
	
	private void buildNetwork(Node sourceNode) {
		System.out.println("building network");
		long start = System.nanoTime();
		
		for (int index = 1; index < network.size(); index++) {
			Node wire = network.get(index);
			
			if (isAtEdge(wire)) {
				continue;
			}
			
			for (Node connectedWire : wire.connectionsOut) {
				if (!connectedWire.inNetwork) {
					addWireToNetwork(connectedWire);
				}
			}
		}
		
		System.out.println("network size: " + network.size());
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private Node getNode(BlockPos pos) {
		return nodes.get(pos);
	}
	
	private Node getOrAddNode(BlockPos pos) {
		Node node = getNode(pos);
		return node == null ? addNode(pos) : node;
	}
	
	private Node addNode(BlockPos pos) {
		return addNode(pos, world.getBlockState(pos));
	}
	
	private Node addNode(BlockPos pos, BlockState state) {
		Node node = nextNode();
		node.update(world, pos, state, wireBlock);
		
		return addNode(node);
	}
	
	private Node addNode(Node node) {
		nodes.put(node.pos, node);
		return node;
	}
	
	private Node nextNode() {
		if (nodeCount >= nodeCache.length) {
			expandNodeCache();
		}
		
		return nodeCache[nodeCount++];
	}
	
	private void expandNodeCache() {
		int size = nodeCache.length;
		Node[] newCache = new Node[2 * size];
		
		for (int index = 0; index < size; index++) {
			newCache[index] = nodeCache[index];
		}
		nodeCache = newCache;
		
		cleanNodeCache(size, 2 * size);
	}
	
	private void cleanNodeCache(int start, int end) {
		if (end > nodeCache.length) {
			end = nodeCache.length;
		}
		for (int index = start; index < end; index++) {
			nodeCache[index] = new Node();
		}
	}
	
	private void addWireToNetwork(Node node) {
		if (!node.isWire()) {
			return;
		}
		
		network.add(node);
		initWire(node);
	}
	
	private void initWire(Node node) {
		node.inNetwork = true;
		
		collectNeighbors(node);
		findConnections(node);
	}
	
	private void collectNeighbors(Node node) {
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = node.pos.offset(dir);
			
			node.neighbors[index] = getOrAddNode(side);
		}
	}
	
	private void findConnections(Node node) {
		Node belowNode = node.neighbors[Directions.DOWN];
		Node aboveNode = node.neighbors[Directions.UP];
		
		boolean belowIsSolid = belowNode.isSolidBlock();
		boolean aboveIsSolid = aboveNode.isSolidBlock();
		
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Node sideNode = node.neighbors[index];
			
			if (sideNode.isWire()) {
				node.addConnection(sideNode, true, true);
				continue;
			}
			
			Direction dir = Directions.HORIZONTAL[index];
			BlockPos side = node.pos.offset(dir);
			
			boolean sideIsSolid = sideNode.isSolidBlock();
			
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				Node aboveSideNode = getOrAddNode(aboveSide);
				
				if (aboveSideNode.isWire()) {
					node.addConnection(aboveSideNode, sideIsSolid,  true);
				}
			}
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				Node belowSideNode = getOrAddNode(belowSide);
				
				if (belowSideNode.isWire()) {
					node.addConnection(belowSideNode, true,  belowIsSolid);
				}
			}
		}
	}
	
	private boolean isAtEdge(Node wire) {
		int nonWirePower = getNonWirePower(wire);
		int wirePower = getWirePower(wire, false);
		
		int receivedPower = wirePower > nonWirePower ? wirePower : nonWirePower;
		int power = wire.power;
		
		wire.power = receivedPower;
		wire.nonWirePower = nonWirePower;
		
		return power == receivedPower;
	}
	
	private void findPoweredWires(Node sourceWire) {
		System.out.println("finding powered wires");
		long start = System.nanoTime();
		
		for (Node wire : network) {
			int wirePower = getWirePower(wire, true);
			
			if (wirePower > wire.nonWirePower) {
				wire.power = wirePower;
			} else {
				wire.power = wire.nonWirePower;
			}
			
			if (wire.power > 0) {
				addPowerSource(wire);
			}
		}
		
		if (!sourceWire.isPowerSource) {
			addPowerSource(sourceWire);
		}
		
		network.clear();
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void addPowerSource(Node wire) {
		poweredWires.add(wire);
		wire.isPowerSource = true;
	}
	
	private int getNonWirePower(Node wire) {
		int power = 0;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Node neighbor = wire.neighbors[index];
			int powerFromNeighbor = 0;
			
			if (neighbor.isSolidBlock()) {
				powerFromNeighbor = getStrongPower(neighbor.pos, Directions.ALL[index].getOpposite());
			} else if (neighbor.isRedstoneComponent()) {
				powerFromNeighbor = neighbor.state.getWeakRedstonePower(world, neighbor.pos, Directions.ALL[index]);
			}
			
			if (powerFromNeighbor > power) {
				power = powerFromNeighbor;
				
				if (power >= MAX_POWER) {
					return MAX_POWER;
				}
			}
		}
		
		return power;
	}
	
	private int getStrongPower(BlockPos pos, Direction ignore) {
		int power = 0;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			Node node = getOrAddNode(side);
			
			if (node.isRedstoneComponent()) {
				int powerFromSide = node.state.getStrongRedstonePower(world, side, dir);
				
				if (powerFromSide > power) {
					power = powerFromSide;
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(Node wire, boolean ignoreNetwork) {
		int power = 0;
		
		for (Node connectedWire : wire.connectionsIn) {
			if (!ignoreNetwork || !connectedWire.inNetwork) {
				int powerFromWire = connectedWire.power - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
				}
			}
		}
		
		return power;
	}
	
	private void letPowerFlow() {
		System.out.println("updating power");
		long start = System.nanoTime();
		
		while (!poweredWires.isEmpty()) {
			Node wire = poweredWires.poll();
			
			if (!wire.inNetwork) {
				continue;
			}
			
			int nextPower = wire.power - 1;
			
			wire.inNetwork = false;
			wire.isPowerSource = false;
			
			updateWireState(wire);
			
			for (Node connectedWire : wire.connectionsOut) {
				if (connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					connectedWire.power = nextPower;
					addPowerSource(connectedWire);
				}
			}
		}
		
		nodes.clear();
		nodeCount = 0;
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void updateWireState(Node wire) {
		if (wire.power < 0) {
			wire.power = 0;
		}
		
		BlockState oldState = wire.state;
		BlockState newState = oldState.with(Properties.POWER, wire.power);
		
		if (newState != oldState) {
			world.setBlockState(wire.pos, newState, 18);
			updatedWires.add(wire.pos);
		}
	}
	
	private void updateNeighbors(BlockPos sourcePos) {
		for (BlockPos pos : updatedWires) {
			emitShapeUpdates(pos);
		}
		
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			collectNeighborPositions(updatedWires.get(index), blockUpdates);
		}
		blockUpdates.removeAll(updatedWires);
		
		updatedWires.clear();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, wireBlock, sourcePos);
		}
	}
	
	private void emitShapeUpdates(BlockPos pos) {
		BlockState state = world.getBlockState(pos);
		
		if (!state.isOf(wireBlock)) {
			return;
		}
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = pos.offset(dir);
			BlockState oldState = world.getBlockState(side);
			
			if (oldState.isOf(wireBlock)) {
				continue;
			}
			
			BlockState newState = oldState.getStateForNeighborUpdate(dir.getOpposite(), state, world, side, pos);
			Block.replace(oldState, newState, world, side, 2);
		}
	}
	
	public void collectNeighborPositions(BlockPos pos, Collection<BlockPos> positions) {
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
