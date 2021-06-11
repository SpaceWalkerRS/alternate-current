package alternate.current.boop;

import alternate.current.interfaces.mixin.IWorld;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Node {
	
	public final World world;
	public final BlockPos pos;
	public final NodeType type;
	
	public BlockState state;
	private boolean invalid;
	
	public Node(World world, BlockPos pos, NodeType type, BlockState state) {
		this.world = world;
		this.pos = pos;
		this.type = type;
		
		this.state = state;
	}
	
	public static Node of(WireBlock wireBlock, World world, BlockPos pos, BlockState state) {
		NodeType type = NodeType.of(wireBlock, world, pos, state);
		
		if (type == NodeType.WIRE) {
			WireNode wire = ((IWorld)world).getWire(wireBlock, pos);
			
			if (wire != null) {
				return wire;
			}
		}
		
		return new Node(world, pos, type, state);
	}
	
	public boolean isInvalid() {
		return invalid;
	}
	
	public boolean isWire() {
		return false;
	}
	
	public boolean isSolidBlock() {
		return type == NodeType.SOLID_BLOCK;
	}
	
	public boolean isRedstoneComponent() {
		return type == NodeType.REDSTONE_COMPONENT;
	}
	
	public WireNode asWire() {
		if (isWire()) {
			return (WireNode)this;
		}
		
		throw new IllegalStateException("This Node is not a wire!");
	}
}
