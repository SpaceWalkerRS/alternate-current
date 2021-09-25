package alternate.current.util;

import alternate.current.redstone.WireBlock;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;

public class BlockState {
	
	public static final BlockState AIR = new BlockState(Blocks.AIR, 0);
	
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
	
	public BlockState with(int blockData) {
		return new BlockState(block, blockData);
	}
	
	public boolean isAir() {
		return this == AIR;
	}
	
	public boolean isSolid() {
		return block.isFullCube();
	}
	
	public boolean emitsRedstonePower() {
		return block.emitsRedstonePower();
	}
	
	public int getWeakPowerFrom(World world, BlockPos pos, Direction dir) {
		return block.method_8626(world, pos.getX(), pos.getY(), pos.getZ(), dir.getIndex());
	}
	
	public int getStrongPowerFrom(World world, BlockPos pos, Direction dir) {
		return block.method_8630(world, pos.getX(), pos.getY(), pos.getZ(), dir.getIndex());
	}
	
	public boolean canBePlacedAt(World world, BlockPos pos) {
		return block.method_8628(world, pos.getX(), pos.getY(), pos.getZ());
	}
}
