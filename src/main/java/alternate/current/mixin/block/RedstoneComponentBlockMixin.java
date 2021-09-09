package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneComponentBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(RedstoneComponentBlock.class)
public class RedstoneComponentBlockMixin implements IBlock {
	
	@Override
	public boolean emitsWeakPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return state.get(RedstoneComponentBlock.FACING) == dir;
	}
	
	@Override
	public boolean emitsStrongPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return state.get(RedstoneComponentBlock.FACING) == dir;
	}
}
