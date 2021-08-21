package alternate.current.MAX_PERFORMANCE.interfaces.mixin;

import alternate.current.MAX_PERFORMANCE.WireBlock;
import alternate.current.MAX_PERFORMANCE.WireNode;
import net.minecraft.util.math.BlockPos;

public interface IChunkSection {
	
	public void clearWires();
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk section.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	/**
	 * Set the given {@code WireNode} in the chunk section.
	 */
	public WireNode setWire(BlockPos pos, WireNode wire);
	
}
