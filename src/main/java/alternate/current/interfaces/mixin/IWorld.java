package alternate.current.interfaces.mixin;

import alternate.current.Wire;
import alternate.current.boop.WireBlock;
import alternate.current.boop.WireNode;

import net.minecraft.util.math.BlockPos;

public interface IWorld {
	
	public void reset();
	
	public int getCount();
	
	public Wire getWire(BlockPos pos);
	
	public void setWire(BlockPos pos, Wire wire, boolean updateConnections);
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate);
	
	public void setWire(WireNode wire);
	
}
