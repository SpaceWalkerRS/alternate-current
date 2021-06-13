package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireHandler;

public interface IServerWorld {
	
	public WireHandler getWireHandler();
	
	public void clearWires();
	
}
