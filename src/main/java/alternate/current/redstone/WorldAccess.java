package alternate.current.redstone;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.util.BlockUtil;

import net.minecraft.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

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
			return Blocks.VOID_AIR.getDefaultState();
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		WorldChunk chunk = world.method_8392(x >> 4, z >> 4);
		ChunkSection section = chunk.getSectionArray()[y >> 4];
		
		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}
		
		return section.method_12254(x & 15, y & 15, z & 15);
	}
	
	/**
	 * An optimized version of World.setBlockState. Since this method is
	 * only used to update redstone wire block states, lighting checks,
	 * height map updates, and block entity updates are omitted.
	 */
	public boolean setWireState(BlockPos pos, BlockState state) {
		int y = pos.getY();
		
		if (y < Y_MIN || y >= Y_MAX) {
			return false;
		}
		
		int x = pos.getX();
		int z = pos.getZ();
		
		WorldChunk chunk = world.method_8392(x >> 4, z >> 4);
		ChunkSection section = chunk.getSectionArray()[y >> 4];
		
		if (section == null) {
			return false; // we should never get here
		}
		
		x &= 15;
		y &= 15;
		z &= 15;
		
		BlockState prevState = section.method_12254(x, y, z);
		
		if (state == prevState) {
			return false;
		}
		
		section.method_12256(x, y, z, state);
		
		// notify clients of the BlockState change
		world.method_73521().method_73551(pos);
		// mark the chunk for saving
		chunk.markDirty();
		
		return true;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		state.dropStacks(world, pos, 0);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), BlockUtil.FLAG_NOTIFY_CLIENTS);
	}
	
	public void updateNeighborShape(BlockPos pos, BlockState state, Direction fromDir, BlockPos fromPos, BlockState fromState) {
		BlockState newState = state.getStateForNeighborUpdate(fromDir, fromState, world, pos, fromPos);
		Block.replaceBlock(state, newState, world, pos, BlockUtil.FLAG_NOTIFY_CLIENTS);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		getBlockState(pos).method_73267(world, pos, fromBlock, fromPos);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, BlockPos fromPos, Block fromBlock) {
		state.method_73267(world, pos, fromBlock, fromPos);
	}
	
	public boolean isConductor(BlockPos pos) {
		return getBlockState(pos).method_73303();
	}
	
	public boolean isConductor(BlockPos pos, BlockState state) {
		return state.method_73303();
	}
	
	public boolean emitsWeakPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsWeakPowerTo(world, pos, state, dir);
	}
	
	public boolean emitsStrongPowerTo(BlockPos pos, BlockState state, Direction dir) {
		return ((IBlock)state.getBlock()).emitsStrongPowerTo(world, pos, state, dir);
	}
	
	public int getWeakPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.method_73259(world, pos, dir);
	}
	
	public int getStrongPowerFrom(BlockPos pos, BlockState state, Direction dir) {
		return state.method_73281(world, pos, dir);
	}
	
	public boolean shouldBreak(BlockPos pos, BlockState state) {
		return !state.method_73272(world, pos);
	}
}
