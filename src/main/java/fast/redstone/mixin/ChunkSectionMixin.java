package fast.redstone.mixin;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;

import fast.redstone.RedstoneWireHandler;
import fast.redstone.interfaces.mixin.IChunkSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;

@Mixin(ChunkSection.class)
public class ChunkSectionMixin implements IChunkSection {
	
	private final Set<RedstoneWireHandler> wireHandlers = new HashSet<>();
	
	@Override
	public RedstoneWireHandler getWireHandler(BlockPos pos) {
		for (RedstoneWireHandler wireHandler : wireHandlers) {
			if (wireHandler.isPosInNetwork(pos)) {
				return wireHandler;
			}
		}
		
		return null;
	}
	
	@Override
	public void addRedstoneWireHandler(RedstoneWireHandler wireHandler) {
		wireHandlers.add(wireHandler);
	}
	
	@Override
	public void removeRedstoneWireHandler(RedstoneWireHandler wireHandler) {
		wireHandlers.remove(wireHandler);
	}
}
