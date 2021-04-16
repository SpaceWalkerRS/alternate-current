package alternate.current.interfaces.mixin;

import net.minecraft.server.world.ChunkHolder;

public interface IThreadedAnvilChunkStorage {
	
	public Iterable<ChunkHolder> getEntryIterator();
	
}
