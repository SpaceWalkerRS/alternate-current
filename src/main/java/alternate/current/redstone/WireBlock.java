package alternate.current.redstone;

import alternate.current.interfaces.mixin.IWorld;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface WireBlock {
	
	public default Block asBlock() {
		return (Block)this;
	}
	
	public default boolean isOf(BlockState state) {
		return state.getBlock() == asBlock();
	}
	
	public IntProperty getPowerProperty();
	
	public int getMinPower();
	
	public int getMaxPower();
	
	public default WireNode createWire(World world, BlockPos pos, BlockState state) {
		return new WireNode(this, world, pos, state);
	}
	
	public default WireNode getWire(World world, BlockPos pos) {
		return ((IWorld)world).getWire(this, pos);
	}
	
	public default WireNode getWire(World world, BlockPos pos, boolean orCreate) {
		return ((IWorld)world).getWire(this, pos, orCreate);
	}
	
	public default boolean shouldUpdateConnections(World world, BlockPos pos, BlockState prevState, BlockState newState, WireNode wire) {
		IntProperty power = getPowerProperty();
		return prevState.get(power) != newState.get(power);
	}
	
	public void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
}
