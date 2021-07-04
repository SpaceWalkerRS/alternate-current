package alternate.current.redstone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import alternate.current.AlternateCurrentMod;
import alternate.current.utils.CollectionsUtils;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WireNode extends Node implements Comparable<WireNode> {
	
	private static final int DEFAULT_MAX_UPDATE_DEPTH = 512;
	
	public final List<BlockPos> connectionsOut;
	public final List<BlockPos> connectionsIn;
	
	public int power;
	public boolean removed;
	
	/* fields used while updating power */
	public int prevPower;
	public int externalPower;
	public boolean shouldBreak;
	public boolean prepared;
	public boolean inNetwork;
	
	private boolean ignoreUpdates;
	
	public WireNode(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		super(world, wireBlock);
		
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
		
		this.pos = pos.toImmutable();
		this.state = state;
		this.power = this.wireBlock.getPower(this.world, this.pos, this.state);
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
		updateConnections(DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	public void updateConnections(int maxDepth) {
		if (!ignoreUpdates) {
			ignoreUpdates = true;
			findConnections(maxDepth);
			ignoreUpdates = false;
		}
	}
	
	private void findConnections(int maxDepth) {
		List<BlockPos> prevConnectionsOut = new ArrayList<>(connectionsOut);
		List<BlockPos> prevConnectionsIn = new ArrayList<>(connectionsIn);
		connectionsOut.clear();
		connectionsIn.clear();
		
		wireBlock.findWireConnections(this);
		
		if (maxDepth-- > 0) {
			onConnectionsChanged(prevConnectionsOut, prevConnectionsIn, maxDepth);
		}
	}
	
	public void addConnection(BlockPos pos, boolean out, boolean in) {
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
		
		updateNeighboringWires(wires, DEFAULT_MAX_UPDATE_DEPTH);
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
