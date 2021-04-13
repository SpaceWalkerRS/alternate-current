package fast.redstone;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Neighbor {
	
	public final NeighborType type;
	public final BlockPos pos;
	public final BlockState state;
	
	public Neighbor(NeighborType type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos;
		this.state = state;
	}
	
	public static Neighbor of(World world, BlockPos pos, BlockState state, Block wireBlock) {
		NeighborType type;
		
		if (state.isOf(wireBlock)) {
			type = NeighborType.WIRE;
		} else if (state.isSolidBlock(world, pos)) {
			type = NeighborType.SOLID_BLOCK;
		} else if (state.emitsRedstonePower()) {
			type = NeighborType.REDSTONE_COMPONENT;
		} else {
			type = NeighborType.OTHER;
		}
		
		return new Neighbor(type, pos, state);
	}
}
