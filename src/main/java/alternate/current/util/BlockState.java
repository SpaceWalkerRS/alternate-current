package alternate.current.util;

import alternate.current.redstone.WireBlock;

import net.minecraft.block.Block;
import net.minecraft.world.World;

public class BlockState {
	
	public static final BlockState AIR;
	
	private final Block block;
	private final int blockData;
	
	public BlockState(Block block, int blockData) {
		this.block = block;
		this.blockData = blockData;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof BlockState) {
			BlockState state = (BlockState)obj;
			return state.block == block && state.blockData == blockData;
		}
		
		return false;
	}
	
	public Block getBlock() {
		return block;
	}
	
	public boolean isOf(Block block) {
		return block == this.block;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return wireBlock.asBlock() == this.block;
	}
	
	public int getBlockData() {
		return blockData;
	}
	
	public BlockState withBlockData(int blockData) {
		return new BlockState(block, blockData);
	}
	
	public boolean isAir() {
		return this == AIR;
	}
	
	public boolean emitsRedstonePower() {
		return block.emitsRedstonePower();
	}
	
	public int getWeakPowerFrom(World world, BlockPos pos, Direction dir) {
		return block.getWeakRedstonePower(world, pos.getX(), pos.getY(), pos.getZ(), dir.getIndex());
	}
	
	public int getStrongPowerFrom(World world, BlockPos pos, Direction dir) {
		return block.getStrongRedstonePower(world, pos.getX(), pos.getY(), pos.getZ(), dir.getIndex());
	}
	
	public boolean canBePlacedAt(World world, BlockPos pos) {
		return block.canReplace(world, pos.getX(), pos.getY(), pos.getZ());
	}
	
	public void dropAsItem(World world, BlockPos pos) {
		block.method_8624(world, pos.getX(), pos.getY(), pos.getZ(), 0, 0);
	}
	
	public boolean breakBlock(World world, BlockPos pos, int flags) {
		return world.setBlock(pos.getX(), pos.getY(), pos.getZ(), 0, 0, flags);
	}
	
	public void neighborUpdate(World world, BlockPos pos, Block fromBlock) {
		block.neighborUpdate(world, pos.getX(), pos.getY(), pos.getZ(), fromBlock.id);
	}
	
	static {
		AIR = new BlockState(null, 0) {
			@Override public boolean emitsRedstonePower() { return false; }
			@Override public int getWeakPowerFrom(World world, BlockPos pos, Direction dir) { return 0; }
			@Override public int getStrongPowerFrom(World world, BlockPos pos, Direction dir) { return 0; }
			@Override public boolean canBePlacedAt(World world, BlockPos pos) { return true; }
			@Override public void dropAsItem(World world, BlockPos pos) { }
			@Override public boolean breakBlock(World world, BlockPos pos, int flags) { return true; }
			@Override public void neighborUpdate(World world, BlockPos pos, Block fromBlock) { }
		};
	}
}
