package fast.redstone.interfaces.mixin;

import fast.redstone.v1.WireV1;
import fast.redstone.v2.WireV2;

public interface IChunkSection {
	
	public WireV1 getWireV1(int x, int y, int z);
	
	public WireV1 setWireV1(int x, int y, int z, WireV1 wire);
	
	public WireV2 getWireV2(int x, int y, int z);
	
	public WireV2 setWireV2(int x, int y, int z, WireV2 wire);
	
}
