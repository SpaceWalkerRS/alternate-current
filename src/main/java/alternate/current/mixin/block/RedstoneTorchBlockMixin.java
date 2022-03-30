package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(RedstoneTorchBlock.class)
public class RedstoneTorchBlockMixin implements IBlock {

	@Override
	public boolean hasSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return dir != Direction.UP;
	}

	@Override
	public boolean hasDirectSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return dir == Direction.DOWN;
	}
}
