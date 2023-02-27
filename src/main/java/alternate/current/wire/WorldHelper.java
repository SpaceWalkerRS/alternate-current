package alternate.current.wire;

import net.minecraft.block.state.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.chunk.WorldChunkSection;

public class WorldHelper {

	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;

	/**
	 * An optimized version of {@link net.minecraft.world.World#setBlockState
	 * World.setBlockState}. Since this method is only used to update redstone wire block
	 * states, lighting checks, height map updates, and block entity updates are
	 * omitted.
	 */
	static boolean setWireState(ServerWorld world, BlockPos pos, BlockState state) {
		int y = pos.getY();

		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}

		int x = pos.getX();
		int z = pos.getZ();

		WorldChunk chunk = world.getChunkAt(x >> 4, z >> 4);
		WorldChunkSection section = chunk.getSections()[y >> 4];

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
		world.getChunkMap().onBlockChanged(pos);
		// mark the chunk for saving
		chunk.setDirty(true);

		return true;
	}
}
