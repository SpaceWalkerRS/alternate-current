package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.class_3705;
import net.minecraft.block.AbstractButtonBlock;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

@Mixin(AbstractButtonBlock.class)
public abstract class AbstractButtonBlockMixin extends class_3705 implements IBlock {

	private AbstractButtonBlockMixin(class_3692 properties) {
		super(properties);
	}

	public boolean isSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}

	public boolean isDirectSignalSourceTo(World world, BlockPos pos, BlockState state, Direction dir) {
		return method_16672(state) == dir;
	}
}
