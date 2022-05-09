package alternate.current.wire;

import alternate.current.interfaces.mixin.IServerWorld;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * This interface should be implemented by any wire block type. While it is the
 * WireType class that represents different wire types and how they behave, this
 * interface is used by the WireHandler to distinguish between wire blocks and
 * non wire blocks.
 * 
 * @author Space Walker
 */
public interface WireBlock {

	public WireType getWireType();

	default void onWireAdded(World world, BlockPos pos) {
		if (!world.method_16390()) {
			((IServerWorld)world).getWireHandler().onWireAdded(pos, getWireType());
		}
	}

	default void onWireRemoved(World world, BlockPos pos, BlockState state) {
		if (!world.method_16390()) {
			((IServerWorld)world).getWireHandler().onWireRemoved(pos, state, getWireType());
		}
	}

	default void onWireUpdated(World world, BlockPos pos) {
		if (!world.method_16390()) {
			((IServerWorld)world).getWireHandler().onWireUpdated(pos, getWireType());
		}
	}
}
