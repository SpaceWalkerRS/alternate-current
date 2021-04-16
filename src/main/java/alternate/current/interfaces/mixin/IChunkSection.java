package alternate.current.interfaces.mixin;

import alternate.current.Wire;

public interface IChunkSection {
	
	public void clearWires();
	
	public Wire getWireV2(int x, int y, int z);
	
	public Wire setWireV2(int x, int y, int z, Wire wire);
	
}
