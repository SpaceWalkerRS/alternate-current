package alternate.current.redstone.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;

public interface IServerWorld {
	
	public WireHandler getWireHandler(WireBlock wireBlock);
	
}