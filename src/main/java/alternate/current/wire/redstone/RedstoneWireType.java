package alternate.current.wire.redstone;

import alternate.current.wire.LevelAccess;
import alternate.current.wire.WireType;

import net.minecraft.block.BlockState;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.util.math.BlockPos;

public class RedstoneWireType extends WireType {

	public RedstoneWireType(String name, int powerStep, boolean offer, boolean accept) {
		super(name, 0, 15, powerStep, offer, accept);
	}

	@Override
	public int getPower(LevelAccess level, BlockPos pos, BlockState state) {
		return state.get(RedstoneWireBlock.POWER);
	}

	@Override
	public BlockState setPower(LevelAccess level, BlockPos pos, BlockState state, int power) {
		return state.with(RedstoneWireBlock.POWER, power);
	}
}
