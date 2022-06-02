package alternate.current.wire;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class LevelHelper {

	/**
	 * An optimized version of {@link net.minecraft.world.level.Level#setBlock
	 * Level.setBlock}. Since this method is only used to update redstone wire block
	 * states, lighting checks, height map updates, and block entity updates are
	 * omitted.
	 */
	static boolean setWireState(ServerLevel level, BlockPos pos, BlockState state, boolean updateNeighborShapes) {
		if (!state.is(Blocks.REDSTONE_WIRE)) {
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

		if (updateNeighborShapes) {
			prevState.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
			state.updateNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
			state.updateIndirectNeighbourShapes(level, pos, Block.UPDATE_CLIENTS);
		}

		return true;
	}
}
