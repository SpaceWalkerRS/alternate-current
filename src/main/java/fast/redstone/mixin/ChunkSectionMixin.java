package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;

import fast.redstone.Wire;
import fast.redstone.interfaces.mixin.IChunkSection;

import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final Wire[] wires = new Wire[4096];
	
	@Override
	public Wire getWire(int x, int y, int z) {
		return wires[toIndex(x, y, z)];
	}
	
	@Override
	public Wire setWire(int x, int y, int z, Wire wire) {
		int index = toIndex(x, y, z);
		
		Wire oldWire = wires[index];
		wires[index] = wire;
		
		return oldWire;
	}
	
	private int toIndex(int x, int y, int z) {
		return x | y << 4 | z << 8;
	}
}
