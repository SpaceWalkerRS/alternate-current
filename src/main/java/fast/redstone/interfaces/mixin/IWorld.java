package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;

import net.minecraft.util.math.BlockPos;

public interface IWorld {
	
	public Wire getWire(BlockPos pos);
	
	public void setWire(BlockPos pos, Wire wire, boolean updateConnections);
	
	public void reset();
	
	public int getCount();
	
}
