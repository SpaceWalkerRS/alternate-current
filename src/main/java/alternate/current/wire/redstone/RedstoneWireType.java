package alternate.current.wire.redstone;

import alternate.current.wire.LevelAccess;
import alternate.current.wire.WireType;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class RedstoneWireType extends WireType {

	public RedstoneWireType(String name, int powerStep, boolean offer, boolean accept) {
		super(name, 0, 15, powerStep, offer, accept);
	}

	@Override
	public int getPower(LevelAccess level, BlockPos pos, BlockState state) {
		return state.getValue(BlockStateProperties.POWER);
	}

	@Override
	public BlockState setPower(LevelAccess level, BlockPos pos, BlockState state, int power) {
		return state.setValue(BlockStateProperties.POWER, power);
	}
}
