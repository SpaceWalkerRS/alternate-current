package alternate.current.redstone;

import alternate.current.AlternateCurrentMod;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;

/**
 * A Node represents a block in the world. It is tied to a
 * specific wire block type so it can be identified as part of
 * a wire network or as a neighbor of a wire network. It also
 * holds a few other pieces of information that speed up the
 * calculations in the WireHandler class.
 * 
 * @author Space Walker
 */
public class Node {
	
	// flags that encode the Node type
	private static final int SOLID_BLOCK = 1;
	private static final int REDSTONE = 2;
	
	public final WireBlock wireBlock;
	public final WorldAccess world;
	
	public BlockPos pos;
	public BlockState state;
	
	private int flags;
	
	public Node(WireBlock wireBlock, WorldAccess world) {
		this.wireBlock = wireBlock;
		this.world = world;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Node) {
			Node node = (Node)o;
			return world == node.world && wireBlock == node.wireBlock && pos.equals(node.pos);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode();
	}
	
	public Node update(BlockPos pos, BlockState state) {
		this.pos = pos;
		this.state = state;
		
		this.flags = 0;
		
		if (wireBlock.isOf(state)) {
			AlternateCurrentMod.LOGGER.warn("Cannot update a Node to a WireNode!");
		} else {
			Block block = state.getBlock();
			
			if (block.isFullCube()) {
				this.flags |= SOLID_BLOCK;
			}
			if (block.emitsRedstonePower()) {
				this.flags |= REDSTONE;
			}
		}
		
		return this;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return this.wireBlock == wireBlock;
	}
	
	public boolean isWire() {
		return false;
	}
	
	public boolean isSolidBlock() {
		return (flags & SOLID_BLOCK) != 0;
	}
	
	public boolean isRedstoneComponent() {
		return (flags & REDSTONE) != 0;
	}
	
	public WireNode asWire() {
		throw new UnsupportedOperationException("Not a WireNode!");
	}
}
