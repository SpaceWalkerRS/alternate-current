package alternate.current.redstone.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireHandler;
import alternate.current.redstone.WireNode;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface IWorld {
	
	/**
	 * Retrieve the {@code WireNode} at this position in the world.
	 * If there is none, try to add one.
	 */
	default WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		return null;
	}
	
	/**
	 * Place the given {@code WireNode} in the world.
	 */
	default void placeWire(WireNode wire) {
		
	}
	
	/**
	 * Remove the given {@code WireNode} from the world.
	 */
	default void removeWire(WireNode wire) {
		
	}
	
	/**
	 * Remove all {@code WireNode}s in the world.
	 */
	default void clearWires() {
		
	}
	
	/**
	 * Update the wire connections of any {@code WireNode}
	 * at the given position in the world.
	 */
	default void updateWireConnections(BlockPos pos) {
		
	}
	
	/**
	 * Update the wire connections of the {@code WireNode}
	 * at the given position in the world if that
	 * {@code WireNode} is of the given {@code WireBlock}.
	 */
	default void updateWireConnections(WireBlock wireBlock, BlockPos pos) {
		
	}
	
	/**
	 * Update the wire connections of any {@code WireNode}s
	 * at the horizontal neighbors of the given position
	 * in the world
	 */
	default void updateWireConnectionsAround(BlockPos pos) {
		for (Direction dir : WireHandler.Directions.HORIZONTAL) {
			BlockPos side = pos.offset(dir);
			updateWireConnections(side);
		}
	}
	
	/**
	 * Update the wire connections of the {@code WireNode}s
	 * at the horizontal neighbors of the given position
	 * in the world if those {@code WireNode}s are of the
	 * given {@code WireBlock}.
	 */
	default void updateWireConnectionsAround(WireBlock wireBlock, BlockPos pos) {
		for (Direction dir : WireHandler.Directions.HORIZONTAL) {
			BlockPos side = pos.offset(dir);
			updateWireConnections(wireBlock, side);
		}
	}
}
