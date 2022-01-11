package alternate.current.redstone;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * This interface should be implemented by each wire block type.
 * While Vanilla only has one wire block type, they could add
 * more in the future, and any mods that add more wire block
 * types that wish to take advantage of Alternate Current's
 * performance improvements should have those wire blocks
 * implement this interface.
 * 
 * @author Space Walker
 */
public interface WireBlock {
	
	public Block asBlock();
	
	public boolean isOf(BlockState state);
	
	/**
	 * The lowest possible power level a wire can have.
	 */
	public int getMinPower();
	
	/**
	 * The largest possible power level a wire can have.
	 */
	public int getMaxPower();
	
	/**
	 * The drop in power level from one wire to the next.
	 */
	public int getPowerStep();
	
	default int clampPower(int power) {
		return Mth.clamp(power, getMinPower(), getMaxPower());
	}
	
	/**
	 * Return the power level of the given wire based on its
	 * location and block state.
	 */
	public int getPower(LevelAccess level, BlockPos pos, BlockState state);
	
	/**
	 * Return a block state that holds the given new power level.
	 */
	public BlockState updatePowerState(LevelAccess level, BlockPos pos, BlockState state, int power);
	
	/**
	 * Find the connections between the given WireNode and
	 * neighboring WireNodes.
	 */
	public void findWireConnections(WireNode wire, WireHandler.NodeProvider nodeProvider);
	
}
