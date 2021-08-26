package alternate.current.redstone.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;
import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
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