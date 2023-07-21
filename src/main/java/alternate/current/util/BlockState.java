package alternate.current.util;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class BlockState {

	public static final BlockState AIR = new BlockState(Blocks.AIR, 0);

	private final Block block;
	private final int metadata;

	public BlockState(Block block, int metadata) {
		this.block = block;
		this.metadata = metadata;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlockState) {
			BlockState state = (BlockState)obj;
			return state.block == block && state.metadata == metadata;
		}

		return false;
	}

	public Block getBlock() {
		return block;
	}

	public boolean is(Block block) {
		return this.block == block;
	}

	public int get() {
		return metadata;
	}

	public BlockState set(int metadata) {
		return new BlockState(block, metadata);
	}

	public boolean isAir() {
		return this == AIR;
	}

	public boolean isConductor() {
		return block.isConductor();
	}

	public boolean isSignalSource() {
		return block.isSignalSource();
	}

	public int getSignal(World world, BlockPos pos, Direction dir) {
		return block.getSignal(world, pos.x, pos.y, pos.z, dir.index);
	}

	public int getDirectSignal(World world, BlockPos pos, Direction dir) {
		return block.getDirectSignal(world, pos.x, pos.y, pos.z, dir.index);
	}

	public boolean canSurvive(World world, BlockPos pos) {
		return block.canSurvive(world, pos.x, pos.y, pos.z);
	}

	public void dropItems(World world, BlockPos pos) {
		block.dropItems(world, pos.x, pos.y, pos.z, 0, 0);
	}

	public void neighborChanged(World world, BlockPos pos, Block neighborBlock) {
		block.neighborChanged(world, pos.x, pos.y, pos.z, neighborBlock);
	}
}
