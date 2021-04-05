package fast.redstone;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fast.redstone.interfaces.mixin.IWireBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Wire implements Comparable<Wire> {
	
	private final Block wireBlock;
	private final World world;
	private final BlockPos pos;
	private final List<WireConnection> connections;
	
	private BlockState state;
	private int power;
	
	private boolean ignoreUpdates;
	private boolean inNetwork;
	private boolean isPowerSource;
	
	public Wire(World world, BlockPos pos, BlockState state) {
		this.wireBlock = state.getBlock();
		
		if (!(this.wireBlock instanceof IWireBlock)) {
			throw new IllegalArgumentException(String.format("The given BlockState must be of a block that implements %s!", IWireBlock.class));
		}
		
		this.world = world;
		this.pos = pos.toImmutable(); // Sometimes this BlockPos is actually a Mutable...
		this.connections = new ArrayList<>();
		
		this.state = state;
		this.power = state.get(Properties.POWER);
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
	
	public Block getWireBlock() {
		return wireBlock;
	}
	
	public World getWorld() {
		return world;
	}
	
	public BlockPos getPos() {
		return pos;
	}
	
	public List<WireConnection> getConnections() {
		return connections;
	}
	
	public BlockState getState() {
		return state;
	}
	
	public void updateState(BlockState state) {
		if (!state.isOf(wireBlock)) {
			throw new IllegalArgumentException(String.format("The given BlockState must be of block %s", wireBlock));
		}
		
		this.state = state;
	}
	
	public int getPower() {
		return power;
	}
	
	public void setPower(int power) {
		this.power = power;
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
		
		connectionsChanged(oldConnections);
	}
	
	private void addConnection(BlockPos pos, BlockState state, boolean out, boolean in) {
		Wire wire = ((IWireBlock)wireBlock).getWire(world, pos, state, true, false);
		WireConnection connection = new WireConnection(wire, out, in);
		
		connections.add(connection);
	}
	
	private void connectionsChanged(List<WireConnection> oldConnections) {
		ignoreUpdates = true;
		
		for (WireConnection connection : connections) {
			boolean changed = true;
			
			Iterator<WireConnection> it = oldConnections.iterator();
			while (it.hasNext()) {
				WireConnection oldConnection = it.next();
				
				if (connection.wire == oldConnection.wire) {
					if (connection.out == oldConnection.out && connection.in == oldConnection.in) {
						changed = false;
					}
					
					it.remove();
				}
			}
			
			if (changed) {
				connection.wire.updateConnections();
			}
		}
		
		for (WireConnection oldConnection : oldConnections) {
			oldConnection.wire.updateConnections();
		}
		
		ignoreUpdates = false;
	}
}
