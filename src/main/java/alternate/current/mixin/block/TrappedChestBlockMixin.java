package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.BlockState;
import net.minecraft.block.TrappedChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(TrappedChestBlock.class)
public class TrappedChestBlockMixin implements IBlock {
	
	@Override
	public boolean emitsWeakPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}
	
	@Override
	public boolean emitsStrongPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return dir == Direction.UP;
	}
}
