package alternate.current.wire;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.util.BlockUtil;
import alternate.current.util.Direction;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public class LevelAccess {

	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;

	private final ServerWorld world;

	LevelAccess(ServerWorld world) {
		this.world = world;
	}

	/**
	 * A slightly optimized version of Level.getBlockState.
	 */
	BlockState getBlockState(BlockPos pos) {
		int y = pos.y;

		if (y < Y_MIN || y >= Y_MAX) {
			return BlockState.AIR;
		}

		int x = pos.x;
		int z = pos.z;

		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getBlockStorage()[y >> 4];

		if (section == null) {
			return BlockState.AIR; // we should never get here
		}

		x &= 15;
		y &= 15;
		z &= 15;

		Block block = section.getBlockAtPos(x, y, z);

		if (block == Blocks.AIR) {
			return BlockState.AIR;
		}

		return new BlockState(block, section.getBlockData(x, y, z));
	}

	/**
	 * An optimized version of Level.setBlock. Since this method is only used to
	 * update redstone wire block states, lighting checks, height map updates, and
	 * block entity updates are omitted.
	 */
	boolean setWireState(BlockPos pos, BlockState state) {
		if (!state.isWire()) {
			return false;
		}

		int y = pos.y;

		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}

		int x = pos.x;
		int z = pos.z;

		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getBlockStorage()[y >> 4];

		if (section == null) {
			return false; // we should never get here
		}

		x &= 15;
		y &= 15;
		z &= 15;

		Block prevBlock = section.getBlockAtPos(x, y, z);

		if (!state.isOf(prevBlock)) {
			return false;
		}

		int metadata = state.get();
		int prevMetadata = section.getBlockData(x, y, z);

		if (metadata == prevMetadata) {
			return false;
		}

		section.method_3932(x, y, z, metadata);

		// notify clients of the BlockState change
		world.getPlayerWorldManager().method_2105(pos.x, pos.y, pos.z);
		// mark the chunk for saving
		chunk.setModified();

		return true;
	}

	boolean breakWire(BlockPos pos, BlockState state) {
		state.dropAsItem(world, pos);
		return world.method_4721(pos.x, pos.y, pos.z, Blocks.AIR, 0, BlockUtil.FLAG_UPDATE_CLIENTS);
	}

	void updateNeighborBlock(BlockPos pos, Block fromBlock) {
		getBlockState(pos).neighborChanged(world, pos, fromBlock);
	}

	void updateNeighborBlock(BlockPos pos, BlockState state, Block fromBlock) {
		state.neighborChanged(world, pos, fromBlock);
	}

	public boolean isConductor(BlockPos pos) {
		return getBlockState(pos).isConductor();
	}

	public boolean isConductor(BlockPos pos, BlockState state) {
		return state.isConductor();
	}

	public int getSignalFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getSignalFrom(world, pos, dir);
	}

	public int getDirectSignalFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getDirectSignalFrom(world, pos, dir);
	}

	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.canSurviveAt(world, pos);
	}
}
