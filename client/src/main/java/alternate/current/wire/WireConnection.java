package alternate.current.wire;

/**
 * This class represents a connection between some WireNode (the 'owner') and a
 * neighboring WireNode. Two wires are considered to be connected if power can
 * flow from one wire to the other (and/or vice versa).
 * 
 * @author Space Walker
 */
public class WireConnection {

	/** The connected wire. */
	final WireNode wire;
	/** Cardinal direction to the connected wire. */
	final int iDir;
	/** True if the owner of the connection can provide power to the connected wire. */
	final boolean offer;
	/** True if the connected wire can provide power to the owner of the connection. */
	final boolean accept;

	/** The next connection in the sequence. */
	WireConnection next;

	WireConnection(WireNode wire, int iDir, boolean offer, boolean accept) {
		this.wire = wire;
		this.iDir = iDir;
		this.offer = offer;
		this.accept = accept;
	}
}
