package fast.redstone.interfaces.mixin;

import fast.redstone.v2.WireV2;

public interface IChunkSection {
	
	public WireV2 getWire(int x, int y, int z);
	
	public WireV2 setWire(int x, int y, int z, WireV2 wire);
	
}
