package alternate.current.interfaces.mixin;

import alternate.current.Wire;
import alternate.current.boop.WireBlock;
import alternate.current.boop.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IChunk {
	
	public void clearWires();
	
	public Wire getWire(BlockPos pos);
	
	public Wire setWire(BlockPos pos, Wire wire);
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate);
	
	public WireNode setWire(WireNode wire);
	
}
