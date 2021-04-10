package fast.redstone.interfaces.mixin;

import fast.redstone.v1.WireV1;
import fast.redstone.v2.WireV2;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface IWireBlock {
	
	public void setWiresGivePower(boolean wiresGivePower);
	
	public WireV1 getWireV1(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections);
	
	public WireV2 getWireV2(World world, BlockPos pos, BlockState state, boolean orCreate, boolean updateConnections);
	
}
