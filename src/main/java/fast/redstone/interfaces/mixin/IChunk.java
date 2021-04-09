package fast.redstone.interfaces.mixin;

import fast.redstone.v1.WireV1;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public WireV1 getWireV1(BlockPos pos);
	
	public WireV1 setWireV1(BlockPos pos, WireV1 wire);
}
