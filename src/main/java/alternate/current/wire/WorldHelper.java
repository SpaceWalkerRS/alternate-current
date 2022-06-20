package alternate.current.wire;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;

public class WorldHelper {

	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;

	static BlockState getBlockState(ServerWorld world, BlockPos pos) {
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

		int blockId = section.method_3926(x, y, z);

		if (blockId == 0) {
			return BlockState.AIR;
		}

		int metadata = section.getBlockData(x, y, z);

		return new BlockState(blockId, metadata);
	}

	/**
	 * An optimized version of {@link net.minecraft.world.World#setBlockState
	 * World.setBlockState}. Since this method is only used to update redstone wire
	 * block states, lighting checks, height map updates, and block entity updates
	 * are omitted.
	 */
	static boolean setWireState(ServerWorld world, BlockPos pos, BlockState state) {
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

		int blockId = state.getBlockId();
		int prevBlockId = section.method_3926(x, y, z);

		if (blockId != prevBlockId) {
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
}
