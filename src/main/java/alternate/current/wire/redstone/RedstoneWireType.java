package alternate.current.wire.redstone;

import alternate.current.wire.LevelAccess;
import alternate.current.wire.WireType;

import net.minecraft.class_3772;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class RedstoneWireType extends WireType {

	public RedstoneWireType(String name, int powerStep, boolean offer, boolean accept) {
		super(name, 0, 15, powerStep, offer, accept);
	}

	@Override
	public int getPower(LevelAccess level, BlockPos pos, BlockState state) {
		return state.method_16934(class_3772.field_18753);
	}

	@Override
	public BlockState setPower(LevelAccess level, BlockPos pos, BlockState state, int power) {
		return state.method_16931(class_3772.field_18753, power);
	}
}
