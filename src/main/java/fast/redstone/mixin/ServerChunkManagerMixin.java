package fast.redstone.mixin;

import java.util.Optional;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fast.redstone.interfaces.mixin.IChunk;
import fast.redstone.interfaces.mixin.IServerChunkManager;
import fast.redstone.interfaces.mixin.IThreadedAnvilChunkStorage;

import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ServerChunkManager;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(ServerChunkManager.class)
public class ServerChunkManagerMixin implements IServerChunkManager {
	
	@Shadow @Final private ThreadedAnvilChunkStorage threadedAnvilChunkStorage;
	
	@Override
	public void clearWires() {
		Iterable<ChunkHolder> it = ((IThreadedAnvilChunkStorage)threadedAnvilChunkStorage).getEntryIterator();
		
		for (ChunkHolder chunkHolder : it) {
			Optional<WorldChunk> optional = chunkHolder.getTickingFuture().getNow(ChunkHolder.UNLOADED_WORLD_CHUNK).left();
			
			if (optional.isPresent()) {
				WorldChunk chunk = optional.get();
				((IChunk)chunk).clearWires();
			}
		}
	}
}
