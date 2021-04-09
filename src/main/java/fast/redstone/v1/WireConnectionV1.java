package fast.redstone.v1;

public class WireConnectionV1 {
	
	public final WireV1 wire;   // The connected wire
	public final boolean out; // true if power can flow out to the connected wire
	public final boolean in;  // true if power can flow in from the connected wire
	
	public WireConnectionV1(WireV1 wire, boolean out, boolean in) {
		this.wire = wire;
		this.out = out;
		this.in = in;
	}
}
