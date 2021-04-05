package fast.redstone.interfaces.mixin;

import fast.redstone.Wire;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWireBlock {
	
	public Wire getWire(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections);
	
	public void setWiresGivePower(boolean wiresGivePower);
	
}
