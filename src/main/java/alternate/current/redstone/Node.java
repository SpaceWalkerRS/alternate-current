package alternate.current.redstone;

import alternate.current.AlternateCurrentMod;

import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class Node {
	
	private static final int SOLID_BLOCK = 1;
	private static final int REDSTONE_COMPONENT = 2;
	
	public final World world;
	public final WireBlock wireBlock;
	
	public BlockPos pos;
	public BlockState state;
	private int attributes;
	
	public Node(World world, WireBlock wireBlock) {
		this.world = world;
		this.wireBlock = wireBlock;
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
		this.pos = pos.toImmutable();
		this.state = state;
		this.attributes = 0;
		
		if (wireBlock.isOf(state)) {
			AlternateCurrentMod.LOGGER.warn("Cannot update a regular Node to a WireNode!");
		} else {
			if (state.isSolidBlock(world, pos)) {
				this.attributes |= SOLID_BLOCK;
			}
			if (state.emitsRedstonePower()) {
				this.attributes |= REDSTONE_COMPONENT;
			}
		}
		
		return this;
	}
	
	public boolean isWire() {
		return false;
	}
	
	public boolean isSolidBlock() {
		return (attributes & SOLID_BLOCK) != 0;
	}
	
	public boolean isRedstoneComponent() {
		return (attributes & REDSTONE_COMPONENT) != 0;
	}
	
	public WireNode asWire() {
		throw new UnsupportedOperationException("Not a WireNode!");
	}
}
