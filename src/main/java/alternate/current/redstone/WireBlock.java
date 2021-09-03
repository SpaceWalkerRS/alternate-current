package alternate.current.redstone;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;

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
	
	default int getPower(WorldAccess world, BlockPos pos, BlockState state) {
		return state.get(Properties.POWER);
	}
	
	default void findWireConnections(WireNode wire) {
		WorldAccess world = wire.world;
		BlockPos pos = wire.pos;
		
		boolean aboveIsSolid = world.isSolidBlock(pos.up());
		boolean belowIsSolid = world.isSolidBlock(pos.down());
		
		for (int iDir = 0; iDir < WireHandler.Directions.HORIZONTAL.length; iDir++) {
			Direction dir = WireHandler.Directions.HORIZONTAL[iDir];
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (isOf(neighbor)) {
				wire.connections.add(side, iDir, true, true);
				continue;
			}
			
			boolean sideIsSolid = world.isSolidBlock(side, neighbor);
			
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
	
	default void onWireAdded(WorldAccess world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		tryUpdatePower(wire);
		
		if (wire.virtualPower > getMinPower()) {
			world.updateShapesAround(wire.pos, wire.state);
		}
		
		updateNeighborsOfConnectedWires(wire);
	}
	
	default void onWireRemoved(WorldAccess world, BlockPos pos, BlockState state, WireNode wire, boolean moved) {
		if (!moved) {
			// If the 'shouldBreak' field is set to 'true', the
			// removal of this wire is part of already ongoing
			// power changes.
			if (!wire.shouldBreak) {
				tryUpdatePower(wire);
			}
			
			updateNeighborsOf(world, pos);
			updateNeighborsOfConnectedWires(wire);
		}
	}
	
	default void tryUpdatePower(WireNode wire) {
		if (shouldUpdatePower(wire)) {
			wire.world.getWireHandler().updatePower(wire);
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
		
		for (Direction dir : WireHandler.Directions.ALL) {
			BlockPos side = wire.pos.offset(dir);
			BlockState neighbor = wire.world.getBlockState(side);
			
			if (isOf(neighbor)) {
				continue;
			}
			
			if (wire.world.isSolidBlock(side, neighbor)) {
				power = Math.max(power, getStrongPowerTo(wire.world, side, dir.getOpposite()));
			}
			if (neighbor.emitsRedstonePower()) {
				power = Math.max(power, wire.world.getWeakPowerFrom(side, neighbor, dir));
			}
			
			if (power >= max) {
				return max;
			}
		}
		
		return power;
	}
	
	default int getStrongPowerTo(WorldAccess world, BlockPos pos, Direction ignore) {
		int power = getMinPower();
		int max = getMaxPower();
		
		for (Direction dir : WireHandler.Directions.ALL) {
			if (dir == ignore) {
				continue;
			}
			
			BlockPos side = pos.offset(dir);
			BlockState neighbor = world.getBlockState(side);
			
			if (neighbor.emitsRedstonePower() && !isOf(neighbor)) {
				power = Math.max(power, world.getStrongPowerFrom(side, neighbor, dir));
				
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
				WireNode connectedWire = wire.world.getWire(pos, true, true);
				
				if (connectedWire != null) {
					power = Math.max(power, connectedWire.currentPower - step);
				}
			}
		}
		
		return power;
	}
	
	default boolean setPower(WireNode wire) {
		BlockState newState = wire.state.with(Properties.POWER, clampPower(wire.virtualPower));
		
		if (newState != wire.state && wire.world.setBlockState(wire.pos, newState)) {
			wire.stateChanged(newState);
			return true;
		}
		
		return false;
	}
	
	default boolean updateWireState(WireNode wire) {
		if (wire.removed) {
			return true;
		}
		if (wire.shouldBreak) {
			return wire.world.breakBlock(wire.pos, wire.state);
		}
		
		return setPower(wire);
	}
	
	default void tryUpdateNeighborsOfWire(WorldAccess world, BlockPos pos) {
		tryUpdateNeighborsOfWire(world, pos, world.getBlockState(pos));
	}
	
	default void tryUpdateNeighborsOfWire(WorldAccess world, BlockPos pos, BlockState state) {
		if (isOf(state)) {
			updateNeighborsOf(world, pos);
		}
	}
	
	default void updateNeighborsOf(WorldAccess world, BlockPos pos) {
		Block block = asBlock();
		List<BlockPos> positions = new ArrayList<>();
		WireHandler.collectNeighborPositions(positions, pos);
		
		for (BlockPos neighborPos : positions) {
			world.updateNeighborBlock(neighborPos, pos, block);
		}
	}
	
	default void updateNeighborsOfConnectedWires(WireNode wire) {
		for (BlockPos pos : wire.connections.getAll()) {
			tryUpdateNeighborsOfWire(wire.world, pos);
		}
	}
}
