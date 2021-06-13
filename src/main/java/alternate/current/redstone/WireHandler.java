package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import alternate.current.AlternateCurrentMod;
import alternate.current.utils.Directions;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.block.Block;
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
	
	private boolean updatingPower;
	
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

	public boolean isUpdatingPower() {
		return updatingPower;
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
		AlternateCurrentMod.LOGGER.info("update power at " + wire.pos.toShortString());
		
		long s = System.nanoTime();
		long t;
		long start = s;
		
		swapWireBlock(wire.wireBlock);
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("swap wire block: " + (t - s));
		s = t;
		buildNetwork(wire);
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("build network: " + (t - s));
		s = t;
		findPoweredWires(wire);
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("find powered wires: " + (t - s));
		s = t;
		
		network.clear();
		
		letPowerFlow();
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("let power flow: " + (t - s));
		s = t;
		
		nodes.clear();
		poweredWires.clear();
		
		Collection<BlockPos> wires = new ArrayList<>(updatedWires);
		updatedWires.clear();
		
		updateNeighbors(wireBlock, wire.pos, wires);
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("update neighbors: " + (t - s));
		s = t;
		
		AlternateCurrentMod.LOGGER.info("total: " + (t - start));
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
				
				if (connectedWire != null && !connectedWire.inNetwork) {
					addToNetwork(connectedWire);
				}
			}
		}
		
		AlternateCurrentMod.LOGGER.info("network size: " + network.size());
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
			
			if (neighbor.isSolidBlock()) {
				powerFromNeighbor = Math.max(powerFromNeighbor, getStrongPowerTo(neighbor.pos, dir.getOpposite()));
			}
			if (neighbor.isRedstoneComponent()) {
				powerFromNeighbor = Math.max(powerFromNeighbor, neighbor.state.getWeakRedstonePower(world, neighbor.pos, dir));
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
			
			if (neighbor.isRedstoneComponent()) {
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
		updatingPower = true;
		
		while (!poweredWires.isEmpty()) {
			WireNode wire = poweredWires.poll();
			
			if (!wire.inNetwork) {
				continue;
			}
			
			int nextPower = wire.power - 1;
			
			wire.inNetwork = false;
			wire.isPowerSource = false;
			wire.clearNeighbors();
			
			updateWireState(wire);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getWire(pos);
				
				if (connectedWire != null && connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					connectedWire.power = nextPower;
					addPowerSource(connectedWire);
				}
			}
		}
		
		updatingPower = false;
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
	
	private void updateNeighbors(WireBlock wireBlock, BlockPos sourcePos, Collection<BlockPos> updatedWires) {
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		for (BlockPos pos : updatedWires) {
			collectNeighborPositions(pos, blockUpdates);
		}
		
		blockUpdates.removeAll(updatedWires);
		
		dispatchShapeUpdates(wireBlock, updatedWires);
		dispatchBlockUpdates(wireBlock, sourcePos, blockUpdates);
	}
	
	private void dispatchShapeUpdates(WireBlock wireBlock, Collection<BlockPos> updatedWires) {
		for (BlockPos pos : updatedWires) {
			BlockState state = world.getBlockState(pos);
			
			for (int index = 0; index < Directions.ALL.length; index++) {
				Direction dir = Directions.ALL[index];
				BlockPos neighborPos = pos.offset(dir);
				BlockState prevState = world.getBlockState(neighborPos);
				
				if (!wireBlock.isOf(prevState)) {
					BlockState newState = prevState.getStateForNeighborUpdate(dir.getOpposite(), state, world, neighborPos, pos);
					Block.replace(prevState, newState, world, neighborPos, 2);
				}
			}
		}
	}
	
	private void dispatchBlockUpdates(WireBlock wireBlock, BlockPos sourcePos, Collection<BlockPos> blockUpdates) {
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, wireBlock.asBlock(), sourcePos);
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
