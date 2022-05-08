package alternate.current.interfaces.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface IBlock {

	default boolean isSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}

	default boolean isDirectSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}
}
