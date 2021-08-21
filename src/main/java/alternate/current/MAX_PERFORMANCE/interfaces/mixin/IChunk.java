package alternate.current.MAX_PERFORMANCE.interfaces.mixin;

import alternate.current.MAX_PERFORMANCE.WireBlock;
import alternate.current.MAX_PERFORMANCE.WireNode;
import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public void clearWires();
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	/**
	 * Place the given {@code WireNode} in the chunk.
	 */
	public void placeWire(WireNode wire);
	
	/**
	 * Remove the given {@code WireNode} from the chunk.
	 */
	public void removeWire(WireNode wire);
	
}
