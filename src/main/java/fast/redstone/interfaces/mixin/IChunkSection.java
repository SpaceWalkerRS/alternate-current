package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;

public interface IChunkSection {
	
	public Wire getWire(int x, int y, int z);
	
	public Wire setWire(int x, int y, int z, Wire wire);
	
}
