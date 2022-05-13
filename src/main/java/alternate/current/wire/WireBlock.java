package alternate.current.wire;

import alternate.current.interfaces.mixin.IServerLevel;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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

	default void onWireAdded(Level level, BlockPos pos) {
		if (!level.isClientSide()) {
			((IServerLevel)level).getWireHandler().onWireAdded(pos, getWireType());
		}
	}

	default void onWireRemoved(Level level, BlockPos pos, BlockState state) {
		if (!level.isClientSide()) {
			((IServerLevel)level).getWireHandler().onWireRemoved(pos, state, getWireType());
		}
	}

	default void onWireUpdated(Level level, BlockPos pos) {
		if (!level.isClientSide()) {
			((IServerLevel)level).getWireHandler().onWireUpdated(pos, getWireType());
		}
	}
}
