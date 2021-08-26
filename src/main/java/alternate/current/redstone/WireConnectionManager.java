package alternate.current.redstone;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import alternate.current.util.collection.CollectionsUtils;

import net.minecraft.util.math.BlockPos;

public class WireConnectionManager {
	
	public static final int DEFAULT_MAX_UPDATE_DEPTH = 512;
	
	/** The owner of these connections */
	public final WireNode wire;
	/** Positions of wires that can provide power to this wire */
	public final BlockPos[][] in;
	/** Positions of wires that this wire can provide power to */
	public final BlockPos[][] out;
	
	private boolean ignoreUpdates;
	
	public int count;
	private int flowTotal;
	public int flow;
	
	public WireConnectionManager(WireNode wire) {
		this.wire = wire;
		this.in = new BlockPos[4][];
		this.out = new BlockPos[4][];
		
		clear();
	}
	
	public Collection<BlockPos> getAll() {
		Set<BlockPos> wires = new HashSet<>();
		
		wires.addAll(collectConnections(in));
		wires.addAll(collectConnections(out));
		
		return wires;
	}
	
	public Collection<BlockPos> getAllIn() {
		return collectConnections(in);
	}
	
	public Collection<BlockPos> getAllOut() {
		return collectConnections(out);
	}
	
	private void clear() {
		Arrays.fill(in, new BlockPos[0]);
		Arrays.fill(out, new BlockPos[0]);
		
		count = 0;
		flowTotal = 0;
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
	}
	
	public void add(BlockPos pos, int iDir, boolean in, boolean out) {
		if (in) {
			addConnection(this.in, pos, iDir);
		}
		if (out) {
			addConnection(this.out, pos, iDir);
		}
		
		count++;
		flowTotal |= (1 << iDir);
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
	}
	
	public void update() {
		update(DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	public void update(int maxDepth) {
		if (!ignoreUpdates) {
			ignoreUpdates = true;
			
			Collection<BlockPos> prevIn = getAllIn();
			Collection<BlockPos> prevOut = getAllOut();
			
			clear();
			wire.wireBlock.findWireConnections(wire);
			
			if (maxDepth-- > 0) {
				Set<BlockPos> affectedWires = new HashSet<>();
				
				affectedWires.addAll(CollectionsUtils.difference(prevIn, getAllIn()));
				affectedWires.addAll(CollectionsUtils.difference(prevOut, getAllOut()));
				
				wire.updateNeighboringWires(affectedWires, maxDepth);
			}
			
			ignoreUpdates = false;
		}
	}
	
	private static void addConnection(BlockPos[][] connections, BlockPos pos, int iDir) {
		BlockPos[] oldArray = connections[iDir];
		BlockPos[] newArray = new BlockPos[oldArray.length + 1];
		
		for (int index = 0; index < oldArray.length; index++) {
			newArray[index] = oldArray[index];
		}
		
		newArray[oldArray.length] = pos;
		connections[iDir] = newArray;
	}
	
	private static Collection<BlockPos> collectConnections(BlockPos[][] connections) {
		Set<BlockPos> wires = new HashSet<>();
		
		for (BlockPos[] array : connections) {
			for (BlockPos wire : array) {
				wires.add(wire);
			}
		}
		
		return wires;
	}
}
