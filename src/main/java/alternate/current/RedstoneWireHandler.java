package alternate.current;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import alternate.current.interfaces.mixin.IWorld;
import alternate.current.utils.Directions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RedstoneWireHandler {
	
	public final int MAX_POWER = 15;
	
	private final Block wireBlock;
	
	private final List<Wire> network;
	private final Map<BlockPos, Wire> wires;
	private final Map<BlockPos, Neighbor> neighbors;
	private final Queue<Wire> poweredWires;
	private final List<BlockPos> updatedWires;
	
	private World world;
	private boolean updatingPower;
	
	public RedstoneWireHandler(Block wireBlock) {
		this.wireBlock = wireBlock;
		
		this.network = new ArrayList<>();
		this.wires = new HashMap<>();
		this.neighbors = new HashMap<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
	}
	
	public void updatePower(Wire wire) {
		if (updatingPower) {
			if (wire.world != world) {
				return; // This should never happen
			}
			
			buildNetwork(wire);
			findPoweredWires(wire);
			
			return;
		}
		long s = System.nanoTime();
		
		world = wire.world;
		
		buildNetwork(wire);
		findPoweredWires(wire);
		letPowerFlow();
		
		System.out.println("collecting neighbor positions");
		long start = System.nanoTime();
		
		Collection<BlockPos> blockUpdates = queueBlockUpdates();
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("updating neighbors");
		start = System.nanoTime();
		
		wires.clear();
		neighbors.clear();
		updatedWires.clear();
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, wireBlock, wire.pos);
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("total: " + (System.nanoTime() - s));
	}
	
	private void buildNetwork(Wire sourceWire) {
		System.out.println("building network");
		long start = System.nanoTime();
		
		addWire(sourceWire.pos);
		addToNetwork(sourceWire);
		
		for (int index = 0; index < network.size(); index++) {
			Wire wire = network.get(index);
			
			for (BlockPos pos : wire.connectionsOut) {
				Wire connectedWire = getOrAddWire(pos);
				
				if (connectedWire != null && !connectedWire.inNetwork) {
					collectNeighbors(connectedWire);
					addToNetwork(connectedWire);
				}
			}
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private Wire getWire(BlockPos pos) {
		return wires.get(pos);
	}
	
	private Wire getOrAddWire(BlockPos pos) {
		Wire wire = getWire(pos);
		return wire == null ? addWire(pos) : wire;
	}
	
	private Wire addWire(BlockPos pos) {
		Wire wire = ((IWorld)world).getWire(pos);
		
		if (wire == null) {
			return null;
		}
		
		Neighbor neighbor = new Neighbor();
		neighbor.update(NeighborType.WIRE, wire.pos, wire.state);
		addNeighbor(neighbor);
		
		return addWire(wire);
	}
	
	private Wire addWire(Wire wire) {
		wires.put(wire.pos, wire);
		return wire;
	}
	
	private void addToNetwork(Wire wire) {
		network.add(wire);
		wire.inNetwork = true;
	}
	
	private void collectNeighbors(Wire wire) {
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = wire.pos.offset(dir);
			
			wire.neighbors[index] = getOrAddNeighbor(side);
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
		return addNeighbor(pos, world.getBlockState(pos));
	}
	
	private Neighbor addNeighbor(BlockPos pos, BlockState state) {
		Neighbor neighbor = Neighbor.of(world, pos, state, wireBlock);
		
		if (neighbor.type == NeighborType.WIRE) {
			Wire wire = ((IWorld)world).getWire(pos);
			
			if (wire != null) {
				addWire(wire);
			}
		}
		
		return addNeighbor(neighbor);
	}
	
	private Neighbor addNeighbor(Neighbor neighbor) {
		neighbors.put(neighbor.pos, neighbor);
		return neighbor;
	}
	
	private void findPoweredWires(Wire sourceWire) {
		System.out.println("finding powered wires");
		long start = System.nanoTime();
		
		for (Wire wire : network) {
			wire.power = getReceivedPower(wire);
			
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
	
	private void addPowerSource(Wire wire) {
		poweredWires.add(wire);
		wire.isPowerSource = true;
	}
	
	public int getReceivedPower(Wire wire) {
		int externalPower = getExternalPower(wire);
		
		if (externalPower >= MAX_POWER) {
			return MAX_POWER;
		}
		
		int wirePower = getWirePower(wire);
		
		if (wirePower > externalPower) {
			return wirePower;
		}
		
		return externalPower;
	}
	
	private int getExternalPower(Wire wire) {
		int power = 0;
		
		for (int index = 0; index < Directions.ALL.length; index++) {
			Neighbor neighbor = wire.neighbors[index];
			int powerFromNeighbor = 0;
			
			if (neighbor.type == NeighborType.SOLID_BLOCK) {
				powerFromNeighbor = getStrongPowerTo(neighbor.pos, Directions.ALL[index].getOpposite());
			} else if (neighbor.type == NeighborType.REDSTONE_COMPONENT) {
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
	
	private int getStrongPowerTo(BlockPos pos, Direction ignore) {
		int power = 0;
		
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			Neighbor neighbor = getOrAddNeighbor(side);
			
			if (neighbor.type == NeighborType.REDSTONE_COMPONENT) {
				int powerFromNeighbor = neighbor.state.getStrongRedstonePower(world, side, dir);
				
				if (powerFromNeighbor > power) {
					power = powerFromNeighbor;
					
					if (power >= MAX_POWER) {
						return MAX_POWER;
					}
				}
			}
		}
		
		return power;
	}
	
	private int getWirePower(Wire wire) {
		int power = 0;
		
		for (BlockPos pos : wire.connectionsIn) {
			Wire connectedWire = getOrAddWire(pos);
			
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
			
			for (BlockPos pos : wire.connectionsOut) {
				Wire connectedWire = wires.get(pos);
				
				if (connectedWire != null && connectedWire.inNetwork && (!connectedWire.isPowerSource || nextPower > connectedWire.power)) {
					connectedWire.power = nextPower;
					addPowerSource(connectedWire);
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
		
		BlockState oldState = wire.state;
		BlockState newState = oldState.with(Properties.POWER, wire.power);
		
		if (newState != oldState) {
			world.setBlockState(wire.pos, newState, 18);
			updatedWires.add(wire.pos);
			emitShapeUpdates(wire);
		}
	}
	
	private void emitShapeUpdates(Wire wire) {
		BlockPos pos = wire.pos;
		
		for (Direction dir : Directions.ALL) {
			BlockPos side = pos.offset(dir);
			BlockState oldState = world.getBlockState(side);
			
			if (oldState.isOf(wireBlock) && wires.containsKey(side)) {
				continue;
			}
			
			BlockState newState = oldState.getStateForNeighborUpdate(dir.getOpposite(), wire.state, world, side, pos);
			Block.replace(oldState, newState, world, side, 2);
		}
	}
	
	private Collection<BlockPos> queueBlockUpdates() {
		Set<BlockPos> blockUpdates = new LinkedHashSet<>();
		
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			collectNeighborPositions(updatedWires.get(index), blockUpdates);
		}
		blockUpdates.removeAll(updatedWires);
		
		return blockUpdates;
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
