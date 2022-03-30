package alternate.current.interfaces.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public interface IBlock {

	default boolean hasSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}

	default boolean hasDirectSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}
}
