package alternate.current.MAX_PERFORMANCE;

import java.util.ArrayList;
import java.util.List;

import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IServerWorld;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IWorld;
import alternate.current.util.Directions;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;

/**
 * This interface should be implemented by each wire block type.
 * While Vanilla only has one wire block type, they could add
 * more in the future, and any mods that add more wire block
 * types that wish to take advantage of Alternate Current's
 * performance improvements should have those wire blocks
 * implement this interface.
 * 
 * @author Space Walker
 */
public interface WireBlock {
	
	public default Block asBlock() {
		return (Block)this;
	}
	
	public default boolean isOf(BlockState state) {
		return asBlock() == state.getBlock();
	}
	
	default int getMinPower() {
		return 0;
	}
	
	default int getMaxPower() {
		return 15;
	}
	
	default int getPowerStep() {
		return 1;
	}
	
	default int clampPower(int power) {
		return MathHelper.clamp(power, getMinPower(), getMaxPower());
	}
	
	default int getPower(World world, BlockPos pos, BlockState state) {
		return state.get(Properties.POWER);
	}
	
	default boolean setPower(World world, BlockPos pos, BlockState state, int power, int flags) {
		BlockState newState = state.with(Properties.POWER, clampPower(power));
		
		if (newState == state) {
			return false;
		}
		
		return world.setBlockState(pos, newState, flags);
	}
	
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
					wire.connections.update();
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
		boolean aboveIsSolid = world.getBlockState(up).isSolidBlock(world, up);
		boolean belowIsSolid = world.getBlockState(down).isSolidBlock(world, down);
		
		for (int iDir = 0; iDir < Directions.HORIZONTAL.length; iDir++) {
			Direction dir = Directions.HORIZONTAL[iDir];
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (isOf(neighbor)) {
				wire.connections.add(side, iDir, true, true);
				continue;
			}
			
			boolean sideIsSolid = neighbor.isSolidBlock(world, side);
			
			if (!aboveIsSolid) {
				BlockPos aboveSide = side.up();
				BlockState aboveSideState = world.getBlockState(aboveSide);
				
				if (isOf(aboveSideState)) {
					wire.connections.add(aboveSide, iDir, sideIsSolid, true);
				}
			}
			if (!sideIsSolid) {
				BlockPos belowSide = side.down();
				BlockState belowSideState = world.getBlockState(belowSide);
				
				if (isOf(belowSideState)) {
					wire.connections.add(belowSide, iDir, true, belowIsSolid);
				}
			}
		}
	}
	
	default boolean shouldBreak(World world, BlockPos pos, BlockState state) {
		return !state.canPlaceAt(world, pos);
	}
	
	default boolean breakBlock(World world, BlockPos pos, BlockState state, int flags) {
		Block.dropStacks(state, world, pos);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), flags);
	}
	
	default void onWireAdded(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		tryUpdatePower(wire);
		
		if (wire.virtualPower > getMinPower()) {
			wire.state.updateNeighbors(world, pos, 2);
			wire.state.prepare(world, pos, 2);
		}
		
		updateNeighborsOfConnectedWires(wire);
	}
	
	default void onWireRemoved(World world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		if (!moved) {
			if (!wire.shouldBreak) {
				tryUpdatePower(wire);
			}
			
			updateNeighborsOf(world, pos);
			updateNeighborsOfConnectedWires(wire);
		}
	}
	
	default void tryUpdatePower(WireNode wire) {
		if (shouldUpdatePower(wire)) {
			if (wire.connections.count == 0) {
				setPower(wire.world, wire.pos, wire.state, wire.virtualPower, Block.NOTIFY_LISTENERS);
				
				List<BlockPos> neighbors = new ArrayList<>();
				WireHandler.collectNeighborPositions(neighbors, wire.pos);
				
				Block block = asBlock();
				
				for (int index = 0; index < neighbors.size(); index++) {
					wire.world.updateNeighbor(neighbors.get(index), block, wire.pos);
				}
			} else {
				((IServerWorld)wire.world).getWireHandler(this).updatePower(wire);
			}
		}
	}
	
	default boolean shouldUpdatePower(WireNode wire) {
		wire.prepared = true;
		
		if (wire.removed || wire.shouldBreak) {
			wire.virtualPower = wire.externalPower = getMinPower();
		} else {
			wire.virtualPower = wire.externalPower = getExternalPower(wire);
			
			if (wire.virtualPower < getMaxPower()) {
				int wirePower = getWirePower(wire);
				
				if (wirePower > wire.virtualPower) {
					wire.virtualPower = wirePower;
				}
			}
		}
		
		return wire.virtualPower != wire.currentPower;
	}
	
	default int getExternalPower(WireNode wire) {
		int power = getMinPower();
		int max = getMaxPower();
		
		for (Direction dir : Directions.ALL) {
			BlockPos side = wire.pos.offset(dir);
			BlockState neighbor = wire.world.getBlockState(side);
			
			if (isOf(neighbor)) {
				continue;
			}
			
			if (neighbor.isSolidBlock(wire.world, side)) {
				power = Math.max(power, getStrongPowerTo(wire.world, side, dir.getOpposite()));
			}
			if (neighbor.emitsRedstonePower()) {
				power = Math.max(power, neighbor.getWeakRedstonePower(wire.world, side, dir));
			}
			
			if (power >= max) {
				return max;
			}
		}
		
		return power;
	}
	
	default int getStrongPowerTo(World world, BlockPos pos, Direction ignore) {
		int power = getMinPower();
		int max = getMaxPower();
		
		for (Direction dir : Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (neighbor.emitsRedstonePower() && !isOf(neighbor)) {
				power = Math.max(power, neighbor.getStrongRedstonePower(world, side, dir));
				
				if (power >= max) {
					return max;
				}
			}
		}
		
		return power;
	}
	
	default int getWirePower(WireNode wire) {
		int power = getMinPower();
		int step = getPowerStep();
		
		for (int iDir = 0; iDir < 4; iDir++) {
			for (BlockPos pos : wire.connections.in[iDir]) {
				WireNode connectedWire = getOrCreateWire(wire.world, pos, true);
				
				if (connectedWire != null) {
					power = Math.max(power, connectedWire.currentPower - step);
				}
			}
		}
		
		return power;
	}
	
	default void tryUpdateNeighborsOfWire(World world, BlockPos pos) {
		tryUpdateNeighborsOfWire(world, pos, world.getBlockState(pos));
	}
	
	default void tryUpdateNeighborsOfWire(World world, BlockPos pos, BlockState state) {
		if (isOf(state)) {
			updateNeighborsOf(world, pos);
		}
	}
	
	default void updateNeighborsOf(World world, BlockPos pos) {
		Block block = asBlock();
		List<BlockPos> positions = new ArrayList<>();
		WireHandler.collectNeighborPositions(positions, pos);
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighbor(neighborPos, block, pos);
		}
	}
	
	default void updateNeighborsOfConnectedWires(WireNode wire) {
		for (BlockPos pos : wire.connections.getAll()) {
			tryUpdateNeighborsOfWire(wire.world, pos);
		}
	}
}
