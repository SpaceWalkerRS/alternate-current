package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;
import alternate.current.utils.Directions;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface IWorld {
	
	public void reset();
	
	public int getCount();
	
	/**
	 * Retrieve the {@code WireNode} at this position in the world.
	 * If there is none, try to add one.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	/**
	 * Retrieve the {@code WireNode} at this position in the world.
	 * If there is none and {@code orCreate} is {@code true},
	 * try to add one.
	 */
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate);
	
	/**
	 * Place the given {@code WireNode} in the world.
	 */
	public void placeWire(WireNode wire);
	
	/**
	 * Remove the given {@code WireNode} from the world.
	 */
	public void removeWire(WireNode wire);
	
	/**
	 * Update the wire connections of any {@code WireNode}
	 * at the given position in the world.
	 */
	public void updateWireConnections(BlockPos pos);
	
	/**
	 * Update the wire connections of the {@code WireNode}
	 * at the given position in the world if that
	 * {@code WireNode} is of the given {@code WireBlock}.
	 */
	public void updateWireConnections(WireBlock wireBlock, BlockPos pos);
	
	/**
	 * Update the wire connections of any {@code WireNode}s
	 * at the horizontal neighbors of the given position
	 * in the world
	 */
	public default void updateWireConnectionsAround(BlockPos pos) {
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.HORIZONTAL[index];
			BlockPos neighbor = pos.offset(dir);
			
			updateWireConnections(neighbor);
		}
	}
	
	/**
	 * Update the wire connections of the {@code WireNode}s
	 * at the horizontal neighbors of the given position
	 * in the world if those {@code WireNode}s are of the
	 * given {@code WireBlock}.
	 */
	public default void updateWireConnectionsAround(WireBlock wireBlock, BlockPos pos) {
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.HORIZONTAL[index];
			BlockPos neighbor = pos.offset(dir);
			
			updateWireConnections(wireBlock, neighbor);
		}
	}
}
