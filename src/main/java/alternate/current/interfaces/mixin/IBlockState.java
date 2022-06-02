package alternate.current.interfaces.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

public interface IBlockState {

	default boolean isSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return false;
	}

	default boolean isDirectSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return false;
	}
}
