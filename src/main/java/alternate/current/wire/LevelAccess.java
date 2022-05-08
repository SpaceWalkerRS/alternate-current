package alternate.current.wire;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.util.BlockUtil;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
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
		int y = pos.getY();

		if (y < Y_MIN || y >= Y_MAX) {
			return Blocks.VOID_AIR.getDefaultState();
		}

		int x = pos.getX();
		int z = pos.getZ();

		Chunk chunk = world.method_16347(x >> 4, z >> 4);
		ChunkSection section = chunk.method_17003()[y >> 4];

		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}

		return section.getBlockState(x & 15, y & 15, z & 15);
	}

	/**
	 * An optimized version of Level.setBlock. Since this method is only used to
	 * update redstone wire block states, lighting checks, height map updates, and
	 * block entity updates are omitted.
	 */
	boolean setWireState(BlockPos pos, BlockState state, boolean updateNeighborShapes) {
		if (!(state.getBlock() instanceof WireBlock)) {
			return false;
		}

		int y = pos.getY();

		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}

		int x = pos.getX();
		int z = pos.getZ();

		Chunk chunk = world.method_16347(x >> 4, z >> 4);
		ChunkSection section = chunk.method_17003()[y >> 4];

		if (section == null) {
			return false; // we should never get here
		}

		x &= 15;
		y &= 15;
		z &= 15;

		BlockState prevState = section.getBlockState(x, y, z);

		if (state == prevState) {
			return false;
		}

		section.setBlockState(x, y, z, state);

		// notify clients of the BlockState change
		world.getPlayerWorldManager().method_10748(pos);
		// mark the chunk for saving
		chunk.setModified(true);

		if (updateNeighborShapes) {
			prevState.method_16888(world, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
			state.method_16876(world, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
			state.method_16888(world, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
		}

		return true;
	}

	boolean breakWire(BlockPos pos, BlockState state) {
		state.method_16867(world, pos, 0);
		return world.method_8506(pos, Blocks.AIR.getDefaultState(), BlockUtil.FLAG_UPDATE_CLIENTS);
	}

	void updateNeighborShape(BlockPos pos, BlockState state, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState newState = state.method_16883(fromDir, fromState, world, pos, fromPos);
		Block.method_16572(state, newState, world, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
	}

	void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		getBlockState(pos).method_16872(world, pos, fromBlock, fromPos);
	}

	void updateNeighborBlock(BlockPos pos, BlockState state, BlockPos fromPos, Block fromBlock) {
		state.method_16872(world, pos, fromBlock, fromPos);
	}

	public boolean isConductor(BlockPos pos) {
		return getBlockState(pos).method_16907();
	}

	public boolean isConductor(BlockPos pos, BlockState state) {
		return state.method_16907();
	}

	public boolean isSignalSourceTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).isSignalSourceTo(world, pos, state, dir);
	}

	public boolean isDirectSignalSourceTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).isDirectSignalSourceTo(world, pos, state, dir);
	}

	public int getSignalFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.method_16864(world, pos, dir);
	}

	public int getDirectSignalFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.method_16886(world, pos, dir);
	}

	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.method_16877(world, pos);
	}
}
