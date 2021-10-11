package alternate.current.redstone;

import alternate.current.interfaces.mixin.IBlock;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.WorldChunk;

public class WorldAccess {
	
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
		
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return Blocks.VOID_AIR.getDefaultState();
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		int index = world.getSectionIndex(y);
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		ChunkSection section = chunk.getSectionArray()[index];
		
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
		int y = pos.getY();
		
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return false;
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		int index = world.getSectionIndex(y);
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		ChunkSection section = chunk.getSectionArray()[index];
		
		if (section == null) {
			return false;
		}
		
		BlockState prevState = section.setBlockState(x & 15, y & 15, z & 15, state);
		
		if (state == prevState) {
			return false;
		}
		
		// notify clients of the BlockState change
		world.getChunkManager().markForUpdate(pos);
		// mark the chunk for saving
		((WorldChunk)chunk).markDirty();
		
		return true;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		Block.dropStacks(state, world, pos);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
	}
	
	public void updateNeighborShape(BlockPos pos, BlockState state, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState newState = state.getStateForNeighborUpdate(fromDir, fromState, world, pos, fromPos);
		Block.replace(state, newState, world, pos, Block.NOTIFY_LISTENERS);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		getBlockState(pos).neighborUpdate(world, pos, fromBlock, fromPos, false);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, BlockPos fromPos, Block fromBlock) {
		state.neighborUpdate(world, pos, fromBlock, fromPos, false);
	}
	
	public boolean isSolidBlock(BlockPos pos) {
		return getBlockState(pos).isSolidBlock(world, pos);
	}
	
	public boolean isSolidBlock(BlockPos pos, BlockState state) {
		return state.isSolidBlock(world, pos);
	}
	
	public boolean emitsWeakPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsWeakPowerTo(world, pos, state, dir);
	}
	
	public boolean emitsStrongPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsStrongPowerTo(world, pos, state, dir);
	}
	
	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getWeakRedstonePower(world, pos, dir);
	}
	
	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.getStrongRedstonePower(world, pos, dir);
	}
	
	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.canPlaceAt(world, pos);
	}
}
