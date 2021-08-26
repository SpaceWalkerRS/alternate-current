package alternate.current.redstone.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;
import net.minecraft.util.math.BlockPos;

public interface IChunkSection {
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk section.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	/**
	 * Set the given {@code WireNode} in the chunk section.
	 */
	public WireNode setWire(BlockPos pos, WireNode wire);
	
}
