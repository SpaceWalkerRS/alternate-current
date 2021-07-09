package alternate.current.redstone;

import alternate.current.interfaces.mixin.IWorld;
import alternate.current.utils.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

public interface WireBlock {
	
	public default Block asBlock() {
		return (Block)this;
	}
	
	public default boolean isOf(BlockState state) {
		return asBlock() == state.getBlock();
	}
	
	public int getMinPower();
	
	public int getMaxPower();
	
	public int getPowerStep();
	
	default int clampPower(int power) {
		return MathHelper.clamp(power, getMinPower(), getMaxPower());
	}
	
	public void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
	public void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved);
	
	public default WireNode getWire(World world, BlockPos pos) {
		return ((IWorld)world).getWire(this, pos);
	}
	
	public default WireNode createWire(World world, BlockPos pos, BlockState state) {
		return new WireNode(this, world, pos, state);
	}
	
	public default WireNode getOrCreateWire(World world, BlockPos pos, boolean updateConnections) {
		WireNode wire = getWire(world, pos);
		
		if (wire == null) {
			BlockState state = world.getBlockState(pos);
			
			if (isOf(state)) {
				wire = createWire(world, pos, state);
				((IWorld)world).placeWire(wire);
				
				if (updateConnections) {
					wire.updateConnections();
				}
			}
		}
		
		return wire;
	}
	
	default void findWireConnections(WireNode wire) {
		World world = wire.world;
		BlockPos pos = wire.pos;
		
		BlockPos up = pos.up();
		BlockPos down = pos.down();
		BlockState aboveNeighbor = world.getBlockState(up);
		BlockState belowNeighbor = world.getBlockState(down);
		boolean aboveIsSolid = aboveNeighbor.isSolidBlock(world, up);
		boolean belowIsSolid = belowNeighbor.isSolidBlock(world, down);
		
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.ALL[index];
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (isOf(neighbor)) {
				wire.addConnection(side, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock(world, side);
			
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				BlockState aboveSideState = world.getBlockState(aboveSide);
				
				if (isOf(aboveSideState)) {
					wire.addConnection(aboveSide, true, sideIsSolid);
				}
			}
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				BlockState belowSideState = world.getBlockState(belowSide);
				
				if (isOf(belowSideState)) {
					wire.addConnection(belowSide, belowIsSolid, true);
				}
			}
		}
	}
	
	default int getPower(World world, BlockPos pos, BlockState state) {
		if (isOf(state)) {
			return state.get(Properties.POWER);
		}
		
		throw new IllegalArgumentException("BlockState " + state + " is not of Block " + this);
	}
	
	default boolean setPower(World world, BlockPos pos, BlockState state, int power, int flags) {
		if (isOf(state)) {
			BlockState newState = state.with(Properties.POWER, clampPower(power));
			
			if (newState != state) {
				return world.setBlockState(pos, newState, flags);
			}
		}
		
		return false;
	}
	
	default boolean breakBlock(World world, BlockPos pos, BlockState state, int flags) {
		Block.dropStacks(state, world, pos);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), flags);
	}
}
