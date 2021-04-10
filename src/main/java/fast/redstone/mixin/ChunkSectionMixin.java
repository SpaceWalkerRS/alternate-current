package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;

import fast.redstone.interfaces.mixin.IChunkSection;
import fast.redstone.v1.WireV1;
import fast.redstone.v2.WireV2;

import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final WireV1[] wiresV1 = new WireV1[4096];
	private final WireV2[] wiresV2 = new WireV2[4096];
	
	@Override
	public WireV1 getWireV1(int x, int y, int z) {
		return wiresV1[toIndex(x, y, z)];
	}
	
	@Override
	public WireV1 setWireV1(int x, int y, int z, WireV1 wire) {
		int index = toIndex(x, y, z);
		
		WireV1 oldWire = wiresV1[index];
		wiresV1[index] = wire;
		
		return oldWire;
	}
	
	@Override
	public WireV2 getWireV2(int x, int y, int z) {
		return wiresV2[toIndex(x, y, z)];
	}
	
	@Override
	public WireV2 setWireV2(int x, int y, int z, WireV2 wire) {
		int index = toIndex(x, y, z);
		
		WireV2 oldWire = wiresV2[index];
		wiresV2[index] = wire;
		
		return oldWire;
	}
	
	private int toIndex(int x, int y, int z) {
		return x | y << 4 | z << 8;
	}
}
