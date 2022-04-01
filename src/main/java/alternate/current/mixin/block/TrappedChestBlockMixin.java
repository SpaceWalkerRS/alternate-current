package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.TrappedChestBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(TrappedChestBlock.class)
public class TrappedChestBlockMixin implements IBlock {

	@Override
	public boolean isSignalSourceTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}

	@Override
	public boolean isDirectSignalSourceTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return dir == Direction.UP;
	}
}
