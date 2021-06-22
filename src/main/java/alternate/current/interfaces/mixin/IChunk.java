package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public void clearWires();
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk.
	 * If there is none and {@code orCreate} is {@code true},
	 * try to add one.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate);
	
	/**
	 * Place the given {@code WireNode} in the chunk.
	 */
	public void placeWire(WireNode wire);
	
	/**
	 * Remove the given {@code WireNode} from the chunk.
	 */
	public void removeWire(WireNode wire);
	
}
