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

/**
 * A WireNode is a Node that represents a redstone wire in the world.
 * It stores all the information about the redstone wire that the
 * WireHandler needs to calculate power changes.
 * 
 * While regular Nodes are only used by and thus stored in the
 * WireHandler, WireNodes are stored in the WorldChunks. This is
 * done so that wire connections do not have to be re-calculated
 * each time a wire network is updated, which makes building the
 * network a lot faster.
 * 
 * @author Space Walker
 */
public class WireNode extends Node implements Comparable<WireNode> {
	
	private static final int DEFAULT_MAX_UPDATE_DEPTH = 512;
	
	/** List of positions of redstone wires that this wire can provide power to */
	public final List<BlockPos> connectionsOut;
	/** List of positions of redstone wires that can provide power to this wire */
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
		this.isWire = true;
		
		this.virtualPower = this.prevPower = this.wireBlock.getPower(this.world, this.pos, this.state);
	}
	
	@Override
	public int compareTo(WireNode wire) {
		int c = Integer.compare(wire.virtualPower, virtualPower);
		return (c == 0) ? Integer.compare(ticket, wire.ticket) : c;
	}
	
	@Override
	public Node update(BlockPos pos, BlockState state) {
		AlternateCurrentMod.LOGGER.warn("Cannot update a WireNode!");
		return this;
	}
	
	@Override
	public WireNode asWire() {
		return this;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return this.wireBlock == wireBlock;
	}
	
	/**
	 * Update the connections this redstone wire has to other wires.
	 */
	public void updateConnections() {
		updateConnections(DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	/**
	 * Update the connections this redstone wire has to other wires.
	 * 
	 * @param maxDepth The maximum depth to which these updates should propagate.
	 */
	public void updateConnections(int maxDepth) {
		if (ignoreUpdates) {
			return;
		}
		
		ignoreUpdates = true;
		
		List<BlockPos> prevConnectionsOut = new ArrayList<>(connectionsOut);
		List<BlockPos> prevConnectionsIn = new ArrayList<>(connectionsIn);
		connectionsOut.clear();
		connectionsIn.clear();
		
		wireBlock.findWireConnections(this);
		
		if (maxDepth-- > 0) {
			onConnectionsChanged(prevConnectionsOut, prevConnectionsIn, maxDepth);
		}
		
		ignoreUpdates = false;
	}
	
	/**
	 * Add a connection to another redstone wire.
	 * 
	 * @param pos the position of the connected wire
	 * @param out true if this wire can provide power to the connected wire
	 * @param in  true if the connected wire can provide power to this wire
	 */
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
	
	/**
	 * Tell connected wires that they should update their connections.
	 */
	public void updateConnectedWires() {
		Collection<BlockPos> wires = new HashSet<>();
		
		wires.addAll(connectionsOut);
		wires.addAll(connectionsIn);
		
		updateNeighboringWires(wires, DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	/**
	 * Tell some collection of wires that they should update their connections
	 * 
	 * @param wires    a collection of positions where there are redstone wires
	 * @param maxDepth the maximum depth to which these updates should propagate
	 */
	public void updateNeighboringWires(Collection<BlockPos> wires, int maxDepth) {
		for (BlockPos pos : wires) {
			WireNode wire = wireBlock.getOrCreateWire(world, pos, false);
			
			if (wire != null) {
				wire.updateConnections(maxDepth);
			}
		}
	}
}
