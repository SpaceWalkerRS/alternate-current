package fast.redstone.interfaces.mixin;

import fast.redstone.v2.WireV2;

import net.minecraft.util.math.BlockPos;

public interface IWorld {
	
	public void reset();
	
	public int getCount();
	
	public WireV2 getWire(BlockPos pos);
	
	public void setWire(BlockPos pos, WireV2 wire, boolean updateConnections);
	
}
