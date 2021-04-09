package fast.redstone.v1;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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

public class RedstoneWireHandlerV1 {
	
	public final int MAX_POWER = 15;
	
	private final Block wireBlock;
	private final List<WireV1> wires;
	private final Set<BlockPos> network;
	private final Queue<WireV1> poweredWires;
	private final List<BlockPos> updatedWires;
	private final Set<BlockPos> blockUpdates;
	
	private World world;
	private boolean updatingPower;
	
	public RedstoneWireHandlerV1(Block wireBlock) {
		this.wireBlock = wireBlock;
		this.wires = new ArrayList<>();
		this.network = new HashSet<>();
		this.poweredWires = new PriorityQueue<>();
		this.updatedWires = new ArrayList<>();
		this.blockUpdates = new LinkedHashSet<>();
	}
	
	public void updatePower(WireV1 sourceWire) {
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
		
		System.out.println("collecting neighbor positions");
		long start = System.nanoTime();
		
		queueBlockUpdates();
		blockUpdates.removeAll(network);
		List<BlockPos> positions = new ArrayList<>(blockUpdates);
		
		network.clear();
		blockUpdates.clear();
		
		System.out.println("t: " + (System.nanoTime() - start));
		System.out.println("updating neighbors");
		start = System.nanoTime();
		
		BlockPos sourcePos = sourceWire.getPos();
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, wireBlock, sourcePos);
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void buildNetwork(WireV1 sourceWire) {
		System.out.println("building network");
		long start = System.nanoTime();
		
		addToNetwork(sourceWire);
		updateNetwork();
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void updateNetwork() {
		for (int index = 0; index < wires.size(); index++) {
			WireV1 wire = wires.get(index);
			
			for (WireConnectionV1 connection : wire.getConnections()) {
				if (connection.out) {
					WireV1 connectedWire = connection.wire;
					
					if (!connectedWire.inNetwork()) {
						addToNetwork(connectedWire);
					}
				}
			}
		}
	}
	
	private void addToNetwork(WireV1 wire) {
		wires.add(wire);
		network.add(wire.getPos());
		wire.addToNetwork();
	}
	
	private void findPoweredWires(WireV1 sourceWire) {
		System.out.println("finding powered wires");
		long start = System.nanoTime();
		
		((IWireBlock)wireBlock).setWiresGivePower(false);
		
		poweredWires.add(sourceWire);
		
		for (WireV1 wire : wires) {
			int power = world.getReceivedRedstonePower(wire.getPos());
			
			for (WireConnectionV1 connection : wire.getConnections()) {
				if (power >= MAX_POWER) {
					break;
				}
				
				if (connection.in) {
					WireV1 connectedWire = connection.wire;
					
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
		
		wires.clear();
		
		((IWireBlock)wireBlock).setWiresGivePower(true);
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void addPoweredWire(WireV1 wire) {
		poweredWires.add(wire);
		wire.addPowerSource();
	}
	
	private void letPowerFlow() {
		System.out.println("updating power");
		long start = System.nanoTime();
		
		while (!poweredWires.isEmpty()) {
			WireV1 wire = poweredWires.poll();
			
			if (!wire.inNetwork()) {
				continue;
			}
			
			int nextPower = wire.getPower() - 1;
			
			wire.removeFromNetwork();
			wire.removePowerSource();
			
			updateWireState(wire);
			
			for (WireConnectionV1 connection : wire.getConnections()) {
				if (connection.out) {
					WireV1 connectedWire = connection.wire;
					
					if (connectedWire.inNetwork() && !connectedWire.isPowerSource()) {
						connectedWire.setPower(nextPower);
						addPoweredWire(connectedWire);
					}
				}
			}
		}
		
		System.out.println("t: " + (System.nanoTime() - start));
	}
	
	private void updateWireState(WireV1 wire) {
		if (wire.getPower() < 0) {
			wire.setPower(0);
		}
		
		BlockState oldState = wire.getState();
		BlockState newState = oldState.with(Properties.POWER, wire.getPower());
		
		if (newState != oldState) {
			BlockPos pos = wire.getPos();
			
			world.setBlockState(pos, newState, 2);
			updatedWires.add(pos);
		}
	}
	
	private void queueBlockUpdates() {
		for (int index = updatedWires.size() - 1; index >= 0; index--) {
			queueBlockUpdates(updatedWires.get(index));
		}
		
		updatedWires.clear();
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
