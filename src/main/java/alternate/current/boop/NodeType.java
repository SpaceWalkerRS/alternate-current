package alternate.current.boop;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public enum NodeType {
	WIRE, SOLID_BLOCK, REDSTONE_COMPONENT, OTHER;
	
	public static NodeType of(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		if (wireBlock.isOf(state)) {
			return NodeType.WIRE;
		} else if (state.isSolidBlock(world, pos)) {
			return NodeType.SOLID_BLOCK;
		} else if (state.emitsRedstonePower()) {
			return NodeType.REDSTONE_COMPONENT;
		}
		
		return NodeType.OTHER;
	}
}
