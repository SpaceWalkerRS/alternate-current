package alternate.current.wire;

import alternate.current.wire.redstone.RedstoneWireType;

public class WireTypes {

	public static final WireType REDSTONE;

	static {

		REDSTONE = new RedstoneWireType("Redstone Wire", 1, true, true);

	}
}
