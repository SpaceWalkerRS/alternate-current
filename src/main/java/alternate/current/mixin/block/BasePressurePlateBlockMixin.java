package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(BasePressurePlateBlock.class)
public class BasePressurePlateBlockMixin implements IBlock {
	
	@Override
	public boolean emitsSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}
	
	@Override
	public boolean emitsDirectSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return dir == Direction.UP;
	}
}
