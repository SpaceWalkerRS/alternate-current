package fast.redstone;

public class WireConnection {
	
	public final Wire wire;   // The connected wire
	public final boolean out; // true if power can flow out to the connected wire
	public final boolean in;  // true if power can flow in from the connected wire
	
	public WireConnection(Wire wire, boolean out, boolean in) {
		this.wire = wire;
		this.out = out;
		this.in = in;
	}
}
