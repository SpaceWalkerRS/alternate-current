package alternate.current.util;

import alternate.current.wire.WireBlock;

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
		return this == AIR;
	}

	public boolean isConductor() {
		return block.isFullCube();
	}

	public boolean isPowerSource() {
		return block.emitsRedstonePower();
	}

	public int getSignalFrom(World world, BlockPos pos, Direction dir) {
		return block.method_426(world, pos.x, pos.y, pos.z, dir.index);
	}

	public int getDirectSignalFrom(World world, BlockPos pos, Direction dir) {
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
		block.method_408(world, pos.x, pos.y, pos.z, fromBlock);
	}
}
