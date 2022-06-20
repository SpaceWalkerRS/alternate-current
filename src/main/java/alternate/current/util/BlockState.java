package alternate.current.util;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockState {

	public static final BlockState AIR = new BlockState(null, 0) {
		public int getBlockId() { return 0; }
		public boolean is(Block block) { return false; }
		public BlockState with(int metadata) { return this; }
		public boolean isAir() { return true; }
		public boolean isSignalSource() { return false; }
		public boolean hasSignal(World world, BlockPos pos, Direction dir) { return false; }
		public boolean hasDirectSignal(World world, BlockPos pos, Direction dir) { return false; }
		public boolean canSurviveAt(World world, BlockPos pos) { return true; }
		public void dropAsItem(World world, BlockPos pos) { }
		public void neighborChanged(World world, BlockPos pos, Block fromBlock) { }
	};

	private final Block block;
	private final int metadata;

	public BlockState(int blockId, int metadata) {
		this(Block.BLOCKS[blockId], metadata);
	}

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

	public int getBlockId() {
		return block.id;
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

	public BlockState with(int metadata) {
		return new BlockState(block, metadata);
	}

	public boolean isAir() {
		return this == AIR;
	}

	public boolean isSignalSource() {
		return block.emitsRedstonePower();
	}

	public boolean hasSignal(World world, BlockPos pos, Direction dir) {
		return block.method_426(world, pos.x, pos.y, pos.z, dir.index);
	}

	public boolean hasDirectSignal(World world, BlockPos pos, Direction dir) {
		return block.method_444(world, pos.x, pos.y, pos.z, dir.index);
	}

	public boolean canSurviveAt(World world, BlockPos pos) {
		return block.canPlaceBlockAt(world, pos.x, pos.y, pos.z);
	}

	public void dropAsItem(World world, BlockPos pos) {
		block.canStayPlaced(world, pos.x, pos.y, pos.z, 0, 0);
	}

	public void neighborChanged(World world, BlockPos pos, Block neighborBlock) {
		block.method_408(world, pos.x, pos.y, pos.z, neighborBlock.id);
	}
}
