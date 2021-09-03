package alternate.current.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import alternate.current.interfaces.mixin.IChunk;
import alternate.current.interfaces.mixin.IChunkSection;
import alternate.current.interfaces.mixin.IServerWorld;
import alternate.current.redstone.WireBlock;
import alternate.current.redstone.WireNode;
import alternate.current.redstone.WorldAccess;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.chunk.WorldChunk;

@Mixin(WorldChunk.class)
public abstract class WorldChunkMixin implements IChunk {
	
	@Shadow @Final private ChunkSection[] sections;
	@Shadow @Final private World world;
	
	@Inject(
			method = "<init>(Lnet/minecraft/server/world/ServerWorld;Lnet/minecraft/world/chunk/ProtoChunk;Ljava/util/function/Consumer;)V",
			at = @At(
					value = "RETURN"
			)
	)
	private void onInitFromProtoChunk(ServerWorld serverWorld, ProtoChunk protoChunk, Consumer<WorldChunk> consumer, CallbackInfo ci) {
		for (WireNode wire : ((IChunk)protoChunk).getWires()) {
			placeWire(wire);
		}
	}
	
	@Inject(
			method = "setBlockState",
			locals = LocalCapture.CAPTURE_FAILHARD,
			at = @At(
					value = "INVOKE",
					shift = Shift.BEFORE,
					target = "Lnet/minecraft/block/BlockState;onStateReplaced(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;Z)V"
			)
	)
	private void onSetBlockStateInjectBeforeStateReplaced(BlockPos pos, BlockState newState, boolean moved, CallbackInfoReturnable<BlockState> cir, int y, int sectionIndex, ChunkSection chunkSection, boolean isEmpty, int chunkX, int chunkY, int chunkZ, BlockState prevState, Block newBlock) {
		if (world.isDebugWorld()) {
			return;
		}
		
		Block prevBlock = prevState.getBlock();
		
		boolean wasWire = prevBlock instanceof WireBlock;
		boolean isWire = newBlock instanceof WireBlock;
		
		if (newBlock != prevBlock) {
			if (wasWire) {
				WireNode wire = getWire(pos);
				
				if (wire != null) {
					removeWire(wire);
					wire.updateConnectedWires();
					wire.wireBlock.onWireRemoved(wire.world, pos, prevState, wire, moved);
				}
			}
			if (isWire) {
				WireBlock wireBlock = (WireBlock)newBlock;
				WorldAccess worldAccess = ((IServerWorld)world).getAccess(wireBlock);
				WireNode wire = new WireNode(wireBlock, worldAccess, pos, newState);
				
				placeWire(wire);
				wire.connections.update();
				wireBlock.onWireAdded(worldAccess, pos, newState, wire, moved);
			}
		} else if (isWire) {
			WireNode wire = getWire(pos);
			
			if (wire != null) {
				if (wire.isOf((WireBlock)newBlock)) {
					wire.stateChanged(newState);
				} else {
					WireBlock wireBlock = (WireBlock)newBlock;
					WorldAccess worldAccess = ((IServerWorld)world).getAccess(wireBlock);
					wire = new WireNode(wireBlock, worldAccess, pos, newState);
					
					placeWire(wire);
					wire.connections.update();
				}
			}
		}
		
		if (!wasWire || !isWire) {
			// Other than placing or breaking wire blocks, the only way
			// to affect wire connections is to place/break a solid
			// block to (un)cut a connection.
			boolean wasSolid = prevState.isSolidBlock(world, pos);
			boolean isSolid = newState.isSolidBlock(world, pos);
			
			if (wasSolid != isSolid) {
				((IServerWorld)world).updateWireConnectionsAround(pos);
			}
		}
	}
	
	@Override
	public WireNode getWire(BlockPos pos) {
		ChunkSection section = getChunkSection(pos.getY());
		
		if (section == null) {
			return null;
		}
		
		return ((IChunkSection)section).getWire(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15);
	}
	
	@Override
	public void placeWire(WireNode wire) {
		wire.removed = false;
		wire.shouldBreak = false;
		setWire(wire.wireBlock, wire.pos, wire);
	}
	
	@Override
	public void removeWire(WireNode wire) {
		wire.removed = true;
		setWire(wire.wireBlock, wire.pos, null);
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
	
	private void setWire(WireBlock wireBlock, BlockPos pos, WireNode wire) {
		ChunkSection section = getChunkSection(pos.getY());
		
		if (section != null) {
			((IChunkSection)section).setWire(pos.getX() & 15, pos.getY() & 15, pos.getZ() & 15, wire);
		}
	}
}
