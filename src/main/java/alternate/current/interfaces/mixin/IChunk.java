package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk.
	 */
	public WireNode getWireNode(BlockPos pos);
	
}
