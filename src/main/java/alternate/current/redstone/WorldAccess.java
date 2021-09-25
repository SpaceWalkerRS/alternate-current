package alternate.current.redstone;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.util.Direction;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.BlockStorage;
import net.minecraft.world.chunk.Chunk;

public class WorldAccess {
	
	private static final int X_MIN = -30000000;
	private static final int X_MAX = 30000000;
	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;
	private static final int Z_MIN = -30000000;
	private static final int Z_MAX = 30000000;
	
	private final WireBlock wireBlock;
	private final ServerWorld world;
	private final WireHandler wireHandler;
	
	public WorldAccess(WireBlock wireBlock, ServerWorld world) {
		this.wireBlock = wireBlock;
		this.world = world;
		this.wireHandler = new WireHandler(this.wireBlock, this);
	}
	
	public WireHandler getWireHandler() {
		return wireHandler;
	}
	
	private boolean isInWorld(int x, int y, int z) {
		return z >= X_MIN && z < X_MAX && y >= Y_MIN && y < Y_MAX && z >= Z_MIN && z < Z_MAX;
	}
	
	public BlockState getBlockState(BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		if (!isInWorld(x, y, z)) {
			return BlockState.AIR;
		}
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		BlockStorage storage = chunk.getBlockStorage()[y >> 4];
		
		if (storage == null) {
			return BlockState.AIR;
		}
		
		return getBlockState(storage, x & 15, y & 15, z & 15);
	}
	
	private BlockState getBlockState(BlockStorage storage, int x, int y, int z) {
		Block block = storage.method_8935(x, y, z);
		return block == Blocks.AIR ? BlockState.AIR : new BlockState(block, storage.method_8940(x, y, z));
	}
	
	/**
	 * An optimized version of World.setBlockState. Since this method is
	 * only used to update redstone wire block states, lighting checks,
	 * height map updates, and block entity updates are omitted.
	 */
	public boolean setBlockState(BlockPos pos, BlockState state) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		if (!isInWorld(x, y, z)) {
			return false;
		}
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		BlockStorage storage = chunk.getBlockStorage()[y >> 4];
		
		if (storage == null) {
			return false;
		}
		
		x &= 15;
		y &= 15;
		z &= 15;
		
		BlockState prevState = getBlockState(storage, x, y, z);
		
		if (state.equals(prevState)) {
			return false;
		}
		
		storage.method_8937(x, y, z, state.getBlock());
		storage.method_8936(x, y, z, state.getBlockData());
		
		// notify clients of the BlockState change
		world.method_8518(pos.getX(), pos.getY(), pos.getZ());
		// mark the chunk for saving
		chunk.method_1385();
		
		return true;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		state.getBlock().method_8624(world, pos.getX(), pos.getY(), pos.getZ(), 0, 0);
		return world.method_8515(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public void updateNeighborBlock(BlockPos pos, Block fromBlock) {
		updateNeighborBlock(pos, getBlockState(pos), fromBlock);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, Block fromBlock) {
		state.getBlock().neighborUpdate(world, pos.getX(), pos.getY(), pos.getZ(), fromBlock);
	}
	
	public boolean isSolidBlock(BlockPos pos) {
		return getBlockState(pos).isSolid();
	}
	
	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getWeakPowerFrom(world, pos, dir);
	}
	
	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getWeakPowerFrom(world, pos, dir);
	}
	
	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.canBePlacedAt(world, pos);
	}
}
