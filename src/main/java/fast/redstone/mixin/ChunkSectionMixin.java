package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;

import fast.redstone.interfaces.mixin.IChunkSection;
import fast.redstone.v1.WireV1;

import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final WireV1[] wires = new WireV1[4096];
	
	@Override
	public WireV1 getWireV1(int x, int y, int z) {
		return wires[toIndex(x, y, z)];
	}
	
	@Override
	public WireV1 setWireV1(int x, int y, int z, WireV1 wire) {
		int index = toIndex(x, y, z);
		
		WireV1 oldWire = wires[index];
		wires[index] = wire;
		
		return oldWire;
	}
	
	private int toIndex(int x, int y, int z) {
		return x | y << 4 | z << 8;
	}
}
