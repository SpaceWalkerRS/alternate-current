package fast.redstone;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

public class Neighbor {
	
	private final NeighborType type;
	private final BlockPos pos;
	
	private BlockState state;
	
	public Neighbor(NeighborType type, BlockPos pos, BlockState state) {
		this.type = type;
		this.pos = pos;
		
		this.state = state;
	}
	
	public NeighborType getType() {
		return type;
	}
	
	public boolean isWire() {
		return type == NeighborType.WIRE;
	}
	
	public BlockPos getPos() {
		return pos;
	}
	
	public BlockState getState() {
		return state;
	}
	
	public void updateState(BlockState state) {
		this.state = state;
	}
}
