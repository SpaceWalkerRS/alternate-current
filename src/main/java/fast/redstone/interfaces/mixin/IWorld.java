package fast.redstone.interfaces.mixin;

import fast.redstone.RedstoneWireHandler;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public interface IWorld {
	
	public RedstoneWireHandler getWireHandler(BlockPos pos);
	
	public RedstoneWireHandler getOrCreateWireHandler(BlockPos pos, BlockState state);
	
	public void addWireHandler(RedstoneWireHandler wireHandler);
	
	public void removeWireHandler(RedstoneWireHandler wireHandler);
	
	public void removeWireHandler(BlockPos pos);
	
	public void reset();
	
	public int getCount();
	
}
