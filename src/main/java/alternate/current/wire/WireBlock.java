package alternate.current.wire;

import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;

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

	default void onWireAdded(World world, int x, int y, int z) {
		if (!world.isClient) {
			((IServerWorld)world).getWireHandler().onWireAdded(new BlockPos(x, y, z), getWireType());
		}
	}

	default void onWireRemoved(World world, int x, int y, int z, int blockId, int metadata) {
		if (!world.isClient) {
			((IServerWorld)world).getWireHandler().onWireRemoved(new BlockPos(x, y, z), new BlockState(blockId, metadata), getWireType());
		}
	}

	default void onWireUpdated(World world, int x, int y, int z) {
		if (!world.isClient) {
			((IServerWorld)world).getWireHandler().onWireUpdated(new BlockPos(x, y, z), getWireType());
		}
	}
}
