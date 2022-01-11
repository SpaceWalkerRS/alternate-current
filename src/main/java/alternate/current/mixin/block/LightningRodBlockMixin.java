package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

@Mixin(LightningRodBlock.class)
public class LightningRodBlockMixin implements IBlock {
	
	@Override
	public boolean emitsSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}
	
	@Override
	public boolean emitsDirectSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return state.getValue(BlockStateProperties.FACING) == dir;
	}
}
