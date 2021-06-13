package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;
import alternate.current.utils.Directions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public interface IWorld {
	
	public void reset();
	
	public int getCount();
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos);
	
	public WireNode getWire(WireBlock wireBlock, BlockPos pos, boolean orCreate);
	
	public void placeWire(WireNode wire);
	
	public void removeWire(WireNode wire);
	
	public void updateWireConnections(BlockPos pos);
	
	public void updateWireConnections(WireBlock wireBlock, BlockPos pos);
	
	public default void updateWireConnectionsAround(BlockPos pos) {
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.HORIZONTAL[index];
			BlockPos neighbor = pos.offset(dir);
			
			updateWireConnections(neighbor);
		}
	}
	
	public default void updateWireConnectionsAround(WireBlock wireBlock, BlockPos pos) {
		for (int index = 0; index < Directions.HORIZONTAL.length; index++) {
			Direction dir = Directions.HORIZONTAL[index];
			BlockPos neighbor = pos.offset(dir);
			
			updateWireConnections(wireBlock, neighbor);
		}
	}
}
