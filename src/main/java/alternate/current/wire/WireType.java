package alternate.current.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;

/**
 * This class holds all information that specifies how a wire of this type
 * should interact with other wires.
 * 
 * @author Space Walker
 */
public abstract class WireType {

	public final String name;
	public final int minPower;
	public final int maxPower;
	public final int powerStep;

	// default behavior when interacting with other wire types
	public final boolean offer;
	public final boolean accept;

	protected WireType(String name, int minPower, int maxPower, int powerStep, boolean offer, boolean accept) {
		if (powerStep < 0) {
			throw new IllegalArgumentException("powerStep must be at least 0!");
		}

		this.name = name;
		this.minPower = minPower;
		this.maxPower = maxPower;
		this.powerStep = powerStep;

		this.offer = offer;
		this.accept = accept;
	}

	public final int clamp(int power) {
		return Mth.clamp(power, minPower, maxPower);
	}

	public abstract int getPower(LevelAccess level, BlockPos pos, BlockState state);

	public abstract BlockState setPower(LevelAccess level, BlockPos pos, BlockState state, int power);

}
