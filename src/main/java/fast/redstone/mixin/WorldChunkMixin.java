package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fast.redstone.RedstoneWireHandler;
import fast.redstone.interfaces.mixin.IChunk;
import fast.redstone.interfaces.mixin.IChunkSection;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public class WorldChunkMixin implements IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	
	@Override
	public RedstoneWireHandler getWireHandler(BlockPos pos) {
		int y = pos.getY();
		
		if (y < 0) {
			return null;
		}
		
		int index = y >> 4;
		
		if (index >= sections.length) {
			return null;
		}
		
		ChunkSection section = sections[index];
		
		if (ChunkSection.isEmpty(section)) {
			return null;
		}
		
		return ((IChunkSection)section).getWireHandler(pos);
	}
	
	@Override
	public void addWireHandler(RedstoneWireHandler wireHandler, int chunkY) {
		if (chunkY >= sections.length) {
			return;
		}
		
		ChunkSection section = sections[chunkY];
		
		if (ChunkSection.isEmpty(section)) {
			return;
		}
		
		((IChunkSection)section).addRedstoneWireHandler(wireHandler);
	}
	
	@Override
	public void removeWireHandler(RedstoneWireHandler wireHandler, int chunkY) {
		if (chunkY >= sections.length) {
			return;
		}
		
		ChunkSection section = sections[chunkY];
		
		if (ChunkSection.isEmpty(section)) {
			return;
		}
		
		((IChunkSection)section).removeRedstoneWireHandler(wireHandler);
	}
}
