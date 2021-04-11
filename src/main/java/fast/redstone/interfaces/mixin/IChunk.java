package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;
import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public Wire getWire(BlockPos pos);
	
	public Wire setWire(BlockPos pos, Wire wire);
	
}
