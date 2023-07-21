package alternate.current.wire;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;

import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public class WorldHelper {

	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;

	static BlockState getBlockState(World world, BlockPos pos) {
		int y = pos.y;

		if (y < Y_MIN || y >= Y_MAX) {
			return BlockState.AIR;
		}

		int x = pos.x;
		int z = pos.z;

		WorldChunk chunk = world.getChunkAt(x >> 4, z >> 4);
		WorldChunkSection section = chunk.getSections()[y >> 4];

		if (section == null) {
			return BlockState.AIR; // we should never get here
		}

		x &= 15;
		y &= 15;
		z &= 15;

		int blockId = section.getBlock(x, y, z);

		if (blockId == 0) {
			return BlockState.AIR;
		}

		int metadata = section.getBlockMetadata(x, y, z);

		return new BlockState(blockId, metadata);
	}

	/**
	 * An optimized version of {@link net.minecraft.world.World#setBlockState
	 * World.setBlockState}. Since this method is only used to update redstone wire block
	 * states, lighting checks, height map updates, and block entity updates are
	 * omitted.
	 */
	static boolean setWireState(World world, BlockPos pos, BlockState state) {
		int y = pos.y;

		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}

		int x = pos.x;
		int z = pos.z;

		WorldChunk chunk = world.getChunkAt(x >> 4, z >> 4);
		WorldChunkSection section = chunk.getSections()[y >> 4];

		if (section == null) {
			return false; // we should never get here
		}

		x &= 15;
		y &= 15;
		z &= 15;

		int blockId = state.getBlockId();
		int prevBlockId = section.getBlock(x, y, z);

		if (blockId != prevBlockId) {
			return false;
		}

		int metadata = state.get();
		int prevMetadata = section.getBlockMetadata(x, y, z);

		if (metadata == prevMetadata) {
			return false;
		}

		section.setBlockMetadata(x, y, z, metadata);

		// notify clients of the BlockState change
		world.onBlockChanged(pos.x, pos.y, pos.z);
		// mark the chunk for saving
		chunk.markDirty();

		return true;
	}
}
