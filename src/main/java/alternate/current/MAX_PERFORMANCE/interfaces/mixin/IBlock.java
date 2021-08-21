package alternate.current.MAX_PERFORMANCE.interfaces.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public interface IBlock {
	
	default boolean emitsWeakPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}
	
	default boolean emitsStrongPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return false;
	}
}
