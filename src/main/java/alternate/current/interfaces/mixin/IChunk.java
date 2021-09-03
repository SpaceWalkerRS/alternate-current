package alternate.current.interfaces.mixin;

import java.util.Collection;
import java.util.Collections;

import alternate.current.redstone.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	/**
	 * Retrieve the {@code WireNode} at this position in the chunk.
	 */
	public WireNode getWire(BlockPos pos);
	
	default Collection<WireNode> getWires() {
		return Collections.emptySet();
	}
	
	/**
	 * Place the given {@code WireNode} in the chunk.
	 */
	public void placeWire(WireNode wire);
	
	/**
	 * Remove the given {@code WireNode} from the chunk.
	 */
	public void removeWire(WireNode wire);
	
}
