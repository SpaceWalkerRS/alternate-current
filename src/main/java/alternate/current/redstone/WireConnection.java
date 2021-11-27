package alternate.current.redstone;

/**
 * This class represents a connection between some WireNode (the
 * 'owner') and a neighboring WireNode. Two wires are considered
 * to be connected if power can flow from one wire to the other
 * (and/or vice versa).
 * 
 * @author Space Walker
 */
public class WireConnection {
	
	/** Position of the connected wire. */
	public final WireNode wire;
	/** Cardinal direction to the connected wire. */
	public final int iDir;
	/** True if the connected wire can provide power to the owner of the connection. */
	public final boolean in;
	/** True if the connected wire can accept power from the owner of the connection. */
	public final boolean out;
	
	public WireConnection(WireNode wire, int iDir, boolean in, boolean out) {
		this.wire = wire;
		this.iDir = iDir;
		this.in = in;
		this.out = out;
	}
}
