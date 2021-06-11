package alternate.current.boop;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.PriorityQueue;

import alternate.current.utils.Directions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class WireHandler {
	
	private final ServerWorld world;
	
	private final List<WireNode> network;
	private final Long2ObjectMap<Node> nodes;
	private final PriorityQueue<WireNode> poweredWires;
	private final List<BlockPos> updatedWires;
	
	private WireBlock wireBlock;
	private IntProperty powerProperty;
	private int minPower;
	private int maxPower;
	
	public WireHandler(ServerWorld world) {
		this.world = world;
		
		this.network = new ArrayList<>();
		this.nodes = new Long2ObjectOpenHashMap<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
	}
	
	private Node getNode(BlockPos pos) {
		return nodes.get(pos.asLong());
	}
	
	private Node getOrAddNode(BlockPos pos) {
		Node node = getNode(pos);
		return node == null ? addNode(pos) : node;
	}
	
	private Node addNode(BlockPos pos) {
		return addNode(pos, world.getBlockState(pos));
	}
	
	private Node addNode(BlockPos pos, BlockState state) {
		return addNode(Node.of(wireBlock, world, pos, state));
	}
	
	private Node addNode(Node node) {
		nodes.put(node.pos.asLong(), node);
		return node;
	}
	
	private WireNode getWire(BlockPos pos) {
		Node node = getNode(pos);
		
		if (node == null) {
			return null;
		}
		
		return node.isWire() ? node.asWire() : null;
	}
	
	private WireNode getOrAddWire(BlockPos pos) {
		Node node = getOrAddNode(pos);
		return node.isWire() ? node.asWire() : null;
	}
	
	public void updatePower(WireNode wire) {
		swapWireBlock(wire.wireBlock);
		buildNetwork(wire);
		findPoweredWires(wire);
		
		network.clear();
		
		letPowerFlow();
		
		nodes.clear();
		poweredWires.clear();
		
		updateNeighbors();
		
		updatedWires.clear();
	}
	
	private void swapWireBlock(WireBlock wireBlock) {
		if (this.wireBlock == wireBlock) {
			return;
		}
		
		this.wireBlock = wireBlock;
		this.powerProperty = this.wireBlock.getPowerProperty();
		this.minPower = this.wireBlock.getMinPower();
		this.maxPower = this.wireBlock.getMaxPower();
	}
	
	private void buildNetwork(WireNode sourceWire) {
		addNode(sourceWire);
		addToNetwork(sourceWire);
		
		for (int index = 0; index < network.size(); index++) {
			WireNode wire = network.get(index);
			findNeighbors(wire);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null) {
					addToNetwork(connectedWire);
				}
			}
		}
	}
	
	private void addToNetwork(WireNode wire) {
		network.add(wire);
		wire.inNetwork = true;
	}
	
	private void findNeighbors(WireNode wire) {
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos pos = wire.pos.offset(dir);
			
			wire.neighbors[index] = getOrAddNode(pos);
		}
	}
	
	private void findPoweredWires(WireNode sourceWire) {
		for (WireNode wire : network) {
			wire.power = getReceivedPower(wire);
			
			if (wire.power > minPower) {
				addPowerSource(wire);
			}
		}
		
		if (!sourceWire.isPowerSource) {
			addPowerSource(sourceWire);
		}
	}
	
	private void addPowerSource(WireNode wire) {
		poweredWires.add(wire);
		wire.isPowerSource = true;
	}
	
	public int getReceivedPower(WireNode wire) {
		int externalPower = getExternalPower(wire);
		
		if (externalPower >= maxPower) {
			return maxPower;
		}
		
		int wirePower = getWirePower(wire);
		
		if (wirePower > externalPower) {
			return wirePower;
		}
		
		return externalPower;
	}
	
	private int getExternalPower(WireNode wire) {
		int power = minPower;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			Node neighbor = wire.neighbors[index];
			
			int powerFromNeighbor = minPower;
			
			if (neighbor.type == NodeType.SOLID_BLOCK) {
				powerFromNeighbor = getStrongPowerTo(neighbor.pos, dir.getOpposite());
			} else
			if (neighbor.type == NodeType.REDSTONE_COMPONENT) {
				powerFromNeighbor = neighbor.state.getWeakRedstonePower(world, neighbor.pos, dir);
			}
			
			if (powerFromNeighbor > power) {
				power = powerFromNeighbor;
				
				if (power >= maxPower) {
					return maxPower;
				}
			}
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
			
			if (neighbor.type == NodeType.REDSTONE_COMPONENT) {
				int powerFromNeighbor = neighbor.state.getStrongRedstonePower(world, side, dir);
				
				if (powerFromNeighbor > power) {
					power = powerFromNeighbor;
					
					if (power >= maxPower) {
						return maxPower;
					}
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(WireNode wire) {
		int power = minPower;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getOrAddWire(pos);
			
			if (connectedWire != null && !connectedWire.inNetwork) {
				int powerFromWire = connectedWire.power - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
				}
			}
		}
		
		return power;
	}
	
	private void letPowerFlow() {
		while (!poweredWires.isEmpty()) {
			WireNode wire = poweredWires.poll();
			
			if (!wire.inNetwork) {
				continue;
			}
			
			int nextPower = wire.power - 1;
			
			wire.inNetwork = false;
			wire.isPowerSource = false;
			
			updateWireState(wire);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getWire(pos);
				
				if (connectedWire != null && connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					addPowerSource(connectedWire);
				}
			}
		}
	}
	
	private void updateWireState(WireNode wire) {
		if (wire.power < minPower) {
			wire.power = minPower;
		}
		
		BlockState prevState = wire.state;
		BlockState newState = prevState.with(powerProperty, wire.power);
		
		if (newState != prevState) {
			BlockPos pos = wire.pos;
			
			world.setBlockState(pos, newState, 18);
			updatedWires.add(pos);
		}
	}
	
	private void updateNeighbors() {
		dispatchShapeUpdates();
		dispatchBlockUpdates();
	}
	
	private void dispatchShapeUpdates() {
		
	}
	
	private void dispatchBlockUpdates() {
		
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
