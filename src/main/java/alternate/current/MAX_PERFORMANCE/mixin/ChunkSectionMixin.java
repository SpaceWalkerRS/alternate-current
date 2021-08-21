package alternate.current.MAX_PERFORMANCE.mixin;

import java.util.Arrays;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.MAX_PERFORMANCE.WireBlock;
import alternate.current.MAX_PERFORMANCE.WireNode;
import alternate.current.MAX_PERFORMANCE.interfaces.mixin.IChunkSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final WireNode[] wires = new WireNode[4096];
	private int wireCount;
	
	@Override
	public void clearWires() {
		if (wireCount > 0) {
			Arrays.fill(wires, null);
		}
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		WireNode wire = wires[getIndex(pos)];
		return wire != null && wire.isOf(wireBlock) ? wire : null;
	}
	
	@Override
	public WireNode setWire(BlockPos pos, WireNode wire) {
		int index = getIndex(pos);
		
		WireNode prevWire = wires[index];
		wires[index] = wire;
		
		if (prevWire != null) {
			wireCount--;
		}
		if (wire != null) {
			wireCount++;
		}
		
		return prevWire;
	}
	
	private int getIndex(BlockPos pos) {
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return x << 8 | y << 4 | z;
	}
}
