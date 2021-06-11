package alternate.current.interfaces.mixin;

import alternate.current.Wire;
import alternate.current.boop.WireBlock;
import alternate.current.boop.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IChunkSection {
	
	public void clearWires();
	
	public Wire getWire(int x, int y, int z);
	
	public Wire setWire(int x, int y, int z, Wire wire);
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	public WireNode setWire(WireNode wire);
	
}
