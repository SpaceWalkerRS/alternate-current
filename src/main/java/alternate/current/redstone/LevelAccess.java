package alternate.current.redstone;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class LevelAccess {

	private final ServerLevel level;

	public LevelAccess(ServerLevel level) {
		this.level = level;
	}

	/**
	 * A slightly optimized version of Level.getBlockState.
	 */
	public BlockState getBlockState(BlockPos pos) {
		int y = pos.getY();

		if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
			return Blocks.VOID_AIR.defaultBlockState();
		}

		int x = pos.getX();
		int z = pos.getZ();
		int index = level.getSectionIndex(y);

		ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		LevelChunkSection section = chunk.getSections()[index];

		if (section == null) {
			return Blocks.AIR.defaultBlockState();
		}

		return section.getBlockState(x & 15, y & 15, z & 15);
	}

	/**
	 * An optimized version of Level.setBlock. Since this method is only used to
	 * update redstone wire block states, lighting checks, height map updates, and
	 * block entity updates are omitted.
	 */
	public boolean setWireState(BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof WireBlock)) {
			return false;
		}

		int y = pos.getY();

		if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) {
			return false;
		}

		int x = pos.getX();
		int z = pos.getZ();
		int index = level.getSectionIndex(y);

		ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		LevelChunkSection section = chunk.getSections()[index];

		if (section == null) {
			return false; // we should never get here
		}

		BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);

		if (state == prevState) {
			return false;
		}

		// notify clients of the BlockState change
		level.getChunkSource().blockChanged(pos);
		// mark the chunk for saving
		chunk.setUnsaved(true);

		return true;
	}

	public boolean breakBlock(BlockPos pos, BlockState state) {
		Block.dropResources(state, level, pos);
		return level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
	}

	public void updateNeighborShape(BlockPos pos, BlockState state, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState newState = state.updateShape(fromDir, fromState, level, pos, fromPos);
		Block.updateOrDestroy(state, newState, level, pos, Block.UPDATE_CLIENTS);
	}

	public void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		getBlockState(pos).neighborChanged(level, pos, fromBlock, fromPos, false);
	}

	public void updateNeighborBlock(BlockPos pos, BlockState state, BlockPos fromPos, Block fromBlock) {
		state.neighborChanged(level, pos, fromBlock, fromPos, false);
	}

	public boolean isConductor(BlockPos pos) {
		return getBlockState(pos).isRedstoneConductor(level, pos);
	}

	public boolean isConductor(BlockPos pos, BlockState state) {
		return state.isRedstoneConductor(level, pos);
	}

	public boolean emitsWeakPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).hasSignalTo(level, pos, state, dir);
	}

	public boolean emitsStrongPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).hasDirectSignalTo(level, pos, state, dir);
	}

	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getSignal(level, pos, dir);
	}

	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getDirectSignal(level, pos, dir);
	}

	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.canSurvive(level, pos);
	}
}
