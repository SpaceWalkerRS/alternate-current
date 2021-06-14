package alternate.current.mixin;

import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private static final int SIZE = 4096;
	
	private final Map<WireBlock, WireNode[]> wiresMap = new HashMap<>();
	private final Map<WireBlock, Integer> wireCounts = new HashMap<>();
	
	@Override
	public void clearWires() {
		wiresMap.clear();
		wireCounts.clear();
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		WireNode[] wires = getWires(wireBlock);
		int index = getIndex(pos);
		
		return wires[index];
	}
	
	@Override
	public WireNode setWire(WireBlock wireBlock, BlockPos pos, WireNode wire) {
		WireNode[] wires = getWires(wireBlock);
		int index = getIndex(pos);
		
		WireNode prevWire = wires[index];
		wires[index] = wire;
		
		boolean wasNull = (prevWire == null);
		boolean isNull = (wire == null);
		
		if (wasNull != isNull) {
			updateWireCount(wireBlock, wasNull);
		}
		
		return prevWire;
	}
	
	private WireNode[] getWires(WireBlock wireBlock) {
		WireNode[] wires = wiresMap.get(wireBlock);
		
		if (wires == null) {
			wires = new WireNode[SIZE];
			wiresMap.put(wireBlock, wires);
		}
		
		return wires;
	}
	
	private void updateWireCount(WireBlock wireBlock, boolean add) {
		wireCounts.compute(wireBlock, (w, wireCount) -> {
			if (wireCount == null) {
				wireCount = 0;
			}
			
			wireCount += (add ? 1 : -1);
			
			if (wireCount <= 0) {
				wiresMap.remove(wireBlock);
			}
			
			return wireCount;
		});
	}
	
	private int getIndex(BlockPos pos) {
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return x | y << 4 | z << 8;
	}
}
