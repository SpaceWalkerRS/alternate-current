package fast.redstone.interfaces.mixin;

import fast.redstone.RedstoneWireHandler;
import net.minecraft.util.math.BlockPos;

public interface IChunkSection {
	
	public RedstoneWireHandler getWireHandler(BlockPos pos);
	
	public void addRedstoneWireHandler(RedstoneWireHandler wireHandler);
	
	public void removeRedstoneWireHandler(RedstoneWireHandler wireHandler);
	
}
