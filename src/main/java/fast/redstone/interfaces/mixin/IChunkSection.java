package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;

public interface IChunkSection {
	
	public void clearWires();
	
	public Wire getWireV2(int x, int y, int z);
	
	public Wire setWireV2(int x, int y, int z, Wire wire);
	
}
