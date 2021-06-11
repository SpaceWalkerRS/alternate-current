package alternate.current.boop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alternate.current.interfaces.mixin.IWorld;
import alternate.current.utils.CollectionsUtils;
import alternate.current.utils.Directions;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class WireNode extends Node implements Comparable<WireNode> {
	
	public final WireBlock wireBlock;
	public final Node[] neighbors;
	public final List<BlockPos> connectionsOut;
	public final List<BlockPos> connectionsIn;
	
	public int power;
	
	public boolean inNetwork;
	public boolean isPowerSource;
	
	private boolean ignoreUpdates;
	
	public WireNode(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		super(world, pos, NodeType.WIRE, state);
		
		this.wireBlock = wireBlock;
		this.neighbors = new Node[Directions.ALL.length];
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
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
	public boolean isWire() {
		return true;
	}
	
	public void updateConnections() {
		updateConnections(512);
	}
	
	public void updateConnections(int maxDepth) {
		if (!ignoreUpdates) {
			collectNeighbors();
			findConnections(maxDepth);
			clearNeighbors();
		}
	}
	
	private void collectNeighbors() {
		for (int index = 0; index < Directions.ALL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos neighborPos = pos.offset(dir);
			BlockState neighborState = world.getBlockState(neighborPos);
			
			neighbors[index] = Node.of(wireBlock, world, neighborPos, neighborState);
		}
	}
	
	private void clearNeighbors() {
		Arrays.fill(neighbors, null);
	}
	
	private void findConnections(int maxDepth) {
		List<BlockPos> prevConnectionsOut = new ArrayList<>(connectionsOut);
		List<BlockPos> prevConnectionsIn = new ArrayList<>(connectionsOut);
		connectionsOut.clear();
		connectionsIn.clear();
		
		Node belowNeighbor = neighbors[Directions.DOWN];
		Node aboveNeighbor = neighbors[Directions.UP];
		boolean belowIsSolid = belowNeighbor.isSolidBlock();
		boolean aboveIsSolid = aboveNeighbor.isSolidBlock();
		
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Node neighbor = neighbors[index];
			BlockPos side = neighbor.pos;
			
			if (neighbor.isWire()) {
				addConnection(side, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock();
			
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
	
	public void updateNeighboringWires() {
		Collection<BlockPos> wires = new HashSet<>();
		
		wires.addAll(connectionsOut);
		wires.addAll(connectionsIn);
		
		updateNeighboringWires(wires, 512);
	}
	
	public void updateNeighboringWires(Collection<BlockPos> wires, int maxDepth) {
		ignoreUpdates = true;
		
		for (BlockPos pos : wires) {
			WireNode wire = ((IWorld)world).getWire(wireBlock, pos);
			
			if (wire != null) {
				wire.updateConnections(maxDepth);
			}
		}
		
		ignoreUpdates = false;
	}
}
