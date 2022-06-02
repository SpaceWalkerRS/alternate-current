package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.interfaces.mixin.IBlockState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BlockState.class)
public class BlockStateMixin implements IBlockState {

	@Shadow public Block getBlock() { return null; }

	@Override
	public boolean isSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return ((IBlock)getBlock()).isSignalSourceTo(level, pos, state(), dir);
	}

	@Override
	public boolean isDirectSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return ((IBlock)getBlock()).isDirectSignalSourceTo(level, pos, state(), dir);
	}

	private BlockState state() {
		return (BlockState)(Object)this;
	}
}
