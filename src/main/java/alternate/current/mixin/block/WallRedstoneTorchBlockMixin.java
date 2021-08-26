package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.redstone.interfaces.mixin.IBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallRedstoneTorchBlock;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(WallRedstoneTorchBlock.class)
public class WallRedstoneTorchBlockMixin implements IBlock {
	
	@Override
	public boolean emitsWeakPowerTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return state.get(Properties.HORIZONTAL_FACING) != dir;
	}
}
