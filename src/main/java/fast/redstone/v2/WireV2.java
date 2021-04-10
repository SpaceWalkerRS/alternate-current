package fast.redstone.v2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fast.redstone.interfaces.mixin.IWireBlock;
import fast.redstone.utils.CollectionsUtils;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WireV2 implements Comparable<WireV2> {
	
	private final Block wireBlock;
	private final World world;
	private final BlockPos pos;
	private final List<Neighbor> neighbors;
	private final List<WireV2> connectionsOut;
	private final List<WireV2> connectionsIn;
	
	private BlockState state;
	private int power;
	
	private boolean ignoreUpdates;
	private boolean inNetwork;
	private boolean isPowerSource;
	
	public WireV2(World world, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof IWireBlock)) {
			throw new IllegalArgumentException(String.format("The given BlockState must be of a Block that implements %s", IWireBlock.class));
		}
		
		this.wireBlock = state.getBlock();
		this.world = world;
		this.pos = pos.toImmutable(); // Sometimes this BlockPos is actually a Mutable...
		this.neighbors = new ArrayList<>();
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
		
		this.state = state;
		this.power = state.get(Properties.POWER);
	}
	
	@Override
	public int compareTo(WireV2 wire) {
		return Integer.compare(wire.power, power);
	}
	
	public Block getWireBlock() {
		return wireBlock;
	}
	
	public World getWorld() {
		return world;
	}
	
	public BlockPos getPos() {
		return pos;
	}
	
	public List<Neighbor> getNeighbors() {
		return neighbors;
	}
	
	public Neighbor asNeighbor() {
		return new Neighbor(NeighborType.WIRE, pos, state);
	}
	
	public List<WireV2> getConnectionsOut() {
		return Collections.unmodifiableList(connectionsOut);
	}
	
	public List<WireV2> getConnectionsIn() {
		return Collections.unmodifiableList(connectionsIn);
	}
	
	public BlockState getState() {
		return state;
	}
	
	public void updateState(BlockState state) {
		if (!state.isOf(wireBlock)) {
			throw new IllegalArgumentException(String.format("The given BlockState must be of block %s", wireBlock.getClass()));
		}
		
		this.state = state;
	}
	
	public int getPower() {
		return power;
	}
	
	public void setPower(int power) {
		this.power = power;
	}
	
	public void remove() {
		this.ignoreUpdates = true;
	}
	
	public boolean inNetwork() {
		return inNetwork;
	}
	
	public void addToNetwork() {
		inNetwork = true;
	}
	
	public void removeFromNetwork() {
		inNetwork = false;
	}
	
	public boolean isPowerSource() {
		return isPowerSource;
	}
	
	public void addPowerSource() {
		isPowerSource = true;
	}
	
	public void removePowerSource() {
		isPowerSource = false;
	}
	
	public void updateConnections() {
		if (ignoreUpdates) {
			return;
		}
		
		List<WireV2> oldConnectionsOut = new ArrayList<>(connectionsOut);
		List<WireV2> oldConnectionsIn = new ArrayList<>(connectionsIn);
		
		connectionsOut.clear();
		connectionsIn.clear();
		
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
		
		connectionsChanged(oldConnectionsOut, oldConnectionsIn);
	}
	
	private void addConnection(BlockPos pos, BlockState state, boolean out, boolean in) {
		WireV2 wire = ((IWireBlock)wireBlock).getWireV2(world, pos, state, true, false);
		
		if (out) {
			connectionsOut.add(wire);
		}
		if (in) {
			connectionsIn.add(wire);
		}
	}
	
	private void connectionsChanged(List<WireV2> oldConnectionsOut, List<WireV2> oldConnectionsIn) {
		Set<WireV2> affectedWires = new HashSet<>();
		
		affectedWires.addAll(CollectionsUtils.difference(oldConnectionsOut, connectionsOut));
		affectedWires.addAll(CollectionsUtils.difference(oldConnectionsIn, connectionsIn));
		
		ignoreUpdates = true;
		
		for (WireV2 wire : affectedWires) {
			wire.updateConnections();
		}
		
		ignoreUpdates = false;
	}
}
