package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(ChestBlock.class)
public class ChestBlockMixin implements IBlock {
	
	@Shadow @Final private int field_831; // chest type, 0 = normal, 1 = trapped
	
	@Override
	public boolean emitsWeakPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return isTrapped();
	}
	
	@Override
	public boolean emitsStrongPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return isTrapped() && dir == Direction.UP;
	}
	
	private boolean isTrapped() {
		return field_831 == 1;
	}
}
