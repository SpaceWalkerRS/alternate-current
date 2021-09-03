package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.redstone.WireNode;

import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ReadOnlyChunk;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ReadOnlyChunk.class)
public class ReadOnlyChunkMixin implements IChunk {
	
	@Shadow @Final private WorldChunk wrapped;
	
	@Override
	public WireNode getWire(BlockPos pos) {
		return ((IChunk)wrapped).getWire(pos);
	}
	
	@Override
	public void placeWire(WireNode wire) {
		((IChunk)wrapped).placeWire(wire);
	}
	
	@Override
	public void removeWire(WireNode wire) {
		((IChunk)wrapped).removeWire(wire);
	}
}
