package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WorldAccess;

public interface IServerWorld {
	
	public WorldAccess getAccess(WireBlock wireBlock);
	
}
