package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WorldAccess;
import net.minecraft.util.math.BlockPos;

public interface IServerWorld {
	
	public WorldAccess getAccess(WireBlock wireBlock);
	
	public void updateWireConnectionsAround(BlockPos pos);
	
}
