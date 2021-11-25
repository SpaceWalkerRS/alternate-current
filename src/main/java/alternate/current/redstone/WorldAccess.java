package alternate.current.redstone;

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

public class WorldAccess {
	
	private static final int Y_MIN = 0;
	private static final int Y_MAX = 256;
	
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
	
	/**
	 * A slightly optimized version of World.getBlockState.
	 */
	public BlockState getBlockState(BlockPos pos) {
		int y = pos.getY();
		
		if (y < Y_MIN || y >= Y_MAX) {
			return Blocks.AIR.getDefaultState();
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getBlockStorage()[y >> 4];
		
		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}
		
		return section.getBlockState(x & 15, y & 15, z & 15);
	}
	
	/**
	 * An optimized version of World.setBlockState. Since this method is
	 * only used to update redstone wire block states, lighting checks,
	 * height map updates, and block entity updates are omitted.
	 */
	public boolean setWireState(BlockPos pos, BlockState state) {
		if (!wireBlock.isOf(state)) {
			return false;
		}
		
		int y = pos.getY();
		
		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getBlockStorage()[y >> 4];
		
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
		
		section.method_1424(x, y, z, state);
		
		// notify clients of the BlockState change
		world.getPlayerWorldManager().method_6002(pos);
		// mark the chunk for saving
		chunk.setModified();
		
		return true;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		state.getBlock().dropAsItem(world, pos, state, 0);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), BlockUtil.FLAG_NOTIFY_CLIENTS);
	}
	
	public void updateNeighborBlock(BlockPos pos, Block fromBlock) {
		updateNeighborBlock(pos, getBlockState(pos), fromBlock);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, Block fromBlock) {
		state.getBlock().neighborUpdate(world, pos, state, fromBlock);
	}
	
	public boolean emitsWeakPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsWeakPowerTo(world, pos, state, dir);
	}
	
	public boolean emitsStrongPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsStrongPowerTo(world, pos, state, dir);
	}
	
	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getBlock().getWeakRedstonePower(world, pos, state, dir);
	}
	
	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getBlock().getStrongRedstonePower(world, pos, state, dir);
	}
	
	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.getBlock().canBePlacedAtPos(world, pos);
	}
}
