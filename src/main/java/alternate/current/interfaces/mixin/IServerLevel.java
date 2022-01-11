package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.LevelAccess;

public interface IServerLevel {
	
	public LevelAccess getAccess(WireBlock wireBlock);
	
}
