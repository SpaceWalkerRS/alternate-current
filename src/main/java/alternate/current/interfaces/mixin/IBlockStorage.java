package alternate.current.interfaces.mixin;

import alternate.current.redstone.WireNode;

import net.minecraft.nbt.ListTag;
import net.minecraft.server.world.ServerWorld;

public interface IBlockStorage {
	
	/**
	 * Retrieve the WireNode at this position in the chunk section.
	 */
	public WireNode getWire(int x, int y, int z);
	
	/**
	 * Set the given WireNode in the chunk section.
	 */
	public WireNode setWire(int x, int y, int z, WireNode wire);
	
	/**
	 * Create a list with NBT data of every WireNode in this section.
	 */
	public ListTag getWireNbt();
	
	/**
	 * Read WireNodes from the given NBT list and place them in this section.
	 */
	public void readWireNbt(ListTag nbt, ServerWorld world);
	
}
