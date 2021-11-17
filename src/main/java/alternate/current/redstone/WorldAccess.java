package alternate.current.redstone;

import alternate.current.interfaces.mixin.IBlock;
import alternate.current.util.BlockUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ObserverBlock;
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
		
		Chunk chunk = world.loadChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getSections()[y >> 4];
		
		if (section == null) {
			return Blocks.AIR.getDefaultState();
		}
		
		return section.method_27435(x & 15, y & 15, z & 15);
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
		
		Chunk chunk = world.loadChunk(x >> 4, z >> 4);
		ChunkSection section = chunk.getSections()[y >> 4];
		
		if (section == null) {
			return false; // we should never get here
		}
		
		x &= 15;
		y &= 15;
		z &= 15;
		
		BlockState prevState = section.method_27435(x, y, z);
		
		if (state == prevState) {
			return false;
		}
		
		section.method_27437(x, y, z, state);
		
		// notify clients of the BlockState change
		world.getRaidManager().onBlockChange(pos);
		// mark the chunk for saving
		chunk.markDirty();
		
		return true;
	}
	
	public boolean breakBlock(BlockPos pos, BlockState state) {
		state.getBlock().method_26417(world, pos, state, 0);
		return world.setBlockState(pos, Blocks.AIR.getDefaultState(), BlockUtil.FLAG_NOTIFY_CLIENTS);
	}
	
	public void updateObserver(BlockPos pos, Block fromBlock, BlockPos fromPos) {
		BlockState state = getBlockState(pos);
		Block block = state.getBlock();
		
		if (block == Blocks.OBSERVER) {
			((ObserverBlock)block).method_26711(state, world, pos, fromBlock, fromPos);
		}
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockPos fromPos, Block fromBlock) {
		getBlockState(pos).neighbourUpdate(world, pos, fromBlock, fromPos);
	}
	
	public void updateNeighborBlock(BlockPos pos, BlockState state, BlockPos fromPos, Block fromBlock) {
		state.neighbourUpdate(world, pos, fromBlock, fromPos);
	}
	
	public boolean isConductor(BlockPos pos) {
		return getBlockState(pos).isSolidBlock();
	}
	
	public boolean isConductor(BlockPos pos, BlockState state) {
		return state.isSolidBlock();
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
		return !state.getBlock().canReplace(world, pos);
	}
}
