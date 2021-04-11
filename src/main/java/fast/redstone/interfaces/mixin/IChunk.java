package fast.redstone.interfaces.mixin;

import fast.redstone.v2.WireV2;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public WireV2 getWire(BlockPos pos);
	
	public WireV2 setWire(BlockPos pos, WireV2 wire);
	
}
