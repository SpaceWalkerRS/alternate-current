package alternate.current.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.Wire;
import alternate.current.interfaces.mixin.IChunkSection;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final Wire[] wiresV2 = new Wire[4096];
	
	private int wireV2Count;
	
	@Override
	public void clearWires() {
		if (wireV2Count > 0) {
			Arrays.fill(wiresV2, null);
		}
	}
	
	@Override
	public Wire getWireV2(int x, int y, int z) {
		return wiresV2[toIndex(x, y, z)];
	}
	
	@Override
	public Wire setWireV2(int x, int y, int z, Wire wire) {
		int index = toIndex(x, y, z);
		
		Wire oldWire = wiresV2[index];
		wiresV2[index] = wire;
		
		if (oldWire != null) {
			wireV2Count--;
		}
		if (wire != null) {
			wireV2Count++;
		}
		
		return oldWire;
	}
	
	private int toIndex(int x, int y, int z) {
		return x | y << 4 | z << 8;
	}
}
