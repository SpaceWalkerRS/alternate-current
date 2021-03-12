package fast.redstone;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import fast.redstone.interfaces.mixin.IRedstoneWire;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class RedstoneWireHandler {
	
	private final Block wireBlock;
	private final World world;
	private final Map<BlockPos, Integer> chunkCoords;
	
	private final Map<BlockPos, Wire> network;
	private final Queue<Wire> poweredWires;
	private final Set<BlockPos> updatedWires;
	private final Set<BlockPos> blockUpdates;
	
	private boolean isUpdatingPower;
	
	public RedstoneWireHandler(World world) {
		this(Blocks.REDSTONE_WIRE, world);
	}
	
	public RedstoneWireHandler(Block wireBlock, World world) {
		this.wireBlock = wireBlock;
		this.world = world;
		this.chunkCoords = new HashMap<>();
		
		this.network = new HashMap<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new HashSet<>();
		this.blockUpdates = new LinkedHashSet<>();
	}
	
	public RedstoneWireHandler(World world, BlockPos pos, BlockState state) {
		this(Blocks.REDSTONE_WIRE, world, pos, state);
	}
	
	public RedstoneWireHandler(Block wireBlock, World world, BlockPos pos, BlockState state) {
		this(wireBlock, world);
		
		updateNetworkConnections(pos, state);
	}
	
	public Set<BlockPos> getChunkCoords(){
		return chunkCoords.keySet();
	}
	
	private BlockPos getChunkPos(BlockPos pos) {
		int x = pos.getX() >> 4;
		int y = pos.getY() >> 4;
		int z = pos.getZ() >> 4;
		
		return new BlockPos(x, y, z);
	}
	
	private void wireAdded(BlockPos blockPos) {
		chunkCoords.compute(getChunkPos(blockPos), (pos, count) -> count == null ? 1 : count + 1);
	}
	
	private void wireRemoved(BlockPos blockPos) {
		chunkCoords.compute(getChunkPos(blockPos), (pos, count) -> count == 1 ? null : count - 1);
	}
	
	public boolean isPosInNetwork(BlockPos pos) {
		return network.containsKey(pos);
	}
	
	public void updateNetworkConnections(BlockPos pos, BlockState state) {
		if (isUpdatingPower) {
			return;
		}
		
		Wire wire = network.get(pos);
		
		if (wire == null) {
			if (!state.isOf(wireBlock)) {
				return;
			}
			
			wire = addWireToNetwork(pos, state);
		} else {
			wire.updateState(state);
		}
		
		wire.updateConnections(true);
	}
	
	private Wire addWireToNetwork(BlockPos pos, BlockState state) {
		Wire wire = new Wire(pos, state);
		
		network.put(pos, wire);
		wireAdded(pos);
		
		return wire;
	}
	
	private Wire getOrAddWire(BlockPos pos, BlockState state) {
		Wire wire = network.get(pos);
		
		if (wire == null) {
			return addWireToNetwork(pos, state);
		}
		
		wire.updateState(state);
		
		return wire;
	}
	
	private void removeWireFromNetwork(Wire wire) {
		if (network.remove(wire.pos, wire) && chunkCoords.containsKey(wire.pos)) {
			wireRemoved(wire.pos);
		}
	}
	
	public void updateNetworkPower(BlockPos sourcePos) {
		if (isUpdatingPower) {
			return;
		}
		
		Wire sourceWire = network.get(sourcePos);
		
		if (sourceWire == null) {
			return;
		}
		
		collectPowerSources(sourceWire);
		letPowerFlow();
		
		blockUpdates.removeAll(updatedWires);
		
		for (BlockPos pos : blockUpdates) {
			world.updateNeighbor(pos, wireBlock, sourcePos);
		}
		
		cleanUp();
	}
	
	private void collectPowerSources(Wire sourceWire) {
		((IRedstoneWire)wireBlock).setWiresGivePower(false);
		
		addToCurrentSubNetwork(sourceWire, 0);
		
		((IRedstoneWire)wireBlock).setWiresGivePower(true);
	}
	
	private void addToCurrentSubNetwork(Wire wire, int range) {
		if (wire.inCurrentSubNetwork) {
			return;
		}
		
		wire.inCurrentSubNetwork = true;
		
		if ((range = wire.setInitialPower(range)) < 0) {
			return;
		}
		
		for (WireConnection connection : wire.connections) {
			if (connection.out) {
				addToCurrentSubNetwork(connection.connectedWire, range);
			}
		}
		
		wire.validatePower();
	}
	
	private void letPowerFlow() {
		isUpdatingPower = true;
		
		Wire wire;
		while ((wire = poweredWires.poll()) != null) {
			wire.updatePowerInWorld();
		}
		
		isUpdatingPower = false;
	}
	
	private void cleanUp() {
		poweredWires.clear();
		updatedWires.clear();
		blockUpdates.clear();
	}
	
	private class Wire implements Comparable<Wire> {
		
		private final BlockPos pos;
		private final List<WireConnection> connections;
		
		private BlockState state;
		private int power;
		
		private boolean inCurrentSubNetwork;
		
		public Wire(BlockPos pos, BlockState state) {
			this.pos = pos;
			this.connections = new ArrayList<>();
			
			this.state = state;
			this.power = state.get(Properties.POWER);
		}
		
		@Override
		public int hashCode() {
			return pos.hashCode();
		}
		
		@Override
		public int compareTo(Wire wire) {
			int c = Integer.compare(wire.power, power);
			
			if (c == 0) {
				c = Integer.compare(pos.getY(), wire.pos.getY());
				
				if (c == 0) {
					c = Integer.compare(pos.getZ(), wire.pos.getZ());
					
					if (c == 0) {
						c = Integer.compare(pos.getX(), wire.pos.getX());
					}
				}
			}
			
			return c;
		}
		
		public void updateState(BlockState newState) {
			state = newState;
			
			if (!state.isOf(wireBlock)) {
				removeWireFromNetwork(this);
				
				List<WireConnection> oldConnections = new ArrayList<>(connections);
				connections.clear();
				
				onConnectionsChanged(oldConnections);
			}
		}
		
		public void updateConnections(boolean propagate) {
			List<WireConnection> oldConnections = new ArrayList<>(connections);
			connections.clear();
			
			for (Direction dir : Direction.Type.HORIZONTAL) {
				BlockPos side = pos.offset(dir);
				BlockState sideState = world.getBlockState(side);
				
				if (sideState.isOf(wireBlock)) {
					addConnection(side, sideState, true, true);
					
					continue;
				}
				
				boolean sideIsSolid = sideState.isSolidBlock(world, side);
				
				BlockPos above = pos.up();
				BlockState aboveState = world.getBlockState(above);
				
				if (!aboveState.isSolidBlock(world, above)) {
					BlockPos aboveSide = side.up();
					BlockState aboveSideState = world.getBlockState(aboveSide);
					
					if (aboveSideState.isOf(wireBlock)) {
						addConnection(aboveSide, aboveSideState, true, sideIsSolid);
					}
				}
				
				if (!sideIsSolid) {
					BlockPos belowSide = side.down();
					BlockState belowSideState = world.getBlockState(belowSide);
					
					if (belowSideState.isOf(wireBlock)) {
						BlockPos below = pos.down();
						BlockState belowState = world.getBlockState(below);
						
						boolean belowIsSolid = belowState.isSolidBlock(world, below);
						
						addConnection(belowSide, belowSideState, belowIsSolid, true);
					}
				}
			}
			
			if (propagate) {
				onConnectionsChanged(oldConnections);
			}
		}
		
		private void addConnection(BlockPos pos, BlockState state, boolean out, boolean in) {
			connections.add(new WireConnection(getOrAddWire(pos, state), out, in));
		}
		
		private void onConnectionsChanged(List<WireConnection> oldConnections) {
			for (WireConnection newConnection : connections) {
				Iterator<WireConnection> it = oldConnections.iterator();
				
				boolean found = false;
				
				while (it.hasNext() && !found) {
					WireConnection oldConnection = it.next();
					
					if (oldConnection.connectedWire == newConnection.connectedWire) {
						if (oldConnection.out != newConnection.out || oldConnection.in != newConnection.in) {
							oldConnection.connectedWire.updateConnections(false);
						}
						
						it.remove();
						
						found = true;
					}
				}
				
				if (!found) {
					newConnection.connectedWire.updateConnections(false);
				}
			}
			
			for (WireConnection oldConnection : oldConnections) {
				oldConnection.connectedWire.updateConnections(false);
			}
		}
		
		public int setInitialPower(int range) {
			int externalPower = world.getReceivedRedstonePower(pos);
			
			if (power != externalPower) {
				if (externalPower != 0) {
					range = (power > externalPower) ? (power - externalPower) : (externalPower - power);
				}
				
				power = externalPower;
			}
			
			return range - 1;
		}
		
		private int getPowerFromWires() {
			int power = 0;
			
			for (WireConnection connection : connections) {
				Wire wire = connection.connectedWire;
				
				if (connection.in && !wire.inCurrentSubNetwork) {
					if (power < wire.power) {
						power = wire.power - 1;
					}
				}
				
				if (power >= 15) {
					break;
				}
			}
			
			return power;
		}
		
		public void validatePower() {
			int wirePower = getPowerFromWires();
			
			if (wirePower > power) {
				power = wirePower;
			}
			
			if (power > 0) {
				poweredWires.add(this);
			}
		}
		
		public void updatePowerInWorld() {
			if (inCurrentSubNetwork) {
				inCurrentSubNetwork = false;
				
				if (power < 0) {
					power = 0;
				}
				
				BlockState newState = state.with(Properties.POWER, power);
				
				if (state != newState) {
					state = newState;
					
					world.setBlockState(pos, state, 2);
					
					queueBlockUpdates();
				}
				
				updatedWires.add(pos);
			}
		}
		
		private void queueBlockUpdates() {
			BlockPos down = pos.down();
			BlockPos up = pos.up();
			BlockPos north = pos.north();
			BlockPos south = pos.south();
			BlockPos west = pos.west();
			BlockPos east = pos.east();
			
			blockUpdates.add(down);
			blockUpdates.add(up);
			blockUpdates.add(north);
			blockUpdates.add(south);
			blockUpdates.add(west);
			blockUpdates.add(east);
			
			blockUpdates.add(down.north());
			blockUpdates.add(up.south());
			blockUpdates.add(down.south());
			blockUpdates.add(up.north());
			blockUpdates.add(down.west());
			blockUpdates.add(up.east());
			blockUpdates.add(down.east());
			blockUpdates.add(up.west());
			
			blockUpdates.add(north.west());
			blockUpdates.add(south.east());
			blockUpdates.add(west.south());
			blockUpdates.add(east.north());
			
			blockUpdates.add(down.down());
			blockUpdates.add(up.up());
			blockUpdates.add(north.north());
			blockUpdates.add(south.south());
			blockUpdates.add(west.west());
			blockUpdates.add(east.east());
		}
	}
	
	private class WireConnection {
		
		public final Wire connectedWire;
		// true if power can flow from this wire to the connected wire
		public final boolean out;
		// true if power can flow from the connected wire to this wire
		public final boolean in;
		
		public WireConnection(Wire wire, boolean out, boolean in) {
			this.connectedWire = wire;
			this.out = out;
			this.in = in;
		}
	}
}
