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
	
	public int prevPower;
	public int virtualPower;
	public int externalPower;
	public int ticket;
	public boolean shouldBreak;
	public boolean removed;
	public boolean prepared;
	public boolean inNetwork;
	
	private boolean ignoreUpdates;
	
	public WireNode(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		super(world, wireBlock);
		
		this.connectionsOut = new ArrayList<>();
		this.connectionsIn = new ArrayList<>();
		
		this.pos = pos.toImmutable();
		this.state = state;
		this.virtualPower = this.prevPower = this.wireBlock.getPower(this.world, this.pos, this.state);
	}
	
	@Override
	public int compareTo(WireNode wire) {
		int c = Integer.compare(wire.virtualPower, virtualPower);
		return (c == 0) ? Integer.compare(ticket, wire.ticket) : c;
	}
	
	@Override
	public Node update(BlockPos pos, BlockState state) {
		AlternateCurrentMod.LOGGER.warn("Cannot update the Node attributes of a WireNode!");
		return this;
	}
	
	@Override
	public boolean isWire() {
		return true;
	}
	
	@Override
	public WireNode asWire() {
		return this;
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
