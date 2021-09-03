package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.redstone.WireNode;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final WireNode[] wires = new WireNode[4096];
	
	@Override
	public WireNode getWire(int x, int y, int z) {
		return wires[getIndex(x, y, z)];
	}
	
	@Override
	public WireNode setWire(int x, int y, int z, WireNode wire) {
		int index = getIndex(x, y, z);
		
		WireNode prevWire = wires[index];
		wires[index] = wire;
		
		return prevWire;
	}
	
	private int getIndex(int x, int y, int z) {
		return x << 8 | y << 4 | z;
	}
}
