package alternate.current.redstone;

import alternate.current.util.BlockPos;
import alternate.current.util.BlockState;
import alternate.current.util.BlockUtil;
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
	
	public static boolean isOutOfBounds(BlockPos pos) {
		return isOutOfBounds(pos.getX(), pos.getY(), pos.getZ());
	}
	
	public static boolean isOutOfBounds(int x, int y, int z) {
		return x < X_MIN || x >= X_MAX || y < Y_MIN || y >= Y_MAX || z < Z_MIN || z >= Z_MAX;
	}
	
	/**
	 * A slightly optimized version of World.getBlockState.
	 */
	public BlockState getBlockState(BlockPos pos) {
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		if (isOutOfBounds(x, y, z)) {
			return BlockState.AIR;
		}
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		BlockStorage section = chunk.getBlockStorage()[y >> 4];
		
		if (section == null) {
			return BlockState.AIR;
		}
		
		return getBlockState(section, x & 15, y & 15, z & 15);
	}
	
	/**
	 * An optimized version of World.setBlockState. Since this method is
	 * only used to update redstone wire block states, lighting checks,
	 * height map updates, and block entity updates are omitted.
	 */
	public boolean setWireState(BlockPos pos, BlockState state) {
		if (!state.isOf(wireBlock)) {
			return false;
		}
		
		int x = pos.getX();
		int y = pos.getY();
		int z = pos.getZ();
		
		if (isOutOfBounds(x, y, z)) {
			return false;
		}
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		BlockStorage section = chunk.getBlockStorage()[y >> 4];
		
		if (section == null) {
			return false; // we should never get here
		}
		
		int sectionX = x & 15;
		int sectionY = y & 15;
		int sectionZ = z & 15;
		
		BlockState prevState = getBlockState(section, sectionX, sectionY, sectionZ);
		
		if (state.equals(prevState)) {
			return false;
		}
		
		setBlockState(section, sectionX, sectionY, sectionZ, state, prevState);
		
		// notify clients of the BlockState change
		world.getPlayerWorldManager().method_10362(x, y, z);
		// mark the chunk for saving
		chunk.method_1385();
		
		return true;
	}
	
	private BlockState getBlockState(BlockStorage section, int sectionX, int sectionY, int sectionZ) {
		Block block = section.method_8935(sectionX, sectionY, sectionZ);
		return block == Blocks.AIR ? BlockState.AIR : new BlockState(block, section.method_8940(sectionX, sectionY, sectionZ));
	}
	
	private void setBlockState(BlockStorage section, int sectionX, int sectionY, int sectionZ, BlockState state, BlockState prevState) {
		Block block = state.getBlock();
		
		if (!prevState.isOf(block)) {
			section.method_8937(sectionX, sectionY, sectionZ, block);
		}
		
		section.method_8936(sectionX, sectionY, sectionZ, state.getBlockData());
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		state.dropAsItem(world, pos);
		return state.breakBlock(world, pos, BlockUtil.FLAG_NOTIFY_CLIENTS);
	}
	
	public void updateNeighborBlock(BlockPos pos, Block fromBlock) {
		getBlockState(pos).neighborUpdate(world, pos, fromBlock);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, Block fromBlock) {
		state.neighborUpdate(world, pos, fromBlock);
	}
	
	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getWeakPowerFrom(world, pos, dir);
	}
	
	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getStrongPowerFrom(world, pos, dir);
	}
	
	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.canBePlacedAt(world, pos);
	}
}
