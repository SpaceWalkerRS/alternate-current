package alternate.current.wire;

import alternate.current.util.BlockUtil;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;

class LevelHelper {

	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;

	/**
	 * An optimized version of {@link net.minecraft.world.level.Level#setBlock
	 * Level.setBlock}. Since this method is only used to update redstone wire block
	 * states, lighting checks, height map updates, and block entity updates are
	 * omitted.
	 */
	static boolean setWireState(ServerLevel level, BlockPos pos, BlockState state, boolean updateNeighborShapes) {
		int y = pos.getY();

		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}

		int x = pos.getX();
		int z = pos.getZ();

		ChunkAccess chunk = level.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		LevelChunkSection section = chunk.getSections()[y >> 4];

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

		if (updateNeighborShapes) {
			prevState.updateIndirectNeighbourShapes(level, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
			state.updateNeighbourShapes(level, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
			state.updateIndirectNeighbourShapes(level, pos, BlockUtil.FLAG_UPDATE_CLIENTS);
		}

		return true;
	}
}
