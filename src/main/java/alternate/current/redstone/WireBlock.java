package alternate.current.redstone;

import alternate.current.interfaces.mixin.IWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WireBlock {
	
	public default Block asBlock() {
		return (Block)this;
	}
	
	public default boolean isOf(BlockState state) {
		return state.getBlock() == asBlock();
	}
	
	default int getMinPower() {
		return 0;
	}
	
	default int getMaxPower() {
		return 15;
	}
	
	public void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
	public default WireNode getWire(World world, BlockPos pos) {
		return ((IWorld)world).getWire(this, pos);
	}
	
	public default WireNode createWire(World world, BlockPos pos, BlockState state) {
		return new WireNode(this, world, pos, state);
	}
	
	default int getPower(World world, BlockPos pos, BlockState state) {
		if (isOf(state)) {
			return state.get(Properties.POWER);
		}
		
		throw new IllegalStateException("BlockState " + state + " is not of Block " + this);
	}
	
	default boolean setPower(WireNode wire, int power, int flags) {
		if (wire.isOf(this)) {
			BlockState newState = wire.state.with(Properties.POWER, power);
			
			if (newState != wire.state) {
				wire.state = newState;
				
				World world = wire.world;
				BlockPos pos = wire.pos;
				
				return world.setBlockState(pos, newState, flags);
			}
		}
		
		return false;
	}
}
