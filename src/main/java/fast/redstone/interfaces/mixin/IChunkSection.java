package fast.redstone.interfaces.mixin;

import fast.redstone.v1.WireV1;

public interface IChunkSection {
	
	public WireV1 getWireV1(int x, int y, int z);
	
	public WireV1 setWireV1(int x, int y, int z, WireV1 wire);
}
