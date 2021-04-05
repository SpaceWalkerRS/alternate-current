package fast.redstone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

import fast.redstone.interfaces.mixin.IWireBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RedstoneWireHandler {
	
	public final int MAX_POWER = 15;
	
	private final Block wireBlock;
	private final Queue<Wire> wires;
	private final Set<BlockPos> network;
	private final Queue<Wire> poweredWires;
	private final Set<BlockPos> blockUpdates;
	
	private World world;
	private boolean updatingPower;
	
	public RedstoneWireHandler(Block wireBlock) {
		this.wireBlock = wireBlock;
		this.wires = new LinkedList<>();
		this.network = new HashSet<>();
		this.poweredWires = new PriorityQueue<>();
		this.blockUpdates = new LinkedHashSet<>();
	}
	
	public void updatePower(Wire sourceWire) {
		if (updatingPower) {
			if (!sourceWire.inNetwork() && !network.contains(sourceWire.getPos())) {
				buildNetwork(sourceWire);
				findPoweredWires(sourceWire);
			}
			
			return;
		}
		
		world = sourceWire.getWorld();
		updatingPower = true;
		
		buildNetwork(sourceWire);
		findPoweredWires(sourceWire);
		letPowerFlow();
		
		updatingPower = false;
		
		blockUpdates.removeAll(network);
		List<BlockPos> positions = new ArrayList<>(blockUpdates);
		
		network.clear();
		blockUpdates.clear();
		
		BlockPos sourcePos = sourceWire.getPos();
		
		for (int index = positions.size() - 1; index >= 0; index--) {
			world.updateNeighbor(positions.get(index), wireBlock, sourcePos);
		}
	}
	
	private void buildNetwork(Wire sourceWire) {
		addToNetwork(sourceWire);
		updateNetwork();
	}
	
	private void updateNetwork() {
		while (!wires.isEmpty()) {
			Wire wire = wires.poll();
			
			for (WireConnection connection : wire.getConnections()) {
				if (connection.out) {
					Wire connectedWire = connection.wire;
					
					if (!connectedWire.inNetwork()) {
						addToNetwork(connectedWire);
					}
				}
			}
		}
	}
	
	private void addToNetwork(Wire wire) {
		wires.add(wire);
		network.add(wire.getPos());
		wire.addToNetwork();
	}
	
	private void findPoweredWires(Wire sourceWire) {
		((IWireBlock)wireBlock).setWiresGivePower(false);
		
		poweredWires.add(sourceWire);
		
		for (Wire wire : wires) {
			int power = world.getReceivedRedstonePower(wire.getPos());
			
			for (WireConnection connection : wire.getConnections()) {
				if (power >= MAX_POWER) {
					break;
				}
				
				if (connection.in) {
					Wire connectedWire = connection.wire;
					
					if (!connectedWire.inNetwork()) {
						int powerFromWire = connectedWire.getPower() - 1;
						
						if (powerFromWire > power) {
							power = powerFromWire;
						}
					}
				}
			}
			
			wire.setPower(power);
			
			if (power > 0) {
				poweredWires.add(wire);
			}
		}
		
		((IWireBlock)wireBlock).setWiresGivePower(true);
	}
	
	private void addPoweredWire(Wire wire) {
		poweredWires.add(wire);
		wire.addPowerSource();
	}
	
	private void letPowerFlow() {
		while (!poweredWires.isEmpty()) {
			Wire wire = poweredWires.poll();
			
			if (!wire.inNetwork()) {
				continue;
			}
			
			int nextPower = wire.getPower() - 1;
			
			wire.removeFromNetwork();
			wire.removePowerSource();
			
			updateWireState(wire);
			
			for (WireConnection connection : wire.getConnections()) {
				if (connection.out) {
					Wire connectedWire = connection.wire;
					
					if (connectedWire.inNetwork() && !connectedWire.isPowerSource()) {
						connectedWire.setPower(nextPower);
						addPoweredWire(connectedWire);
					}
				}
			}
		}
	}
	
	private void updateWireState(Wire wire) {
		if (wire.getPower() < 0) {
			wire.setPower(0);
		}
		
		BlockState oldState = wire.getState();
		BlockState newState = oldState.with(Properties.POWER, wire.getPower());
		
		if (newState != oldState) {
			BlockPos pos = wire.getPos();
			
			world.setBlockState(pos, newState, 2);
			queueBlockUpdates(pos);
		}
	}
	
	private void queueBlockUpdates(BlockPos pos) {
		blockUpdates.addAll(collectNeighborPositions(pos));
	}
	
	public List<BlockPos> collectNeighborPositions(BlockPos pos) {
		List<BlockPos> positions = new ArrayList<>();
		
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
		
		return positions;
	}
}
