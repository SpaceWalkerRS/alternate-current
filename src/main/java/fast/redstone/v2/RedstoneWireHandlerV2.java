package fast.redstone.v2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
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

public class RedstoneWireHandlerV2 {
	
	private static final Direction[] DIRECTIONS = new Direction[] {
			Direction.WEST,
			Direction.EAST,
			Direction.NORTH,
			Direction.SOUTH,
			Direction.DOWN,
			Direction.UP
	};
	
	public final int MAX_POWER = 15;
	
	private final Block wireBlock;
	
	private final List<WireV2> wires;
	private final Set<BlockPos> network;
	private final Map<BlockPos, Neighbor> neighbors;
	private final Queue<WireV2> poweredWires;
	private final List<BlockPos> updatedWires;
	private final Set<BlockPos> blockUpdates;
	
	private World world;
	private boolean updatingPower;
	
	public RedstoneWireHandlerV2(Block wireBlock) {
		if (!(wireBlock instanceof IWireBlock)) {
			throw new IllegalArgumentException(String.format("The given Block must implement %s", IWireBlock.class));
		}
		
		this.wireBlock = wireBlock;
		
		this.wires = new ArrayList<>();
		this.network = new HashSet<>();
		this.neighbors = new HashMap<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
		this.blockUpdates = new LinkedHashSet<>();
	}
	
	public void updatePower(WireV2 wire) {
		if (updatingPower) {
			return;
		}
		
		this.world = wire.getWorld();
		
		buildNetwork(wire);
		findPoweredWires(wire);
		letPowerFlow();
		
		System.out.println("collecting neighbor positions");
		long start = System.nanoTime();
		
		queueBlockUpdates();
		blockUpdates.removeAll(network);
		List<BlockPos> positions = new ArrayList<>(blockUpdates);
		
		network.clear();
		neighbors.clear();
		blockUpdates.clear();
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("updating neighbors");
		start = System.nanoTime();
		
		BlockPos sourcePos = wire.getPos();
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, wireBlock, sourcePos);
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void buildNetwork(WireV2 sourceWire) {
		System.out.println("building network");
		long start = System.nanoTime();
		
		addToNetwork(sourceWire);
		
		for (int index = 0; index < wires.size(); index++) {
			WireV2 wire = wires.get(index);
			
			for (WireV2 connectedWire : wire.getConnectionsOut()) {
				if (!connectedWire.inNetwork()) {
					addToNetwork(connectedWire);
					addNeighbor(connectedWire.asNeighbor());
				}
			}
			
			findNeighbors(wire);
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void addToNetwork(WireV2 wire) {
		wires.add(wire);
		network.add(wire.getPos());
		
		wire.addToNetwork();
	}
	
	private void findNeighbors(WireV2 wire) {
		List<Neighbor> neighbors = wire.getNeighbors();
		neighbors.clear();
		
		for (Direction dir : DIRECTIONS) {
			BlockPos side = wire.getPos().offset(dir);
			Neighbor neighbor = getOrAddNeighbor(side);
			
			neighbors.add(neighbor);
		}
	}
	
	private Neighbor getNeighbor(BlockPos pos) {
		return neighbors.get(pos);
	}
	
	private Neighbor getOrAddNeighbor(BlockPos pos) {
		Neighbor neighbor = getNeighbor(pos);
		return neighbor == null ? addNeighbor(pos) : neighbor;
	}
	
	private Neighbor addNeighbor(BlockPos pos) {
		return addNeighbor(createNeighbor(pos, world.getBlockState(pos)));
	}
	
	private Neighbor addNeighbor(Neighbor neighbor) {
		neighbors.put(neighbor.getPos(), neighbor);
		return neighbor;
	}
	
	private Neighbor createNeighbor(BlockPos pos, BlockState state) {
		NeighborType type;
		
		if (state.isOf(wireBlock)) {
			type = NeighborType.WIRE;
		} else if (state.isSolidBlock(world, pos)) {
			type = NeighborType.SOLID_BLOCK;
		} else if (state.emitsRedstonePower()) {
			type = NeighborType.REDSTONE_COMPONENT;
		} else {
			type = NeighborType.OTHER;
		}
		
		return new Neighbor(type, pos, state);
	}
	
	private void findPoweredWires(WireV2 sourceWire) {
		System.out.println("finding powered wires");
		long start = System.nanoTime();
		
		for (WireV2 wire : wires) {
			int power = getReceivedPower(wire);
			wire.setPower(power);
			
			if (power > 0) {
				addPowerSource(wire);
			}
		}
		
		addPowerSource(sourceWire);
		wires.clear();
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private int getReceivedPower(WireV2 wire) {
		int externalPower = getExternalPower(wire);
		
		if (externalPower >= MAX_POWER) {
			return MAX_POWER;
		}
		
		int powerFromWires = getPowerFromConnections(wire);
		
		if (powerFromWires > externalPower) {
			return powerFromWires;
		}
		
		return externalPower;
	}
	
	private int getExternalPower(WireV2 wire) {
		int power = 0;
		
		int index = 0;
		for (Neighbor neighbor : wire.getNeighbors()) {
			int powerFromNeighbor = 0;
			
			if (neighbor.getType() == NeighborType.SOLID_BLOCK) {
				powerFromNeighbor = getStrongPowerTo(neighbor, DIRECTIONS[index].getOpposite());
			} else if (neighbor.getType() == NeighborType.REDSTONE_COMPONENT) {
				powerFromNeighbor = neighbor.getState().getWeakRedstonePower(world, neighbor.getPos(), DIRECTIONS[index]);
			}
			
			if (powerFromNeighbor > power) {
				power = powerFromNeighbor;
				
				if (power >= MAX_POWER) {
					return MAX_POWER;
				}
			}
			
			index++;
		}
		
		return power;
	}
	
	private int getStrongPowerTo(Neighbor block, Direction ignore) {
		int power = 0;
		
		for (Direction dir : DIRECTIONS) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = block.getPos().offset(dir);
			Neighbor neighbor = getOrAddNeighbor(side);
			
			if (neighbor.getType() == NeighborType.REDSTONE_COMPONENT) {
				int powerFromSide = neighbor.getState().getStrongRedstonePower(world, side, dir);
				
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
	
	private int getPowerFromConnections(WireV2 wire) {
		int power = 0;
		
		for (WireV2 connectedWire : wire.getConnectionsIn()) {
			if (!connectedWire.inNetwork()) {
				int powerFromWire = connectedWire.getPower() - 1;
				
				if (powerFromWire > power) {
					power = powerFromWire;
				}
			}
		}
		
		return power;
	}
	
	private void addPowerSource(WireV2 wire) {
		poweredWires.add(wire);
		wire.addPowerSource();
	}
	
	private void letPowerFlow() {
		System.out.println("updating power");
		long start = System.nanoTime();
		
		updatingPower = true;
		
		while (!poweredWires.isEmpty()) {
			WireV2 wire = poweredWires.poll();
			
			if (!wire.inNetwork()) {
				continue;
			}
			
			int nextPower = wire.getPower() - 1;
			
			wire.removeFromNetwork();
			wire.removePowerSource();
			
			updateWireState(wire);
			
			for (WireV2 connectedWire : wire.getConnectionsOut()) {
				if (connectedWire.inNetwork() && (!connectedWire.isPowerSource() || nextPower > connectedWire.getPower())) {
					connectedWire.setPower(nextPower);
					addPowerSource(connectedWire);
				}
			}
		}
		
		updatingPower = false;
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void updateWireState(WireV2 wire) {
		if (wire.getPower() < 0) {
			wire.setPower(0);
		}
		
		BlockState oldState = wire.getState();
		BlockState newState = oldState.with(Properties.POWER, wire.getPower());
		
		if (newState != oldState) {
			BlockPos pos = wire.getPos();
			
			world.setBlockState(pos, newState, 18);
			updatedWires.add(pos);
			emitShapeUpdates(wire);
		}
	}
	
	private void emitShapeUpdates(WireV2 wire) {
		int index = 0;
		for (Neighbor neighbor : wire.getNeighbors()) {
			if (!neighbor.isWire()) {
				BlockPos pos = neighbor.getPos();
				Direction dir = DIRECTIONS[index].getOpposite();
				
				BlockState oldState = neighbor.getState();
				BlockState newState = oldState.getStateForNeighborUpdate(dir, oldState, world, pos, wire.getPos());
				
				if (newState != oldState) {
					neighbor.updateState(newState);
					Block.replace(oldState, newState, world, pos, 2);
				}
			}
			
			index++;
		}
	}
	
	private void queueBlockUpdates() {
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			collectNeighborPositions(updatedWires.get(index), blockUpdates);
		}
		
		updatedWires.clear();
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
}
