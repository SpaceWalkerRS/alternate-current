package alternate.current.redstone;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import alternate.current.util.collection.CollectionsUtils;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockPos;

public class WireConnectionManager {
	
	public static final int DEFAULT_MAX_UPDATE_DEPTH = 512;
	
	/** The owner of these connections */
	public final WireNode wire;
	
	public WireConnection[] all;
	public final WireConnection[][] byDir;
	
	private boolean ignoreUpdates;
	
	private int flowTotal;
	public int flow;
	
	public WireConnectionManager(WireNode wire) {
		this.wire = wire;
		this.byDir = new WireConnection[WireHandler.Directions.HORIZONTAL.length][];
		
		this.clear();
	}
	
	public NbtList toNbt() {
		NbtList nbt = new NbtList();
		
		for (WireConnection connection : all) {
			nbt.add(connection.toNbt());
		}
		
		return nbt;
	}
	
	public void fromNbt(NbtList nbt) {
		if (all.length > 0) {
			clear();
		}
		
		for (int index = 0; index < nbt.size(); index++) {
			NbtCompound connectionNbt = nbt.getCompound(index);
			WireConnection connection = WireConnection.fromNbt(connectionNbt);
			
			addConnection(connection);
		}
	}
	
	public Collection<BlockPos> getPositions() {
		return collectPositions(all);
	}
	
	private void clear() {
		all = new WireConnection[0];
		Arrays.fill(byDir, new WireConnection[0]);
		
		flowTotal = 0;
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
	}
	
	public void add(BlockPos pos, int iDir, boolean in, boolean out) {
		addConnection(new WireConnection(pos, iDir, in, out));
	}
	
	private void addConnection(WireConnection connection) {
		all = withConnection(all, connection);
		
		WireConnection[] connections = byDir[connection.iDir];
		byDir[connection.iDir] = withConnection(connections, connection);
		
		flowTotal |= (1 << connection.iDir);
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
	}
	
	public void update() {
		update(DEFAULT_MAX_UPDATE_DEPTH);
	}
	
	public void update(int maxDepth) {
		if (!ignoreUpdates) {
			ignoreUpdates = true;
			
			WireConnection[] prev = all;
			
			clear();
			wire.wireBlock.findWireConnections(wire);
			
			if (maxDepth-- > 0) {
				Collection<WireConnection> c1 = collectConnections(prev);
				Collection<WireConnection> c2 = collectConnections(all);
				Collection<WireConnection> difference = CollectionsUtils.difference(c1, c2);
				
				Collection<BlockPos> positions = new HashSet<>();
				
				for (WireConnection connection : difference) {
					positions.add(connection.pos);
				}
				
				wire.updateNeighboringWires(positions, maxDepth);
			}
			
			ignoreUpdates = false;
		}
	}
	
	private static Collection<WireConnection> collectConnections(WireConnection[] connections) {
		Set<WireConnection> positions = new HashSet<>();
		
		for (WireConnection connection : connections) {
			positions.add(connection);
		}
		
		return positions;
	}
	
	private static Collection<BlockPos> collectPositions(WireConnection[] connections) {
		Set<BlockPos> positions = new HashSet<>();
		
		for (WireConnection connection : connections) {
			positions.add(connection.pos);
		}
		
		return positions;
	}
	
	private static WireConnection[] withConnection(WireConnection[] connections, WireConnection connection) {
		WireConnection[] newArray = new WireConnection[connections.length + 1];
		
		for (int index = 0; index < connections.length; index++) {
			newArray[index] = connections[index];
		}
		newArray[connections.length] = connection;
		
		return newArray;
	}
}
