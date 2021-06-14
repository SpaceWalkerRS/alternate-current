package alternate.current.redstone;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Node {
	
	private static final int SOLID_BLOCK = 1;
	private static final int REDSTONE_COMPONENT = 2;
	
	public final World world;
	public final BlockPos pos;
	public final int types;
	
	public BlockState state;
	private boolean invalid;
	
	public Node(World world, BlockPos pos, int types, BlockState state) {
		this.world = world;
		this.pos = pos.toImmutable();
		this.types = types;
		
		this.state = state;
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
	
	public static Node of(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		if (wireBlock.isOf(state)) {
			WireNode wire = wireBlock.getWire(world, pos);
			
			if (wire != null) {
				return wire;
			}
		}
		
		int types = 0;
		
		if (state.isSolidBlock(world, pos)) {
			types |= SOLID_BLOCK;
		}
		if (state.emitsRedstonePower()) {
			types |= REDSTONE_COMPONENT;
		}
		
		return new Node(world, pos, types, state);
	}
	
	public boolean isInvalid() {
		return invalid;
	}
	
	public boolean isWire() {
		return false;
	}
	
	public boolean isSolidBlock() {
		return (types & SOLID_BLOCK) != 0;
	}
	
	public boolean isRedstoneComponent() {
		return (types & REDSTONE_COMPONENT) != 0;
	}
	
	public WireNode asWire() {
		if (isWire()) {
			return (WireNode)this;
		}
		
		throw new IllegalStateException("This Node is not a wire!");
	}
}
