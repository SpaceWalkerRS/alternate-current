package alternate.current.MAX_PERFORMANCE.interfaces.mixin;

import net.minecraft.server.world.ChunkHolder;

public interface IThreadedAnvilChunkStorage {
	
	public Iterable<ChunkHolder> getEntryIterator();
	
}
