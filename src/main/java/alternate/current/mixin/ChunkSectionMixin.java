package alternate.current.mixin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.Wire;
import alternate.current.boop.WireBlock;
import alternate.current.boop.WireNode;
import alternate.current.interfaces.mixin.IChunkSection;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private static final int SIZE = 4096;
	
	private final Wire[] wires = new Wire[4096];
	private final Map<WireBlock, WireNode[]> wiresMap = new HashMap<>();
	
	private int wireCount;
	
	@Override
	public void clearWires() {
		if (wireCount > 0) {
			Arrays.fill(wires, null);
		}
	}
	
	@Override
	public Wire getWire(int x, int y, int z) {
		return wires[getIndex(x, y, z)];
	}
	
	@Override
	public Wire setWire(int x, int y, int z, Wire wire) {
		int index = getIndex(x, y, z);
		
		Wire oldWire = wires[index];
		wires[index] = wire;
		
		if (oldWire != null) {
			wireCount--;
		}
		if (wire != null) {
			wireCount++;
		}
		
		return oldWire;
	}
	
	@Override
	public WireNode getWire(WireBlock wireBlock, BlockPos pos) {
		WireNode[] wires = getWires(wireBlock);
		int index = getIndex(pos);
		
		return wires[index];
	}
	
	@Override
	public WireNode setWire(WireNode wire) {
		WireNode[] wires = getWires(wire.wireBlock);
		int index = getIndex(wire.pos);
		
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
	
	private WireNode[] getWires(WireBlock wireBlock) {
		WireNode[] wires = wiresMap.get(wireBlock);
		
		if (wires == null) {
			wires = new WireNode[SIZE];
			wiresMap.put(wireBlock, wires);
		}
		
		return wires;
	}
	
	private int getIndex(BlockPos pos) {
		int x = pos.getX() & 15;
		int y = pos.getY() & 15;
		int z = pos.getZ() & 15;
		
		return x | y << 4 | z << 8;
	}
	
	private int getIndex(int x, int y, int z) {
		return x | y << 4 | z << 8;
	}
}
