package alternate.current.mixin;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.redstone.WireNode;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final WireNode[] wires = new WireNode[4096];
	
	private int wireCount;
	
	@Override
	public WireNode getWire(int x, int y, int z) {
		return wires[getIndex(x, y, z)];
	}
	
	@Override
	public WireNode setWire(int x, int y, int z, WireNode wire) {
		int index = getIndex(x, y, z);
		
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
	
	@Override
	public NbtList getWireNbt() {
		NbtList nbt = new NbtList();
		
		if (wireCount > 0) {
			for (WireNode wire : wires) {
				if (wire == null) {
					continue;
				}
				
				nbt.add(wire.toNbt());
			}
		}
		
		return nbt;
	}
	
	@Override
	public void readWireNbt(NbtList nbt, ServerWorld world) {
		for (int index = 0; index < nbt.size(); index++) {
			NbtCompound wireNbt = nbt.getCompound(index);
			WireNode wire = WireNode.fromNbt(wireNbt, world, (ChunkSection)(Object)this);
			
			if (wire != null) {
				int x = wire.pos.getX() & 15;
				int y = wire.pos.getY() & 15;
				int z = wire.pos.getZ() & 15;
				
				setWire(x, y, z, wire);
			}
		}
	}
	
	private int getIndex(int x, int y, int z) {
		return x << 8 | y << 4 | z;
	}
}
