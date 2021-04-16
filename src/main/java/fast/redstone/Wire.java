package fast.redstone;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fast.redstone.interfaces.mixin.IWireBlock;
import fast.redstone.utils.CollectionsUtils;
import fast.redstone.utils.Directions;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class Wire implements Comparable<Wire> {
	
	public final Block wireBlock;
	public final World world;
	public final BlockPos pos;
	public final Neighbor[] neighbors;
	public final List<BlockPos> connectionsOut;
	public final List<BlockPos> connectionsIn;
	
	public BlockState state;
	public int power;
	
	public boolean ignoreUpdates;
	public boolean inNetwork;
	public boolean isPowerSource;
	
	public Wire(World world, BlockPos pos, BlockState state) {
		Block wireBlock = state.getBlock();
		
		if (!(wireBlock instanceof IWireBlock)) {
			throw new IllegalArgumentException(String.format("The given BlockState is of a Block (%s) that does not implement %s", wireBlock, IWireBlock.class));
		}
		
		this.wireBlock = wireBlock;
		
		this.world = world;
		this.pos = pos.toImmutable(); // Sometimes this BlockPos is actually a Mutable...
		this.neighbors = new Neighbor[Directions.ALL.length];
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
		
		this.state = state;
		this.power = this.state.get(Properties.POWER);
	}
	
	@Override
	public int compareTo(Wire wire) {
		int c = Integer.compare(wire.power, power);
		
		if (c == 0) {
			c = Integer.compare(pos.getX(), wire.pos.getX());
			
			if (c == 0) {
				c = Integer.compare(pos.getZ(), wire.pos.getZ());
				
				if (c == 0) {
					c = Integer.compare(pos.getY(), wire.pos.getY());
				}
			}
		}
		
		return c;
	}
	
	public void removed() {
		ignoreUpdates = true;
	}
	
	public void updateConnections() {
		updateConnections(512);
	}
	
	public void updateConnections(int maxDepth) {
		if (!ignoreUpdates) {
			collectNeighbors();
			findConnections(maxDepth);
		}
	}
	
	public void findConnections() {
		findConnections(512);
	}
	
	public void findConnections(int maxDepth) {
		List<BlockPos> oldConnectionsOut = new ArrayList<>(connectionsOut);
		List<BlockPos> oldConnectionsIn = new ArrayList<>(connectionsOut);
		connectionsOut.clear();
		connectionsIn.clear();
		
		Neighbor belowNeighbor = neighbors[Directions.DOWN];
		Neighbor aboveNeighbor = neighbors[Directions.UP];
		boolean belowIsSolid = (belowNeighbor.type == NeighborType.SOLID_BLOCK);
		boolean aboveIsSolid = (aboveNeighbor.type == NeighborType.SOLID_BLOCK);
		
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Neighbor neighbor = neighbors[index];
			BlockPos side = neighbor.pos;
			
			if (neighbor.type == NeighborType.WIRE) {
				addConnection(side, true, true);
				
				continue;
			}
			
			boolean sideIsSolid = (neighbor.type == NeighborType.SOLID_BLOCK);
			
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				BlockState aboveSideState = world.getBlockState(aboveSide);
				
				if (aboveSideState.isOf(wireBlock)) {
					addConnection(aboveSide, true, sideIsSolid);
				}
			}
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				BlockState belowSideState = world.getBlockState(belowSide);
				
				if (belowSideState.isOf(wireBlock)) {
					addConnection(belowSide, belowIsSolid, true);
				}
			}
		}
		
		if (maxDepth-- > 0) {
			connectionsChanged(oldConnectionsOut, oldConnectionsIn, maxDepth);
		}
	}
	
	public void collectNeighbors() {
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = pos.offset(dir);
			BlockState state = world.getBlockState(side);
			
			neighbors[index] = Neighbor.of(world, side, state, wireBlock);
		}
	}
	
	private void addConnection(BlockPos pos, boolean out, boolean in) {
		if (out) {
			connectionsOut.add(pos);
		}
		if (in) {
			connectionsIn.add(pos);
		}
	}
	
	private void connectionsChanged(List<BlockPos> oldConnectionsOut, List<BlockPos> oldConnectionsIn, int maxDepth) {
		Set<BlockPos> affectedWires = new HashSet<>();
		
		affectedWires.addAll(CollectionsUtils.difference(oldConnectionsOut, connectionsOut));
		affectedWires.addAll(CollectionsUtils.difference(oldConnectionsIn, connectionsIn));
		
		ignoreUpdates = true;
		
		for (BlockPos pos : affectedWires) {
			Wire wire = ((IWireBlock)wireBlock).getWire(world, pos, true, false);
			
			if (wire != null) {
				wire.updateConnections(maxDepth);
			}
		}
		
		ignoreUpdates = false;
	}
}
