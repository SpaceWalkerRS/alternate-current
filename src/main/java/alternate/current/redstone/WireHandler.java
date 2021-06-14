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
import net.minecraft.util.math.BlockPos.Mutable;
import net.minecraft.util.math.Direction;

public class WireHandler {
	
	private final ServerWorld world;
	
	private final List<WireNode> network;
	private final Long2ObjectMap<Node> nodes;
	private final PriorityQueue<WireNode> poweredWires;
	
	private WireBlock wireBlock;
	private IntProperty powerProperty;
	private int minPower;
	private int maxPower;
	
	public WireHandler(ServerWorld world) {
		this.world = world;
		
		this.network = new ArrayList<>();
		this.nodes = new Long2ObjectOpenHashMap<>();
		this.poweredWires = new PriorityQueue<>();
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
		return node != null && node.isWire() ? node.asWire() : null;
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
		
		List<BlockPos> updatedWires = new ArrayList<>();
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		letPowerFlow(updatedWires, blockUpdates);
		t = System.nanoTime();
		AlternateCurrentMod.LOGGER.info("let power flow: " + (t - s));
		s = t;
		
		nodes.clear();
		poweredWires.clear();
		
		updateNeighbors(wireBlock, wire.pos, updatedWires, blockUpdates);
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
			collectNeighbors(wire);
			
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
	
	private void collectNeighbors(WireNode wire) {
		Mutable pos = new Mutable();
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			pos.set(wire.pos, dir);
			
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
	
	private int getWirePower(WireNode wire) {
		int power = minPower;
		
		for (BlockPos pos : wire.connectionsIn) {
			WireNode connectedWire = getOrAddWire(pos);
			
			if (connectedWire != null && !connectedWire.inNetwork) {
				power = Math.max(power, connectedWire.power - 1);
			}
		}
		
		return power;
	}
	
	private void letPowerFlow(Collection<BlockPos> updatedWires, Collection<BlockPos> blockUpdates) {
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
			
			updatedWires.add(wire.pos);
			collectNeighborPositions(wire.pos, blockUpdates);
			
			for (BlockPos pos : wire.connectionsOut) {
				WireNode connectedWire = getWire(pos);
				
				if (connectedWire != null && connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					connectedWire.power = nextPower;
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
			wire.state = newState;
			BlockPos pos = wire.pos;
			
			world.setBlockState(pos, newState, 18);
		}
	}
	
	private void updateNeighbors(WireBlock wireBlock, BlockPos sourcePos, Collection<BlockPos> updatedWires, Collection<BlockPos> blockUpdates) {
		long s = System.nanoTime();
		blockUpdates.removeAll(updatedWires);
		AlternateCurrentMod.LOGGER.info("removing wire positions: "  + (System.nanoTime() - s));
		
		dispatchShapeUpdates(wireBlock, updatedWires);
		dispatchBlockUpdates(wireBlock, sourcePos, blockUpdates);
	}
	
	private void dispatchShapeUpdates(WireBlock wireBlock, Collection<BlockPos> updatedWires) {
		long s = System.nanoTime();
		
		Mutable neighborPos = new Mutable();
		
		for (BlockPos wirePos : updatedWires) {
			BlockState wireState = world.getBlockState(wirePos);
			
			if (!wireBlock.isOf(wireState)) {
				continue;
			}
			
			for (int index = 0; index < Directions.ALL.length; index++) {
				Direction dir = Directions.ALL[index];
				neighborPos.set(wirePos, dir);
				BlockState prevState = world.getBlockState(neighborPos);
				
				// Shape updates to redstone wires are super expensive
				// and should never happen as a result of power changes
				// anyway.
				if (!wireBlock.isOf(prevState)) {
					BlockState newState = prevState.getStateForNeighborUpdate(dir.getOpposite(), wireState, world, neighborPos, wirePos);
					Block.replace(prevState, newState, world, neighborPos, 2);
				}
			}
		}
		
		AlternateCurrentMod.LOGGER.info("shape updates: " + (System.nanoTime() - s));
	}
	
	private void dispatchBlockUpdates(WireBlock wireBlock, BlockPos sourcePos, Collection<BlockPos> blockUpdates) {
		long s = System.nanoTime();
		
		Block block = wireBlock.asBlock();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, block, sourcePos);
		}
		
		AlternateCurrentMod.LOGGER.info("block updates: " + (System.nanoTime() - s));
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
