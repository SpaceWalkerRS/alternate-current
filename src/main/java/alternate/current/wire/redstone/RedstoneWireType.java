package alternate.current.wire.redstone;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.wire.LevelAccess;
import alternate.current.wire.WireType;

public class RedstoneWireType extends WireType {

	public RedstoneWireType(String name, int powerStep, boolean offer, boolean accept) {
		super(name, 0, 15, powerStep, offer, accept);
	}

	@Override
	public int getPower(LevelAccess level, BlockPos pos, BlockState state) {
		return state.get();
	}

	@Override
	public BlockState setPower(LevelAccess level, BlockPos pos, BlockState state, int power) {
		return state.with(power);
	}
}
