package alternate.current.redstone;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.interfaces.mixin.IChunkSection;

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
	
	public BlockState getBlockState(BlockPos pos) {
		int y = pos.getY();
		
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return Blocks.VOID_AIR.getDefaultState();
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		ChunkSection section = chunk.getSectionArray()[y >> 4];
		
		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}
		
		return section.getBlockState(x & 15, y & 15, z & 15);
	}
	
	public boolean setBlockState(BlockPos pos, BlockState state) {
		int y = pos.getY();
		
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return false;
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		ChunkSection section = chunk.getSectionArray()[y >> 4];
		
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
	
	public WireNode getWire(BlockPos pos, boolean create, boolean update) {
		int y = pos.getY();
		
		if (y < world.getBottomY() || y >= world.getTopY()) {
			return null;
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		Chunk chunk = world.getChunk(x >> 4, z >> 4, ChunkStatus.FULL, true);
		ChunkSection section = chunk.getSectionArray()[y >> 4];
		
		if (section == null) {
			return null;
		}
		
		x &= 15;
		y &= 15;
		z &= 15;
		
		WireNode wire = ((IChunkSection)section).getWire(x, y, z);
		
		if (wire == null) {
			if (create) {
				BlockState state = section.getBlockState(x, y, z);
				
				if (wireBlock.isOf(state)) {
					wire = new WireNode(wireBlock, this, pos, state);
					((IChunkSection)section).setWire(x, y, z, wire);
					
					if (update) {
						wire.connections.update();
					}
				}
			}
			
			return wire;
		}
		
		return wire.isOf(wireBlock) ? wire : null;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		Block.dropStacks(state, world, pos);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_LISTENERS);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		BlockState state = getBlockState(pos);
		
		if (!state.isAir() && !wireBlock.isOf(state)) {
			state.neighborUpdate(world, pos, fromBlock, fromPos, false);
		}
	}
	
	public void updateNeighborShape(BlockPos pos, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState prevState = getBlockState(pos);
		
		if (!prevState.isAir() && !wireBlock.isOf(prevState)) {
			BlockState newState = prevState.getStateForNeighborUpdate(fromDir, fromState, world, fromPos, fromPos);
			Block.replace(prevState, newState, world, pos, Block.NOTIFY_LISTENERS);
		}
	}
	
	public void updateShapesAround(BlockPos pos, BlockState state) {
		state.updateNeighbors(world, pos, Block.NOTIFY_LISTENERS);
		state.prepare(world, pos, Block.NOTIFY_LISTENERS);
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
