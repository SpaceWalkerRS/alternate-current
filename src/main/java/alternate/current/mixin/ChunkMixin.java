package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import alternate.current.interfaces.mixin.IBlockStorage;
import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.BlockStorage;
import net.minecraft.world.chunk.Chunk;

@Mixin(Chunk.class)
public abstract class ChunkMixin implements IChunk {
	
	@Shadow @Final private BlockStorage[] blockStorage;
	@Shadow @Final private World world;
	
	@Inject(
			method = "getBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)Lnet/minecraft/block/BlockState;",
			locals = LocalCapture.CAPTURE_FAILHARD,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/Block;onBreaking(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;)V"
			)
	)
	private void onGetBlockStateInjectBeforeOnBreaking(BlockPos pos, BlockState newState, CallbackInfoReturnable<BlockState> cir, int chunkX, int y, int chunkZ, int chunkIndex, int chunkHeight, BlockState prevState, Block newBlock, Block prevBlock) {
		boolean wasWire = prevBlock instanceof WireBlock;
		boolean isWire = newBlock instanceof WireBlock;
		
		if (!wasWire || !isWire) {
			// Other than placing or breaking wire blocks, the only way
			// to affect wire connections is to place/break a solid
			// block to (un)cut a connection.
			boolean wasSolid = prevBlock.isFullCube();
			boolean isSolid = newBlock.isFullCube();
			
			if (wasSolid != isSolid) {
				((IServerWorld)world).updateWireConnectionsAround(pos);
			}
		}
	}
	
	@Override
	public WireNode getWireNode(BlockPos pos) {
		BlockStorage section = getChunkSection(pos.getY());
		
		if (section == null) {
			return null;
		}
		
		return ((IBlockStorage)section).getWire(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
	}
	
	private BlockStorage getChunkSection(int y) {
		if (y < 0) {
			return null;
		}
		
		int index = y >> 4;
		
		if (index >= blockStorage.length) {
			return null;
		}
		
		return blockStorage[index];
	}
}
