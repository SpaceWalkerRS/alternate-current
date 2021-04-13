package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import fast.redstone.interfaces.mixin.IThreadedAnvilChunkStorage;

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.server.world.ChunkHolder;
import net.minecraft.server.world.ThreadedAnvilChunkStorage;

@Mixin(ThreadedAnvilChunkStorage.class)
public abstract class ThreadedAnvilChunkStorageMixin implements IThreadedAnvilChunkStorage {
	
	@Shadow private Long2ObjectLinkedOpenHashMap<ChunkHolder> chunkHolders;
	
	@Override
	public Iterable<ChunkHolder> getEntryIterator() {
		return chunkHolders.values();
	}
}
