package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.class_3705;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeverBlock;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(LeverBlock.class)
public abstract class LeverBlockMixin extends class_3705 implements IBlock {

	private LeverBlockMixin(class_3692 properties) {
		super(properties);
	}

	@Override
	public boolean isSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}

	@Override
	public boolean isDirectSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return method_16672(state) == dir;
	}
}
