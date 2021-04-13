package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;
import net.minecraft.util.math.BlockPos;

public interface IWorld {
	
	public void reset();
	
	public int getCount();
	
	public Wire getWireV2(BlockPos pos);
	
	public void setWire(BlockPos pos, Wire wire, boolean updateConnections);
	
}
