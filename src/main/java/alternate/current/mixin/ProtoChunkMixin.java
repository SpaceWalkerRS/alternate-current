package alternate.current.mixin;

import java.util.Collection;

import org.spongepowered.asm.mixin.Mixin;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.redstone.WireNode;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ProtoChunk;

@Mixin(ProtoChunk.class)
public class ProtoChunkMixin implements IChunk {
	
	private final Long2ObjectMap<WireNode> wires = new Long2ObjectOpenHashMap<>();
	
	@Override
	public WireNode getWire(BlockPos pos) {
		return wires.get(pos.asLong());
	}
	
	@Override
	public Collection<WireNode> getWires() {
		return wires.values();
	}
	
	@Override
	public void placeWire(WireNode wire) {
		wires.put(wire.pos.asLong(), wire);
	}
	
	@Override
	public void removeWire(WireNode wire) {
		wires.remove(wire.pos.asLong());
	}
}
