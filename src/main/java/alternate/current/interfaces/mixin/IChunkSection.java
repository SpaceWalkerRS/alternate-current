package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireNode;

public interface IChunkSection {
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk section.
	 */
	public WireNode getWire(int x, int y, int z);
	
	/**
	 * Set the given {@code WireNode} in the chunk section.
	 */
	public WireNode setWire(int x, int y, int z, WireNode wire);
	
}
