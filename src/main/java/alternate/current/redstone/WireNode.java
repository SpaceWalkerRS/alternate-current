package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alternate.current.AlternateCurrentMod;
import alternate.current.utils.CollectionsUtils;
import alternate.current.utils.Directions;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WireNode extends Node implements Comparable<WireNode> {
	
	private static final int DEFAULT_MAX_DEPTH = 512;
	
	public final Node[] neighbors;
	public final List<BlockPos> connectionsOut;
	public final List<BlockPos> connectionsIn;
	
	public int power;
	public boolean removed;
	
	/* fields used while updating power */
	public int prevPower;
	public int externalPower;
	public boolean inNetwork;
	public boolean isPowerSource;
	
	private boolean ignoreUpdates;
	
	public WireNode(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		super(world, wireBlock);
		
		this.neighbors = new Node[Directions.ALL.length];
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
		
		this.pos = pos.toImmutable();
		this.state = state;
		this.power = wireBlock.getPower(this.world, this.pos, this.state);
	}
	
	@Override
	public int compareTo(WireNode wire) {
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
	
	@Override
	public Node update(BlockPos pos, BlockState state) {
		AlternateCurrentMod.LOGGER.warn("Cannot update a WireNode!");
		return this;
	}
	
	@Override
	public boolean isWire() {
		return true;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return this.wireBlock == wireBlock;
	}
	
	public void updateConnections() {
		updateConnections(DEFAULT_MAX_DEPTH);
	}
	
	public void updateConnections(int maxDepth) {
		if (!ignoreUpdates) {
			ignoreUpdates = true;
			findConnections(maxDepth);
			ignoreUpdates = false;
		}
	}
	
	public void clearNeighbors() {
		Arrays.fill(neighbors, null);
	}
	
	private void findConnections(int maxDepth) {
		List<BlockPos> prevConnectionsOut = new ArrayList<>(connectionsOut);
		List<BlockPos> prevConnectionsIn = new ArrayList<>(connectionsIn);
		connectionsOut.clear();
		connectionsIn.clear();
		
		BlockPos up = pos.up();
		BlockPos down = pos.down();
		BlockState aboveNeighbor = world.getBlockState(up);
		BlockState belowNeighbor = world.getBlockState(down);
		boolean aboveIsSolid = aboveNeighbor.isSolidBlock(world, up);
		boolean belowIsSolid = belowNeighbor.isSolidBlock(world, down);
		
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (wireBlock.isOf(neighbor)) {
				addConnection(side, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock(world, side);
			
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				BlockState aboveSideState = world.getBlockState(aboveSide);
				
				if (wireBlock.isOf(aboveSideState)) {
					addConnection(aboveSide, true, sideIsSolid);
				}
			}
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				BlockState belowSideState = world.getBlockState(belowSide);
				
				if (wireBlock.isOf(belowSideState)) {
					addConnection(belowSide, belowIsSolid, true);
				}
			}
		}
		
		if (maxDepth-- > 0) {
			onConnectionsChanged(prevConnectionsOut, prevConnectionsIn, maxDepth);
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
	
	private void onConnectionsChanged(Collection<BlockPos> prevConnectionsOut, Collection<BlockPos> prevConnectionsIn, int maxDepth) {
		Set<BlockPos> affectedWires = new HashSet<>();
		
		affectedWires.addAll(CollectionsUtils.difference(prevConnectionsOut, connectionsOut));
		affectedWires.addAll(CollectionsUtils.difference(prevConnectionsIn, connectionsIn));
		
		updateNeighboringWires(affectedWires, maxDepth);
	}
	
	public void updateConnectedWires() {
		Collection<BlockPos> wires = new HashSet<>();
		
		wires.addAll(connectionsOut);
		wires.addAll(connectionsIn);
		
		updateNeighboringWires(wires, DEFAULT_MAX_DEPTH);
	}
	
	public void updateNeighboringWires(Collection<BlockPos> wires, int maxDepth) {
		for (BlockPos pos : wires) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, false);
			
			if (wire != null) {
				wire.updateConnections(maxDepth);
			}
		}
	}
}
