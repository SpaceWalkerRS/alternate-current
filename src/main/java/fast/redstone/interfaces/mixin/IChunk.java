package fast.redstone.interfaces.mixin;

import fast.redstone.RedstoneWireHandler;
import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public RedstoneWireHandler getWireHandler(BlockPos pos);
	
	public void addWireHandler(RedstoneWireHandler wireHandler, int chunkY);
	
	public void removeWireHandler(RedstoneWireHandler wireHandler, int chunkY);
	
}
