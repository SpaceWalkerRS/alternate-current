package fast.redstone.interfaces.mixin;

import fast.redstone.v2.WireV2;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWireBlock {
	
	public void setWiresGivePower(boolean wiresGivePower);
	
	public WireV2 getWire(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections);
	
}
