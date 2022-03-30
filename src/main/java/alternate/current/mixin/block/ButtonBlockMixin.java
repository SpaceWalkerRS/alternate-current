package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(ButtonBlock.class)
public abstract class ButtonBlockMixin extends FaceAttachedHorizontalDirectionalBlock implements IBlock {

	private ButtonBlockMixin(Properties properties) {
		super(properties);
	}

	public boolean hasSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return true;
	}

	public boolean hasDirectSignalTo(Level level, BlockPos pos, BlockState state, Direction dir) {
		return getConnectedDirection(state) == dir;
	}
}
