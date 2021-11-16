package alternate.current.redstone;

import java.util.Arrays;

import alternate.current.redstone.WireHandler.Directions;

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
	private static final int CONDUCTOR = 0b01;
	private static final int REDSTONE  = 0b10;
	
	public final WireBlock wireBlock;
	public final WorldAccess world;
	public final Node[] neighbors;
	
	public BlockPos pos;
	public BlockState state;
	public boolean removed;
	
	private int flags;
	
	public Node(WireBlock wireBlock, WorldAccess world) {
		this.wireBlock = wireBlock;
		this.world = world;
		this.neighbors = new Node[Directions.ALL.length];
	}
	
	@Override
	public boolean equals(Object o) {
		if (o instanceof Node) {
			Node node = (Node)o;
			return world == node.world && pos.equals(node.pos);
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return pos.hashCode();
	}
	
	public Node update(BlockPos pos, BlockState state) {
		if (wireBlock.isOf(state)) {
			throw new IllegalStateException("Cannot update a regular Node to a WireNode!");
		}
		
		this.pos = pos.toImmutable();
		this.state = state;
		this.removed = false;
		this.flags = 0;
		
		Arrays.fill(neighbors, null);
		
		if (this.world.isConductor(this.pos, this.state)) {
			this.flags |= CONDUCTOR;
		}
		if (this.state.emitsRedstonePower()) {
			this.flags |= REDSTONE;
		}
		
		return this;
	}
	
	public boolean isOf(WireBlock wireBlock) {
		return this.wireBlock == wireBlock;
	}
	
	public boolean isWire() {
		return false;
	}
	
	public boolean isConductor() {
		return (flags & CONDUCTOR) != 0;
	}
	
	public boolean isRedstoneComponent() {
		return (flags & REDSTONE) != 0;
	}
	
	public WireNode asWire() {
		throw new UnsupportedOperationException("Not a WireNode!");
	}
}
