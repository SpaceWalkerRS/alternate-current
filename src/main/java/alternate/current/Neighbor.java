package alternate.current;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Neighbor {
	
	public NeighborType type;
	public BlockPos pos;
	public BlockState state;
	
	public Neighbor() {
		
	}
	
	public static Neighbor of(World world, BlockPos pos, BlockState state, Block wireBlock) {
		Neighbor neighbor = new Neighbor();
		neighbor.update(world, pos, state, wireBlock);
		
		return neighbor;
	}
	
	public void update(World world, BlockPos pos, BlockState state, Block wireBlock) {
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
		
		update(type, pos, state);
	}
	
	public void update(NeighborType type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos;
		this.state = state;
	}
}
