package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;
import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public void clearWires();
	
	public Wire getWireV2(BlockPos pos);
	
	public Wire setWireV2(BlockPos pos, Wire wire);
	
}
