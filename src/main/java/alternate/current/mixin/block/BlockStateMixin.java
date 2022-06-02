package alternate.current.mixin.block;

import org.spongepowered.asm.mixin.Mixin;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.MapCodec;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.interfaces.mixin.IBlockState;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

@Mixin(BlockState.class)
public abstract class BlockStateMixin extends BlockStateBase implements IBlockState {

	private BlockStateMixin(Block block, ImmutableMap<Property<?>, Comparable<?>> valaues, MapCodec<BlockState> codec) {
		super(block, valaues, codec);
	}

	@Override
	public boolean isSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return ((IBlock)getBlock()).isSignalSourceTo(level, pos, asState(), dir);
	}

	@Override
	public boolean isDirectSignalSourceTo(Level level, BlockPos pos, Direction dir) {
		return ((IBlock)getBlock()).isDirectSignalSourceTo(level, pos, asState(), dir);
	}
}
