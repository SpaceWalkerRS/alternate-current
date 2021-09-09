package alternate.current.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	@Shadow @Final private World world;
	
	@Inject(
			method = "setBlockState",
			locals = LocalCapture.CAPTURE_FAILHARD,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/BlockState;onBlockRemoved(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V"
			)
	)
	private void onSetBlockStateInjectBeforeBlockRemoved(BlockPos pos, BlockState newState, boolean moved, CallbackInfoReturnable<BlockState> cir, int chunkX, int y, int chunkZ, ChunkSection chunkSection, boolean isEmpty, BlockState prevState, Block newBlock, Block prevBlock) {
		boolean wasWire = prevBlock instanceof WireBlock;
		boolean isWire = newBlock instanceof WireBlock;
		
		if (!wasWire || !isWire) {
			// Other than placing or breaking wire blocks, the only way
			// to affect wire connections is to place/break a solid
			// block to (un)cut a connection.
			boolean wasSolid = prevState.isSimpleFullBlock(world, pos);
			boolean isSolid = newState.isSimpleFullBlock(world, pos);
			
			if (wasSolid != isSolid) {
				((IServerWorld)world).updateWireConnectionsAround(pos);
			}
		}
	}
	
	@Override
	public WireNode getWireNode(BlockPos pos) {
		ChunkSection section = getChunkSection(pos.getY());
		
		if (section == null) {
			return null;
		}
		
		return ((IChunkSection)section).getWire(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
	}
	
	private ChunkSection getChunkSection(int y) {
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
		
		return section;
	}
}
