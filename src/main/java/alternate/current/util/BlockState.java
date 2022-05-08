package alternate.current.util;

import alternate.current.wire.WireBlock;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockState {

	public static final BlockState AIR = new BlockState(null, 0) {
		public int getBlockId() { return 0; }
		public boolean isOf(Block block) { return false; }
		public boolean isOf(WireBlock wireBlock) { return false; }
		public BlockState with(int metadata) { return this; }
		public boolean isAir() { return true; }
		public boolean isPowerSource() { return false; }
		public boolean hasSignalFrom(World world, BlockPos pos, Direction dir) { return false; }
		public boolean hasDirectSignalFrom(World world, BlockPos pos, Direction dir) { return false; }
		public boolean isWire() { return false; }
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

	public Block getBlock() {
		return block;
	}

	public int getBlockId() {
		return block.id;
	}

	public boolean isOf(Block block) {
		return this.block == block;
	}

	public boolean isOf(WireBlock wireBlock) {
		return this.block == wireBlock;
	}

	public int get() {
		return metadata;
	}

	public BlockState with(int metadata) {
		return new BlockState(block, metadata);
	}

	public boolean isAir() {
		return false;
	}

	public boolean isPowerSource() {
		return block.emitsRedstonePower();
	}

	public boolean hasSignalFrom(World world, BlockPos pos, Direction dir) {
		return block.method_426(world, pos.x, pos.y, pos.z, dir.index);
	}

	public boolean hasDirectSignalFrom(World world, BlockPos pos, Direction dir) {
		return block.method_444(world, pos.x, pos.y, pos.z, dir.index);
	}

	public boolean isWire() {
		return block instanceof WireBlock;
	}

	public boolean canSurviveAt(World world, BlockPos pos) {
		return block.method_434(world, pos.x, pos.y, pos.z);
	}

	public void dropAsItem(World world, BlockPos pos) {
		block.method_445(world, pos.x, pos.y, pos.z, 0, 0);
	}

	public void neighborChanged(World world, BlockPos pos, Block fromBlock) {
		block.method_408(world, pos.x, pos.y, pos.z, fromBlock.id);
	}
}
