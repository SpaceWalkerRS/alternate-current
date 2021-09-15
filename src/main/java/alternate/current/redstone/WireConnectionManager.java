package alternate.current.redstone;

import java.util.Arrays;

public class WireConnectionManager {
	
	public static final int DEFAULT_MAX_UPDATE_DEPTH = 512;
	
	/** The owner of these connections */
	public final WireNode wire;
	
	/** All connections to other wires */
	public WireConnection[] all;
	/** Connections to other wires, sorted per cardinal direction */
	public final WireConnection[][] byDir;
	
	private int flowTotal;
	public int flow;
	
	public WireConnectionManager(WireNode wire) {
		this.wire = wire;
		this.all = new WireConnection[0];
		this.byDir = new WireConnection[WireHandler.Directions.HORIZONTAL.length][];
		
		this.clear();
	}
	
	public void clear() {
		this.all = new WireConnection[0];
		Arrays.fill(byDir, this.all);
	}
	
	public void add(WireNode wire, int iDir, boolean in, boolean out) {
		addConnection(new WireConnection(wire, iDir, in, out));
	}
	
	private void addConnection(WireConnection connection) {
		all = withConnection(all, connection);
		
		WireConnection[] connections = byDir[connection.iDir];
		byDir[connection.iDir] = withConnection(connections, connection);
		
		flowTotal |= (1 << connection.iDir);
		flow = WireHandler.FLOW_IN_TO_FLOW_OUT[flowTotal];
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
