package fast.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import fast.redstone.interfaces.mixin.IWireBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RedstoneWireHandlerOld {
	
	private static final Direction[] DIRECTIONS = new Direction[] {
			Direction.DOWN,
			Direction.UP,
			Direction.NORTH,
			Direction.SOUTH,
			Direction.WEST,
			Direction.EAST
	};
	private static final int DOWN = 0;
	private static final int UP = 1;
	/*private static final int NORTH = 2;
	private static final int SOUTH = 3;
	private static final int WEST = 4;
	private static final int EAST = 5;*/
	
	public final int MAX_POWER = 15;
	
	private final Block wireBlock;
	private final List<Wire> wires;
	private final Map<BlockPos, Node> nodes;
	private final Queue<Wire> poweredWires;
	private final List<BlockPos> updatedWires;
	private final Set<BlockPos> blockUpdates;
	
	private World world;
	private boolean updatingPower;
	
	public RedstoneWireHandlerOld(Block wireBlock) {
		if (!(wireBlock instanceof IWireBlock)) {
			throw new IllegalArgumentException(String.format("The given Block must implement %s", IWireBlock.class));
		}
		
		this.wireBlock = wireBlock;
		this.wires = new ArrayList<>();
		this.nodes = new HashMap<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
		this.blockUpdates = new LinkedHashSet<>();
	}
	
	public void updatePower(World world, BlockPos sourcePos, BlockState sourceState) {
		this.world = world;
		Wire sourceWire = addSourceWire(sourcePos, sourceState);
		
		if (validPowerState(sourceWire)) {
			wires.clear();
			nodes.clear();
			
			if (!updatingPower) {
				poweredWires.clear();
			}
			
			return;
		}
		
		updateNetwork();
		findPoweredWires();
		
		nodes.clear();
		
		if (updatingPower) {
			return;
		}
		
		letPowerFlow();
		
		System.out.println("collecting neighbor positions");
		long start = System.nanoTime();
		
		blockUpdates.removeAll(updatedWires);
		List<BlockPos> positions = new ArrayList<>(blockUpdates);
		
		updatedWires.clear();
		blockUpdates.clear();
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("updating neighbors");
		start = System.nanoTime();
		
		for (int index = positions.size() - 1; index >= 0; index--) {
			world.updateNeighbor(positions.get(index), wireBlock, sourcePos);
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private Wire addSourceWire(BlockPos pos, BlockState state) {
		Wire wire = new Wire(pos, state);
		
		nodes.put(pos, wire);
		addWireToNetwork(wire);
		findNeighborsAndConnections(wire);
		
		return wire;
	}
	
	private boolean validPowerState(Wire sourceWire) {
		int powerReceived = getReceivedRedstonePower(sourceWire);
		
		if (sourceWire.power == powerReceived) {
			return true;
		}
		
		sourceWire.power = powerReceived;
		poweredWires.add(sourceWire);
		
		return false;
	}
	
	private void updateNetwork() {
		System.out.println("updating network");
		long start = System.nanoTime();
		
		for (int index = 1; index < wires.size(); index++) {
			findNeighborsAndConnections(wires.get(index));
		}
		
		System.out.println("t: "  + (System.nanoTime() - start));
	}
	
	private void findNeighborsAndConnections(Wire wire) {
		BlockPos pos = wire.pos;
		
		for (int index = 0; index < DIRECTIONS.length; index++) {
			Direction dir = DIRECTIONS[index];
			BlockPos side = pos.offset(dir);
			
			Node sideNode = getOrAddNode(side);
			wire.neighbors[index] = sideNode;
			
			if (dir.getAxis().isHorizontal()) {
				if (sideNode.isWire()) {
					addWireToNetwork((Wire)sideNode);
					wire.addConnection((Wire)sideNode, true, true);
					
					continue;
				}
				
				boolean sideIsSolid = sideNode.type == Node.Type.SOLID_BLOCK;
				
				if (wire.aboveNode().type != Node.Type.SOLID_BLOCK) {
					BlockPos aboveSide = side.up();
					Node aboveSideNode = getOrAddNode(aboveSide);
					
					if (aboveSideNode.isWire()) {
						addWireToNetwork((Wire)aboveSideNode);
						wire.addConnection((Wire)aboveSideNode, true, sideIsSolid);
					}
				}
				if (!sideIsSolid) {
					BlockPos belowSide = side.down();
					Node belowSideNode = getOrAddNode(belowSide);
					
					if (belowSideNode.isWire()) {
						Node belowNode = wire.belowNode();
						boolean belowIsSolid = belowNode.type == Node.Type.SOLID_BLOCK;
						
						if (belowIsSolid) {
							addWireToNetwork((Wire)belowSideNode);
						}
						
						wire.addConnection((Wire)belowSideNode, belowIsSolid, true);
					}
				}
			}
		}
	}
	
	private void addWireToNetwork(Wire wire) {
		if (!wire.inNetwork) {
			wires.add(wire);
			wire.inNetwork = true;
		}
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
		Node node;
		
		if (state.isOf(wireBlock)) {
			node = new Wire(pos, state);
		} else if (state.emitsRedstonePower()) {
			node = new Node(Node.Type.REDSTONE_COMPONENT, pos, state);
		} else if (state.isSolidBlock(world, pos)) {
			node = new Node(Node.Type.SOLID_BLOCK, pos, state);
		} else {
			node = new Node(Node.Type.OTHER, pos, state);
		}
		
		nodes.put(pos, node);
		
		return node;
	}
	
	private void findPoweredWires() {
		System.out.println("finding powered wires");
		long start = System.nanoTime();
		
		((IWireBlock)wireBlock).setWiresGivePower(false);
		
		for (int index = 1; index < wires.size(); index++) {
			Wire wire = wires.get(index);
			wire.power = getReceivedRedstonePower(wire);
			
			if (wire.power > 0) {
				poweredWires.add(wire);
			}
		}
		
		wires.clear();
		
		((IWireBlock)wireBlock).setWiresGivePower(true);
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private int getReceivedRedstonePower(Wire wire) {
		int powerFromNeighbors = getPowerFromNeighbors(wire);
		
		if (powerFromNeighbors >= MAX_POWER) {
			return MAX_POWER;
		}
		
		int powerFromConnections = getPowerFromConnections(wire);
		
		if (powerFromConnections > powerFromNeighbors) {
			return powerFromConnections;
		}
		
		return powerFromNeighbors;
	}
	
	private int getPowerFromNeighbors(Wire wire) {
		int power = 0;
		
		for (int index = 0; index < DIRECTIONS.length; index++) {
			Node node = wire.neighbors[index];
			
			int powerFromNeighbor;
			switch (node.type) {
			case REDSTONE_COMPONENT:
				powerFromNeighbor = node.state.getWeakRedstonePower(world, node.pos, DIRECTIONS[index]);
				break;
			case SOLID_BLOCK:
				powerFromNeighbor = getStrongPowerReceived(node, DIRECTIONS[index].getOpposite());
				break;
			default:
				continue;
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
	
	private int getStrongPowerReceived(Node node, Direction ignore) {
		int power = 0;
		
		for (int index = 0; index < DIRECTIONS.length; index++) {
			Direction dir = DIRECTIONS[index];
			
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = node.pos.offset(dir);
			Node sideNode = getOrAddNode(side);
			
			if (sideNode.type == Node.Type.REDSTONE_COMPONENT) {
				int powerFromSide = sideNode.state.getStrongRedstonePower(world, side, dir);
				
				if (powerFromSide > power) {
					power = powerFromSide;
					
					if (power >= MAX_POWER) {
						return MAX_POWER;
					}
				}
			}
		}
		
		return power;
	}
	
	private int getPowerFromConnections(Wire wire) {
		int power = 0;
		
		for (Wire connectedWire : wire.connectionsIn) {
			if (!connectedWire.inNetwork) {
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
		
		updatingPower = true;
		
		while (!poweredWires.isEmpty()) {
			Wire wire = poweredWires.poll();
			
			if (!wire.inNetwork) {
				continue;
			}
			
			int nextPower = wire.power - 1;
			
			wire.inNetwork = false;
			wire.isPowerSource = false;
			
			updateWireState(wire);
			
			for (Wire connectedWire : wire.connectionsOut) {
				if (connectedWire.inNetwork && !connectedWire.isPowerSource) {
					connectedWire.power = nextPower;
					
					poweredWires.add(connectedWire);
					connectedWire.isPowerSource = true;
				}
			}
		}
		
		updatingPower = false;
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void updateWireState(Wire wire) {
		if (wire.power < 0) {
			wire.power = 0;
		}
		
		BlockState newState = wire.state.with(Properties.POWER, wire.power);
		
		if (newState != wire.state) {
			world.setBlockState(wire.pos, newState, 18);
			emitShapeUpdates(wire);
			updatedWires.add(wire.pos);
			queueBlockUpdates(wire);
		}
	}
	
	private void emitShapeUpdates(Wire wire) {
		for (int index = 0; index < DIRECTIONS.length; index++) {
			Node neighbor = wire.neighbors[index];
			
			if (!neighbor.isWire()) {
				Direction dir = DIRECTIONS[index].getOpposite();
				
				BlockState oldState = neighbor.state;
				BlockState newState = oldState.getStateForNeighborUpdate(dir, neighbor.state, world, neighbor.pos, wire.pos);
				
				if (newState != oldState) {
					neighbor.state = newState;
					Block.replace(oldState, newState, world, neighbor.pos, 2);
				}
			}
		}
	}
	
	private void queueBlockUpdates(Wire wire) {
		collectNeighborPositions(wire.pos, blockUpdates);
	}
	
	public void collectNeighborPositions(BlockPos pos, Collection<BlockPos> positions) {
		BlockPos down = pos.down();
		BlockPos up = pos.up();
		BlockPos north = pos.north();
		BlockPos south = pos.south();
		BlockPos west = pos.west();
		BlockPos east = pos.east();
		
		positions.add(down);
		positions.add(up);
		positions.add(north);
		positions.add(south);
		positions.add(west);
		positions.add(east);
		
		positions.add(down.north());
		positions.add(up.south());
		positions.add(down.south());
		positions.add(up.north());
		positions.add(down.west());
		positions.add(up.east());
		positions.add(down.east());
		positions.add(up.west());
		
		positions.add(north.west());
		positions.add(south.east());
		positions.add(west.south());
		positions.add(east.north());
		
		positions.add(down.down());
		positions.add(up.up());
		positions.add(north.north());
		positions.add(south.south());
		positions.add(west.west());
		positions.add(east.east());
	}
	
	private static class Node {
		
		public enum Type {
			WIRE, REDSTONE_COMPONENT, SOLID_BLOCK, OTHER;
		}
		
		public Type type;
		public BlockPos pos;
		public BlockState state;
		
		public Node(Type type, BlockPos pos, BlockState state) {
			this.type = type;
			this.pos = pos;
			this.state = state;
		}
		
		public boolean isWire() {
			return type == Type.WIRE;
		}
	}
	
	private static class Wire extends Node implements Comparable<Wire> {
		
		public final Node[] neighbors;
		public final List<Wire> connectionsOut;
		public final List<Wire> connectionsIn;
		
		public int power;
		
		public boolean inNetwork;
		public boolean isPowerSource;
		
		public Wire(BlockPos pos, BlockState state) {
			super(Type.WIRE, pos, state);
			
			if (!(state.getBlock() instanceof IWireBlock)) {
				throw new IllegalArgumentException(String.format("The given BlockState must be of a Block that implements %s", IWireBlock.class));
			}
			
			this.neighbors = new Node[DIRECTIONS.length];
			this.connectionsOut = new ArrayList<>();
			this.connectionsIn = new ArrayList<>();
			
			this.power = this.state.get(Properties.POWER);
		}
		
		@Override
		public int compareTo(Wire wire) {
			return Integer.compare(wire.power, power);
		}
		
		public Node belowNode() {
			return neighbors[DOWN];
		}
		
		public Node aboveNode() {
			return neighbors[UP];
		}
		
		public void addConnection(Wire wire, boolean out, boolean in) {
			if (out) {
				connectionsOut.add(wire);
			}
			if (in) {
				connectionsIn.add(wire);
			}
		}
	}
}
