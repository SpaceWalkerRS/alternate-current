package fast.redstone.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fast.redstone.RedstoneWireHandler;
import fast.redstone.interfaces.mixin.IChunk;
import fast.redstone.interfaces.mixin.IWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(World.class)
public abstract class WorldMixin implements IWorld {
	
	private RedstoneWireHandler cachedWireHandler;
	
	private int blockUpdateCount;
	
	@Shadow public abstract WorldChunk getChunk(int x, int z);
	@Shadow public abstract BlockState getBlockState(BlockPos pos);
	@Shadow public abstract boolean isDebugWorld();
	@Shadow public abstract boolean isClient();
	
	@Inject(
			method = "updateNeighbor",
			at = @At(
					value = "INVOKE",
					target = "Lnet/minecraft/block/BlockState;neighborUpdate(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/Block;Lnet/minecraft/util/math/BlockPos;Z)V"
			)
	)
	private void onUpdateNeighborInjectAtNeighborUpdate(BlockPos sourcePos, Block sourceBlock, BlockPos neighborPos, CallbackInfo ci) {
		if (!isClient()) {
			blockUpdateCount++;
		}
	}
	
	@Override
	public void reset() {
		blockUpdateCount = 0;
	}
	
	@Override
	public int getCount() {
		return blockUpdateCount;
	}
	
	@Override
	public RedstoneWireHandler getWireHandler(BlockPos pos) {
		if (isDebugWorld()) {
			return null;
		}
		
		if (cachedWireHandler != null && cachedWireHandler.isPosInNetwork(pos)) {
			return cachedWireHandler;
		}
		
		WorldChunk chunk = getChunk(pos.getX() >> 4, pos.getZ() >> 4);
		RedstoneWireHandler wireHandler = ((IChunk)chunk).getWireHandler(pos);
		
		if (wireHandler != null) {
			cachedWireHandler = wireHandler;
		}
		
		return wireHandler;
	}
	
	@Override
	public RedstoneWireHandler getOrCreateWireHandler(BlockPos pos, BlockState state) {
		RedstoneWireHandler wireHandler = getWireHandler(pos);
		
		if (wireHandler == null) {
			wireHandler = new RedstoneWireHandler((World)(Object)this, pos, state);
			
			addWireHandler(wireHandler);
		}
		
		return wireHandler;
	}
	
	@Override
	public void addWireHandler(RedstoneWireHandler wireHandler) {
		if (isDebugWorld()) {
			return;
		}
		
		for (BlockPos chunkPos : wireHandler.getChunkCoords()) {
			int x = chunkPos.getX();
			int y = chunkPos.getY();
			int z = chunkPos.getZ();
			
			((IChunk)getChunk(x, z)).addWireHandler(wireHandler, y);
		}
	}
	
	@Override
	public void removeWireHandler(RedstoneWireHandler wireHandler) {
		if (isDebugWorld()) {
			return;
		}
		
		for (BlockPos chunkPos : wireHandler.getChunkCoords()) {
			int x = chunkPos.getX();
			int z = chunkPos.getZ();
			int y = chunkPos.getY();
			
			((IChunk)getChunk(x, z)).removeWireHandler(wireHandler, y);
		}
	}
	
	@Override
	public void removeWireHandler(BlockPos pos) {
		RedstoneWireHandler wireHandler = getWireHandler(pos);
		
		if (wireHandler != null) {
			removeWireHandler(wireHandler);
		}
		
		if (wireHandler == cachedWireHandler) {
			cachedWireHandler = null;
		}
	}
}
